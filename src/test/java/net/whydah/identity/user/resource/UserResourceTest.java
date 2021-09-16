package net.whydah.identity.user.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.identity.application.ApplicationService;
import net.whydah.identity.application.search.LuceneApplicationIndexer;
import net.whydah.identity.application.search.LuceneApplicationSearch;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.BCryptService;
import net.whydah.identity.user.identity.LdapAuthenticator;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.user.identity.RDBMSLdapUserIdentityDao;
import net.whydah.identity.user.identity.RDBMSLdapUserIdentityRepository;
import net.whydah.identity.user.identity.RDBMSUserIdentity;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.identity.UserIdentityServiceV2;
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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


public class UserResourceTest {
    private static final Logger log = LoggerFactory.getLogger(UserResourceTest.class);

    private static LdapAuthenticator ldapAuthenticator = Mockito.mock(LdapAuthenticator.class);
    private static LdapUserIdentityDao ldapUserIdentityDao = Mockito.mock(LdapUserIdentityDao.class);;

    private static String luceneApplicationDirectory;
    private static String luceneUsersDirectory;

    private static UserIdentityService userIdentityService;
    private static UserIdentityServiceV2 userIdentityServiceV2;
    private static UserAggregateService userAggregateService;

    private static RDBMSLdapUserIdentityDao rdbmsLdapUserIdentityDao;
    private static RDBMSLdapUserIdentityRepository rdbmsUserIdentityRepository;

    private static DatabaseMigrationHelper dbHelper;

    private static LuceneUserSearch luceneUserSearch;

    private static ConstrettoConfiguration configuration;
    private static BCryptService bCryptService;

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
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();

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

        bCryptService = new BCryptService(configuration.evaluateToString("userdb.password.pepper"), configuration.evaluateToInt("userdb.password.bcrypt.preferredcost"));

        rdbmsLdapUserIdentityDao = new RDBMSLdapUserIdentityDao(dataSource);
        rdbmsUserIdentityRepository = new RDBMSLdapUserIdentityRepository(rdbmsLdapUserIdentityDao, bCryptService, configuration);
        userIdentityServiceV2 = new UserIdentityServiceV2(rdbmsUserIdentityRepository, auditLogDao, luceneIndexer, luceneUserSearch, bCryptService);

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
        RDBMSUserIdentity rdbmsUserIdentity = giveMeTestRDBMSUserIdentity();
        String json = new ObjectMapper().writeValueAsString(rdbmsUserIdentity);

        //when(ldapUserIdentityDao.deleteUserIdentity(any())).thenReturn(true);
        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(false);
        //when(ldapUserIdentityDao.addUserIdentity(any())).thenReturn(true);

        UserResource userResource = new UserResource(userIdentityService, userIdentityServiceV2, userAggregateService, new ObjectMapper(), bCryptService);
        javax.ws.rs.core.Response response = userResource.addUserIdentity(json);
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

        RDBMSUserIdentity userIdentity = new ObjectMapper().readValue(response.getEntity().toString(), RDBMSUserIdentity.class);
        assertEquals(rdbmsUserIdentity, userIdentity);
    }

    @Test
    public void test_addUserIdentity_notOk() throws Exception {
        RDBMSUserIdentity rdbmsUserIdentity = giveMeTestRDBMSUserIdentity();
        String json = new ObjectMapper().writeValueAsString(rdbmsUserIdentity);

        when(ldapUserIdentityDao.deleteUserIdentity(any())).thenReturn(true);
        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(true);
        when(ldapUserIdentityDao.addUserIdentity(any())).thenReturn(true);

        UserResource userResource = new UserResource(userIdentityService, userIdentityServiceV2, userAggregateService, new ObjectMapper(), bCryptService);
        javax.ws.rs.core.Response response = userResource.addUserIdentity(json);
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

        RDBMSUserIdentity userIdentity = new ObjectMapper().readValue(response.getEntity().toString(), RDBMSUserIdentity.class);
        assertEquals(rdbmsUserIdentity, userIdentity);

        when(luceneUserSearch.usernameExists(any())).thenReturn(true);

        response = userResource.addUserIdentity(json);
        assertEquals(response.getStatus(), Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    public void test_getUserIdentity_from_ldap_and_db_ok() throws Exception {
        RDBMSUserIdentity rdbmsUserIdentity = giveMeTestRDBMSUserIdentity();
        String uid = rdbmsUserIdentity.getUid();

        String json = new ObjectMapper().writeValueAsString(rdbmsUserIdentity);

        /** create useridentity */
        UserResource userResource = new UserResource(userIdentityService, userIdentityServiceV2, userAggregateService, new ObjectMapper(), bCryptService);
        javax.ws.rs.core.Response response = userResource.addUserIdentity(json);
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

        //when(ldapUserIdentityDao.usernameExist(any())).thenReturn(false);

        /* get useridentity - useridentity exists in both ldap and db  */
        response = userResource.getUserIdentity(uid);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }


    @Test
    public void test_getUserIdentity_from_db_ok() throws Exception {
        RDBMSUserIdentity rdbmsUserIdentity = giveMeTestRDBMSUserIdentity();
        String uid = rdbmsUserIdentity.getUid();

        String json = new ObjectMapper().writeValueAsString(rdbmsUserIdentity);

        /** create useridentity */
        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(false);
        UserResource userResource = new UserResource(userIdentityService, userIdentityServiceV2, userAggregateService, new ObjectMapper(), bCryptService);
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


        RDBMSUserIdentity userIdentity = new ObjectMapper().readValue(response.getEntity().toString(), RDBMSUserIdentity.class);
        assertEquals(rdbmsUserIdentity, userIdentity);
    }



    @Test
    public void test_updateUserIdentity_ok() throws Exception {
        RDBMSUserIdentity rdbmsUserIdentity = giveMeTestRDBMSUserIdentity();
        String uid = rdbmsUserIdentity.getUid();

        String json = new ObjectMapper().writeValueAsString(rdbmsUserIdentity);

        //when(ldapUserIdentityDao.deleteUserIdentity(any())).thenReturn(true);
        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(false);
        //when(ldapUserIdentityDao.addUserIdentity(any())).thenReturn(true);

        UserResource userResource = new UserResource(userIdentityService, userIdentityServiceV2, userAggregateService, new ObjectMapper(), bCryptService);
        javax.ws.rs.core.Response response = userResource.addUserIdentity(json);
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

        RDBMSUserIdentity userIdentity = new ObjectMapper().readValue(response.getEntity().toString(), RDBMSUserIdentity.class);
        assertEquals(rdbmsUserIdentity, userIdentity);

        userIdentity.setFirstName("Edvard");
        userIdentity.setLastName("Teach");
        userIdentity.setEmail("test.testesen@gmail.com");

        String jsonUpdate = new ObjectMapper().writeValueAsString(userIdentity);

        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(true);

        response = userResource.updateUserIdentity(uid, jsonUpdate);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());

        RDBMSUserIdentity updated = new ObjectMapper().readValue(response.getEntity().toString(), RDBMSUserIdentity.class);

        assertEquals(updated.getUid(), uid);
        assertEquals(updated.getUsername(), rdbmsUserIdentity.getUsername());
        assertEquals(updated.getFirstName(), "Edvard");
        assertEquals(updated.getLastName(), "Teach");
        assertEquals(updated.getEmail(), "test.testesen@gmail.com");


    }

    @Test
    public void test_deleteUserIdentityAndRoles_ok() throws Exception {
        String json = new ObjectMapper().writeValueAsString(giveMeTestRDBMSUserIdentity());

        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(false);
        UserResource userResource = new UserResource(userIdentityService, userIdentityServiceV2, userAggregateService, new ObjectMapper(), bCryptService);
        Response addResponse = userResource.addUserIdentity(json);
        assertEquals(addResponse.getStatus(), Response.Status.CREATED.getStatusCode());

        String addedJson = addResponse.getEntity().toString();
        UserIdentity addedUserIdentity = new ObjectMapper().readValue(addedJson, RDBMSUserIdentity.class);

        String uid = addedUserIdentity.getUid();
        log.info("User={} added with username={}", uid, addedUserIdentity.getUsername());

        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(true);
        Response deleteResponse = userResource.deleteUserIdentityAndRoles(uid);
        assertEquals(deleteResponse.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
    }



    private RDBMSUserIdentity giveMeTestRDBMSUserIdentity() {
        String uid = UUID.randomUUID().toString();
        String username = "test.testesen@test.no";
        String firstname = "Test";
        String lastname = "Testesen";
        String email = username;
        String cellPhone = "+4290012345";
        String personRef = "123456789";

        UserIdentity userIdentity = new UserIdentity(uid, username, firstname, lastname, personRef, email, cellPhone);
        String password = new PasswordGenerator().generate();
        return new RDBMSUserIdentity(userIdentity, password);
    }


    private Response addUser(RDBMSUserIdentity rdbmsUserIdentity) throws Exception {
        when(ldapUserIdentityDao.usernameExist(any())).thenReturn(false);
        UserResource userResource = new UserResource(userIdentityService, userIdentityServiceV2, userAggregateService, new ObjectMapper(), bCryptService);

        String uid = rdbmsUserIdentity.getUid();

        String json = new ObjectMapper().writeValueAsString(rdbmsUserIdentity);
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