package net.whydah.identity.user.identity;

import net.whydah.identity.Main;
import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.user.resource.UserAdminHelper;
import net.whydah.identity.user.role.UserPropertyAndRoleDao;
import net.whydah.identity.user.search.LuceneIndexer;
import net.whydah.identity.user.search.LuceneSearch;
import net.whydah.identity.util.FileUtils;
import net.whydah.identity.util.PasswordGenerator;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 02/04/14
 */
public class UserIdentityServiceTest {
    //private static EmbeddedADS ads;
    private static LdapUserIdentityDao ldapUserIdentityDao;
    private static PasswordGenerator passwordGenerator;
    private static LuceneIndexer luceneIndexer;
    private static UserAdminHelper userAdminHelper;

    private static Main main = null;


    @BeforeClass
    public static void setUp() throws Exception {
        //System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);
        //System.setProperty(ConfigTags.CONSTRETTO_TAGS, ConfigTags.DEV_MODE);
        ApplicationMode.setCIMode();
        final ConstrettoConfiguration configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("file:./useridentitybackend_override.properties"))
                .done()
                .getConfiguration();


        String roleDBDirectory = configuration.evaluateToString("roledb.directory");
        String ldapPath = configuration.evaluateToString("ldap.embedded.directory");
        String luceneDir = configuration.evaluateToString("lucene.directory");
        FileUtils.deleteDirectories(ldapPath, roleDBDirectory, luceneDir);

        main = new Main(configuration.evaluateToInt("service.port"));
        main.startEmbeddedDS(ldapPath, configuration.evaluateToInt("ldap.embedded.port"));

        BasicDataSource dataSource = initBasicDataSource(configuration);
        new DatabaseMigrationHelper(dataSource).upgradeDatabase();


        String primaryLdapUrl = configuration.evaluateToString("ldap.primary.url");
        String primaryAdmPrincipal = configuration.evaluateToString("ldap.primary.admin.principal");
        String primaryAdmCredentials = configuration.evaluateToString("ldap.primary.admin.credentials");
        String primaryUidAttribute = configuration.evaluateToString("ldap.primary.uid.attribute");
        String primaryUsernameAttribute = configuration.evaluateToString("ldap.primary.username.attribute");
        String readonly = configuration.evaluateToString("ldap.primary.readonly");

        ldapUserIdentityDao = new LdapUserIdentityDao(primaryLdapUrl, primaryAdmPrincipal, primaryAdmCredentials, primaryUidAttribute, primaryUsernameAttribute, readonly);


        ApplicationDao configDataRepository = new ApplicationDao(dataSource);
        UserPropertyAndRoleDao userPropertyAndRoleDao = new UserPropertyAndRoleDao(dataSource);

        Directory index = new NIOFSDirectory(new File(luceneDir));
        luceneIndexer = new LuceneIndexer(index);
        AuditLogDao auditLogDao = new AuditLogDao(dataSource);
        userAdminHelper = new UserAdminHelper(ldapUserIdentityDao, luceneIndexer, auditLogDao, userPropertyAndRoleDao, configuration);
        passwordGenerator = new PasswordGenerator();

        /*
        int LDAP_PORT = 19389;
        String ldapUrl = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";
        String readOnly = AppConfig.appConfig.getProperty("ldap.primary.readonly");
        ldapUserIdentityDao = new LdapUserIdentityDao(ldapUrl, "uid=admin,ou=system", "secret", "uid", "initials", readOnly);


        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:file:" + "target/" + UserIdentityServiceTest.class.getSimpleName()  + "/hsqldb");

        new DatabaseMigrationHelper(dataSource).upgradeDatabase();


        String workDirPath = "target/" + UserIdentityServiceTest.class.getSimpleName();
        File workDir = new File(workDirPath);
        FileUtils.deleteDirectory(workDir);
        if (!workDir.mkdirs()) {
            fail("Error creating working directory " + workDirPath);

        }

        luceneIndexer = new LuceneIndexer(index);

        // Create the server
        ads = new EmbeddedADS(workDir);
        ads.startServer(LDAP_PORT);
        Thread.sleep(1000);
        */


    }

    private static BasicDataSource initBasicDataSource(ConstrettoConfiguration configuration) {
        String jdbcdriver = configuration.evaluateToString("roledb.jdbc.driver");
        String jdbcurl = configuration.evaluateToString("roledb.jdbc.url");
        String roledbuser = configuration.evaluateToString("roledb.jdbc.user");
        String roledbpasswd = configuration.evaluateToString("roledb.jdbc.password");

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jdbcdriver);
        dataSource.setUrl(jdbcurl);
        dataSource.setUsername(roledbuser);
        dataSource.setPassword(roledbpasswd);
        return dataSource;
    }

    @AfterClass
    public static void stop() {
        if (main != null) {
            main.stop();
        }
    }

    @Test
    public void testAddUserToLdap() throws Exception {
        UserIdentityService userIdentityService =
                new UserIdentityService(null, ldapUserIdentityDao, null, passwordGenerator, null, luceneIndexer, Mockito.mock(LuceneSearch.class));

        String username = "username123";
        UserIdentity userIdentity = new UserIdentity("uid", username, "firstName", "lastName", "test@test.no", "password", "12345678", "personRef"
        );
        userAdminHelper.addUser(userIdentity);

        UserIdentityRepresentation fromLdap = userIdentityService.getUserIdentity(username);

        assertEquals(userIdentity, fromLdap);
        Response response = userAdminHelper.addUser(userIdentity);
        assertTrue("Expected ConflictException because user should already exist.", response.getStatus() == Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }


    @Test
    public void testAddUserStrangeCellPhone() throws Exception {
        UserIdentityService userIdentityService =
                new UserIdentityService(null, ldapUserIdentityDao, null, passwordGenerator, null, luceneIndexer, Mockito.mock(LuceneSearch.class));

        String username = "username1234";
        UserIdentity userIdentity = new UserIdentity("uid2", username, "firstName2", "lastName2", "test2@test.no", "password2", "+47 123 45 678", "personRef2"
        );
        userAdminHelper.addUser(userIdentity);

        UserIdentityRepresentation fromLdap = userIdentityService.getUserIdentity(username);

        assertEquals(userIdentity, fromLdap);
        Response response = userAdminHelper.addUser(userIdentity);
        assertTrue("Expected ConflictException because user should already exist.", response.getStatus() == Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }
}
