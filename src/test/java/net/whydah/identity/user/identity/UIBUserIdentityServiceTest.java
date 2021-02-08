package net.whydah.identity.user.identity;

import net.whydah.identity.Main;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.ldapserver.EmbeddedADS;
import net.whydah.identity.user.authentication.UserAdminHelper;
import net.whydah.identity.user.role.UserApplicationRoleEntryDao;
import net.whydah.identity.user.search.LuceneUserIndexer;
import net.whydah.identity.user.search.LuceneUserSearch;
import net.whydah.identity.util.FileUtils;
import net.whydah.identity.util.PasswordGenerator;
import net.whydah.sso.user.types.UserIdentity;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.testng.Assert.assertEquals;


/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 02/04/14
 */
public class UIBUserIdentityServiceTest {
    private static final Logger log = LoggerFactory.getLogger(UIBUserIdentityServiceTest.class);
    private static final String ldapPath = "target/UIBUserIdentityServiceTest/ldap";

    private static LdapUserIdentityDao ldapUserIdentityDao;
    private static PasswordGenerator passwordGenerator;
    private static LuceneUserIndexer luceneIndexer;
    private static UserAdminHelper userAdminHelper;
    private static Directory index;

    private static Main main = null;
    private BasicDataSource dataSource;


    @BeforeClass
    public void setUp() throws Exception {
        ApplicationMode.setCIMode();
        final ConstrettoConfiguration config = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("classpath:useridentitybackend-test.properties"))
                .done()
                .getConfiguration();


        String roleDBDirectory = config.evaluateToString("roledb.directory");
        String luceneUsersDir = config.evaluateToString("lucene.usersdirectory");
        FileUtils.deleteDirectories(ldapPath, roleDBDirectory, luceneUsersDir);

        Map<String, String> ldapProperties = Main.ldapProperties(config);
        ldapProperties.put("ldap.embedded.directory", ldapPath);
        ldapProperties.put(EmbeddedADS.PROPERTY_BIND_PORT, "10789");
        String primaryLdapUrl = "ldap://localhost:10789/dc=people,dc=whydah,dc=no";
        ldapProperties.put("ldap.primary.url", primaryLdapUrl);

        main = new Main(6652);
        main.startEmbeddedDS(ldapProperties);

        dataSource = Main.initBasicDataSource(config);
        DatabaseMigrationHelper dbHelper = new DatabaseMigrationHelper(dataSource);
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();


        String primaryAdmPrincipal = config.evaluateToString("ldap.primary.admin.principal");
        String primaryAdmCredentials = config.evaluateToString("ldap.primary.admin.credentials");
        String primaryUidAttribute = config.evaluateToString("ldap.primary.uid.attribute");
        String primaryUsernameAttribute = config.evaluateToString("ldap.primary.username.attribute");
        String readonly = config.evaluateToString("ldap.primary.readonly");
        ldapUserIdentityDao = new LdapUserIdentityDao(primaryLdapUrl, primaryAdmPrincipal, primaryAdmCredentials, primaryUidAttribute, primaryUsernameAttribute, readonly);

        UserApplicationRoleEntryDao userApplicationRoleEntryDao = new UserApplicationRoleEntryDao(dataSource);

        index = new NIOFSDirectory(Paths.get(new File(luceneUsersDir).getPath()));
        luceneIndexer = new LuceneUserIndexer(index);
        AuditLogDao auditLogDao = new AuditLogDao(dataSource);
        userAdminHelper = new UserAdminHelper(ldapUserIdentityDao, luceneIndexer, auditLogDao, userApplicationRoleEntryDao, config);
        passwordGenerator = new PasswordGenerator();
    }

    @AfterClass
    public void stop() {
        if (main != null) {
            main.stopEmbeddedDS();
        }

        try {
            if(!dataSource.isClosed()) {
                dataSource.close();
            }
        } catch (SQLException e) {
            log.error("", e);
        }

        FileUtils.deleteDirectories(ldapPath);
    }

    @Test
    public void testAddUserToLdap() throws Exception {
        UserIdentityService userIdentityService =
                new UserIdentityService(null, ldapUserIdentityDao, null, passwordGenerator, luceneIndexer, Mockito.mock(LuceneUserSearch.class));

        String username = "username123";
        LDAPUserIdentity userIdentity = new LDAPUserIdentity("uidvalue", username, "firstName", "lastName", "test@test.no", "password", "12345678", "personRef"
        );
        userAdminHelper.addUser(userIdentity);

        UserIdentity fromLdap = userIdentityService.getUserIdentity(username);

        assertEquals(userIdentity, fromLdap);
        Response response = userAdminHelper.addUser(userIdentity);
        assertEquals(Response.Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus(), "Expected ConflictException because user should already exist.");
    }

    @Test
    public void testAddTestUserToLdap() throws Exception {
        UserIdentityService userIdentityService =
                new UserIdentityService(null, ldapUserIdentityDao, null, passwordGenerator, luceneIndexer, Mockito.mock(LuceneUserSearch.class));

        Random rand = new Random();
        rand.setSeed(new java.util.Date().getTime());
        LDAPUserIdentity userIdentity = new LDAPUserIdentity("uidvalue",
                "us" + UUID.randomUUID().toString().replace("-", "").replace("_", ""),
                "Mt Test",
                "Testesen",
                UUID.randomUUID().toString().replace("-", "").replace("_", "") + "@getwhydah.com",
                "47" + Integer.toString(rand.nextInt(100000000)),
                null,
                "pref");

        userAdminHelper.addUser(userIdentity);

        UserIdentity fromLdap = userIdentityService.getUserIdentity(userIdentity.getUsername());

        assertEquals(userIdentity, fromLdap);

    }

    @Test
    public void testAddUserStrangeCellPhone() throws Exception {
        UserIdentityService userIdentityService =
                new UserIdentityService(null, ldapUserIdentityDao, null, passwordGenerator, luceneIndexer, Mockito.mock(LuceneUserSearch.class));

        String username = "username1234";
        LDAPUserIdentity userIdentity = new LDAPUserIdentity("uid2", username, "firstName2", "lastName2", "test2@test.no", "password2", "+47 123 45 678", "personRef2"
        );
        userAdminHelper.addUser(userIdentity);

        UserIdentity fromLdap = userIdentityService.getUserIdentity(username);

        assertEquals(userIdentity, fromLdap);
        Response response = userAdminHelper.addUser(userIdentity);
        assertEquals(Response.Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus(), "Expected ConflictException because user should already exist.");
    }

    @Test
    public void testPersistenceAddTestUserToLdap() throws Exception {
        UserIdentityService userIdentityService =
                new UserIdentityService(null, ldapUserIdentityDao, null, passwordGenerator, luceneIndexer, Mockito.mock(LuceneUserSearch.class));

        Random rand = new Random();
        rand.setSeed(new java.util.Date().getTime());
        LDAPUserIdentity userIdentity = new LDAPUserIdentity("uidvalue",
                "us" + UUID.randomUUID().toString().replace("-", "").replace("_", ""),
                "Mt Test",
                "Testesen",
                UUID.randomUUID().toString().replace("-", "").replace("_", "") + "@getwhydah.com",
                "47" + Integer.toString(rand.nextInt(100000000)),
                null,
                "pref");

        userAdminHelper.addUser(userIdentity);

        UserIdentity fromLdap = userIdentityService.getUserIdentity(userIdentity.getUsername());

        assertEquals(userIdentity, fromLdap);
        stop();
        setUp();
        UserIdentityService userIdentityService2 =
                new UserIdentityService(null, ldapUserIdentityDao, null, passwordGenerator, luceneIndexer, Mockito.mock(LuceneUserSearch.class));
        UserIdentity fromLdap2 = userIdentityService2.getUserIdentity(userIdentity.getUsername());

        // TODO: Still not working
        // assertEquals(userIdentity, fromLdap2);

    }
}
