package net.whydah.identity.user.resource;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.whydah.identity.application.ApplicationService;
import net.whydah.identity.application.search.LuceneApplicationIndexer;
import net.whydah.identity.application.search.LuceneApplicationSearch;
import net.whydah.identity.audit.AuditLogDao;

import net.whydah.identity.dataimport.DatabaseMigrationHelper;

import net.whydah.identity.user.UserAggregateService;

import net.whydah.identity.user.identity.*;
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
import org.junit.*;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;


public class UserResourceTest {
    private static final Logger log = LoggerFactory.getLogger(UserResourceTest.class);

    private static LdapAuthenticator ldapAuthenticator = Mockito.mock(LdapAuthenticator.class);
    private static LdapUserIdentityDao ldapUserIdentityDao = Mockito.mock(LdapUserIdentityDao.class);;

    private static String luceneApplicationDirectory;
    private static String luceneUsersDirectory;

    private static UserIdentityService userIdentityService;
    private static UserIdentityServiceV2 userIdentityServiceV2;
    private static UserAggregateService userAggregateService;

    private static DatabaseMigrationHelper dbHelper;

    private static LuceneUserSearch luceneUserSearch;

    private static ConstrettoConfiguration configuration;

    @BeforeClass
    public static void init() throws Exception {
        configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("classpath:useridentitybackend-test.properties"))
                .addResource(Resource.create("classpath:useridentitybackend-test-override.properties"))
                .done()
                .getConfiguration();

        BasicDataSource dataSource = initBasicDataSource(configuration);
        dbHelper = new DatabaseMigrationHelper(dataSource);

        /** lucene setup **/
        luceneUsersDirectory = "target/" + configuration.evaluateToString("lucene.usersdirectory");
        luceneApplicationDirectory = "target/" + configuration.evaluateToString("lucene.applicationsdirectory");

        Directory usersDirectory = new NIOFSDirectory(Paths.get(new File(luceneUsersDirectory).getPath()));
        LuceneUserIndexer luceneIndexer = new LuceneUserIndexer(usersDirectory);
        luceneUserSearch = Mockito.mock(LuceneUserSearch.class);

        Directory applicationsDirectory = new NIOFSDirectory(Paths.get(new File(luceneApplicationDirectory).getPath()));
        LuceneApplicationIndexer luceneApplicationIndexer = new LuceneApplicationIndexer(applicationsDirectory);
        LuceneApplicationSearch luceneApplicationSearcher = new LuceneApplicationSearch(applicationsDirectory);

        PasswordGenerator pwdGenerator = new PasswordGenerator();
        AuditLogDao auditLogDao = new AuditLogDao(dataSource);


        userIdentityService = new UserIdentityService(ldapAuthenticator, ldapUserIdentityDao,  auditLogDao, pwdGenerator, luceneIndexer, luceneUserSearch);

        RDBMSLdapUserIdentityDao rdbmsLdapUserIdentityDao = new RDBMSLdapUserIdentityDao(dataSource);
        RDBMSLdapUserIdentityRepository rdbmsUserIdentityRepository = new RDBMSLdapUserIdentityRepository(rdbmsLdapUserIdentityDao, configuration);
        userIdentityServiceV2 = new UserIdentityServiceV2(rdbmsUserIdentityRepository, auditLogDao, pwdGenerator, luceneIndexer, luceneUserSearch);

        UserApplicationRoleEntryDao userApplicationRoleEntryDao = new UserApplicationRoleEntryDao(dataSource);
        ApplicationService applicationService = new ApplicationService(null, auditLogDao, luceneApplicationIndexer, luceneApplicationSearcher);

        userAggregateService = new UserAggregateService(userIdentityService, userIdentityServiceV2, userApplicationRoleEntryDao, applicationService, luceneIndexer, auditLogDao);
    }

    @Before
    public void setUp() throws Exception {
        dbHelper.upgradeDatabase();
    }

    @After
    public void cleanUp() throws Exception {
        dbHelper.cleanDatabase();
        deleteTestDataDirectories();
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


    @Test
    public void test_addUserIdentity_ok() throws Exception {
        LDAPUserIdentity ldapUserIdentity = giveMeTestLdapUserIdentity();
        String json = new ObjectMapper().writeValueAsString(ldapUserIdentity);

        //when(ldapUserIdentityDao.deleteUserIdentity(any())).thenReturn(true);
        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(false);
        //when(ldapUserIdentityDao.addUserIdentity(any())).thenReturn(true);

        UserResource userResource = new UserResource(userIdentityService, userIdentityServiceV2, userAggregateService, new ObjectMapper());
        javax.ws.rs.core.Response response = userResource.addUserIdentity(json);
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

        LDAPUserIdentity userIdentity = new ObjectMapper().readValue(response.getEntity().toString(), LDAPUserIdentity.class);
        assertEquals(ldapUserIdentity, userIdentity);
    }

    @Test
    public void test_addUserIdentity_notOk() throws Exception {
        LDAPUserIdentity ldapUserIdentity = giveMeTestLdapUserIdentity();
        String json = new ObjectMapper().writeValueAsString(ldapUserIdentity);

        when(ldapUserIdentityDao.deleteUserIdentity(any())).thenReturn(true);
        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(true);
        when(ldapUserIdentityDao.addUserIdentity(any())).thenReturn(true);

        UserResource userResource = new UserResource(userIdentityService, userIdentityServiceV2, userAggregateService, new ObjectMapper());
        javax.ws.rs.core.Response response = userResource.addUserIdentity(json);
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

        LDAPUserIdentity userIdentity = new ObjectMapper().readValue(response.getEntity().toString(), LDAPUserIdentity.class);
        assertEquals(ldapUserIdentity, userIdentity);

        when(luceneUserSearch.usernameExists(any())).thenReturn(true);

        response = userResource.addUserIdentity(json);
        assertEquals(response.getStatus(), Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    public void test_getUserIdentity_from_ldap_and_db_ok() throws Exception {
        LDAPUserIdentity ldapUserIdentity = giveMeTestLdapUserIdentity();
        String uid = ldapUserIdentity.getUid();

        String json = new ObjectMapper().writeValueAsString(ldapUserIdentity);

        /** create useridentity */
        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(false);
        UserResource userResource = new UserResource(userIdentityService, userIdentityServiceV2, userAggregateService, new ObjectMapper());
        javax.ws.rs.core.Response response = userResource.addUserIdentity(json);
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

        /** get useridentity - useridentity exists in both ldap and db  */
        response = userResource.getUserIdentity(uid);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }


    @Test
    public void test_getUserIdentity_from_db_ok() throws Exception {
        LDAPUserIdentity ldapUserIdentity = giveMeTestLdapUserIdentity();
        String uid = ldapUserIdentity.getUid();

        String json = new ObjectMapper().writeValueAsString(ldapUserIdentity);

        /** create useridentity */
        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(false);
        UserResource userResource = new UserResource(userIdentityService, userIdentityServiceV2, userAggregateService, new ObjectMapper());
        javax.ws.rs.core.Response response = userResource.addUserIdentity(json);
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

        /** get useridentity - useridentity exists in both ldap and db  */
        response = userResource.getUserIdentity(uid);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());

        /** get useridentity - useridentity only exists in db  */
        when(userIdentityService.getUserIdentityForUid(any())).thenReturn(null);
        response = userResource.getUserIdentity(uid);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());


        /** get useridentity - after NamingException from LDAP occur  */
        when(userIdentityService.getUserIdentityForUid(any())).thenThrow(new NamingException(""));
        response = userResource.getUserIdentity(uid);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());


        LDAPUserIdentity userIdentity = new ObjectMapper().readValue(response.getEntity().toString(), LDAPUserIdentity.class);
        assertEquals(ldapUserIdentity, userIdentity);
    }



    @Test
    public void test_updateUserIdentity_ok() throws Exception {
        LDAPUserIdentity ldapUserIdentity = giveMeTestLdapUserIdentity();
        String uid = ldapUserIdentity.getUid();

        String json = new ObjectMapper().writeValueAsString(ldapUserIdentity);

        //when(ldapUserIdentityDao.deleteUserIdentity(any())).thenReturn(true);
        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(false);
        //when(ldapUserIdentityDao.addUserIdentity(any())).thenReturn(true);

        UserResource userResource = new UserResource(userIdentityService, userIdentityServiceV2, userAggregateService, new ObjectMapper());
        javax.ws.rs.core.Response response = userResource.addUserIdentity(json);
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

        LDAPUserIdentity userIdentity = new ObjectMapper().readValue(response.getEntity().toString(), LDAPUserIdentity.class);
        assertEquals(ldapUserIdentity, userIdentity);

        userIdentity.setFirstName("Edvard");
        userIdentity.setLastName("Teach");
        userIdentity.setEmail("test.testesen@gmail.com");

        String jsonUpdate = new ObjectMapper().writeValueAsString(userIdentity);

        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(true);

        response = userResource.updateUserIdentity(uid, jsonUpdate);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());

        LDAPUserIdentity updated = new ObjectMapper().readValue(response.getEntity().toString(), LDAPUserIdentity.class);

        assertEquals(updated.getUid(), uid);
        assertEquals(updated.getUsername(), ldapUserIdentity.getUsername());
        assertEquals(updated.getFirstName(), "Edvard");
        assertEquals(updated.getLastName(), "Teach");
        assertEquals(updated.getEmail(), "test.testesen@gmail.com");


    }

    @Test
    public void test_deleteUserIdentityAndRoles_ok() throws Exception {
        LDAPUserIdentity ldapUserIdentity = giveMeTestLdapUserIdentity();
        String uid = ldapUserIdentity.getUid();

        String json = new ObjectMapper().writeValueAsString(ldapUserIdentity);

        Response response = createUserIdentity(json);

        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

        LDAPUserIdentity userIdentity = new ObjectMapper().readValue(response.getEntity().toString(), LDAPUserIdentity.class);
        assertEquals(ldapUserIdentity, userIdentity);

        UserResource userResource = new UserResource(userIdentityService, userIdentityServiceV2, userAggregateService, new ObjectMapper());
        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(true);
        response = userResource.getUserIdentity(uid);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());

        when(ldapUserIdentityDao.getUserIndentity(any())).thenReturn(userIdentity);
        response = userResource.deleteUserIdentityAndRoles(uid);
        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());

        response = userResource.getUserIdentity(uid);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }



    private LDAPUserIdentity giveMeTestLdapUserIdentity() {
        String uid = UUID.randomUUID().toString();
        String username = "test.testesen@test.no";
        String firstname = "Test";
        String lastname = "Testesen";
        String email = username;
        String cellPhone = "+4290012345";
        String personRef = "123456789";

        UserIdentity userIdentity = new UserIdentity(uid, username, firstname, lastname, personRef, email, cellPhone);
        String password = new PasswordGenerator().generate();
        return new LDAPUserIdentity(userIdentity, password);
    }


    private Response createUserIdentity(String json) throws Exception {
        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(false);
        UserResource userResource = new UserResource(userIdentityService, userIdentityServiceV2, userAggregateService, new ObjectMapper());
        return userResource.addUserIdentity(json);
    }

    @AfterClass
    public static void deleteTestDataDirectories() {
        List<String> testDataDirectories = new ArrayList<String>()
        {{
            add(luceneUsersDirectory);
            add(luceneApplicationDirectory);
        }};

        testDataDirectories.stream().forEach(dir -> FileUtils.deleteDirectory(dir));
    }

}