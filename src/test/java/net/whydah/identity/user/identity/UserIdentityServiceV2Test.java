package net.whydah.identity.user.identity;

import net.whydah.identity.application.search.LuceneApplicationIndexer;
import net.whydah.identity.application.search.LuceneApplicationSearch;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
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
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;


public class UserIdentityServiceV2Test {
    private static PasswordGenerator passwordGenerator;
    private static DatabaseMigrationHelper dbHelper;
    private static RDBMSLdapUserIdentityRepository rdbmsLdapUserIdentityRepository;
    private static ConstrettoConfiguration configuration;

    private static LuceneUserSearch luceneUserSearch;
    private static String luceneApplicationDirectory;
    private static String luceneUsersDirectory;

    private static UserIdentityServiceV2 userIdentityServiceV2;

    private String password = new PasswordGenerator().generate();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void init() throws Exception {
        configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
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

        passwordGenerator = new PasswordGenerator();
        AuditLogDao auditLogDao = new AuditLogDao(dataSource);

        String luceneUsersDir = configuration.evaluateToString("lucene.usersdirectory");
        Directory index = new NIOFSDirectory(Paths.get(new File(luceneUsersDir).getPath()));
        FileUtils.deleteDirectories(luceneUsersDir);
        LuceneUserIndexer luceneUserIndexer= new LuceneUserIndexer(index);

        RDBMSLdapUserIdentityDao rdbmsLdapUserIdentityDao = new RDBMSLdapUserIdentityDao(dataSource);
        rdbmsLdapUserIdentityRepository = new RDBMSLdapUserIdentityRepository(rdbmsLdapUserIdentityDao, configuration);

        PasswordGenerator passwordGenerator = new PasswordGenerator();
        userIdentityServiceV2 = new UserIdentityServiceV2(rdbmsLdapUserIdentityRepository, auditLogDao, passwordGenerator, luceneUserIndexer, luceneUserSearch);
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

    @AfterClass
    public static void deleteTestDataDirectories() {
        List<String> testDataDirectories = new ArrayList<String>()
        {{
            add(luceneUsersDirectory);
            add(luceneApplicationDirectory);
        }};

        testDataDirectories.stream().forEach(dir -> FileUtils.deleteDirectory(dir));
    }

    private static ConstrettoConfiguration initConstrettoConfiguration() {
        return new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("classpath:useridentitybackend-test.properties"))
                .done()
                .getConfiguration();
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

    private UserIdentity giveMeTestUserIdentity() {
        String uid = UUID.randomUUID().toString();
        String username = "test.testesen@test.no";
        String firstname = "Test";
        String lastname = "Testesen";
        String email = username;
        String cellPhone = "+4290012345";
        String personRef = "123456789";

        return new UserIdentity(uid, username, firstname, lastname, personRef, email, cellPhone);
    }

    @Test
    public void test_add() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityExtension userIdentityExtension = new UserIdentityExtension(userIdentity);

        RDBMSUserIdentity stored = userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);

        assertNotNull("UserIdentity stored", stored);
    }

    @Test
    public void test_get() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityExtension userIdentityExtension = new UserIdentityExtension(userIdentity);

        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);

        RDBMSUserIdentity fromDB = userIdentityServiceV2.getUserIdentity(userIdentity.getUsername());

        assertNotNull("UserIdentity found", fromDB);
        assertEquals(userIdentity.getUid(), fromDB.getUid());
        assertEquals(userIdentity.getUsername(), fromDB.getUsername());
        assertEquals(userIdentity.getFirstName(), fromDB.getFirstName());
        assertEquals(userIdentity.getLastName(), fromDB.getLastName());
        assertEquals(userIdentity.getEmail(), fromDB.getEmail());
        assertEquals(userIdentity.getCellPhone(), fromDB.getCellPhone());
        assertEquals(userIdentity.getPersonRef(), fromDB.getPersonRef());
        assertNotNull(fromDB.getPassword());
    }

    @Test
    public void test_reject_useridentity_if_exists() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityExtension userIdentityExtension = new UserIdentityExtension(userIdentity);
        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);

        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);
        RDBMSUserIdentity fromDB = userIdentityServiceV2.getUserIdentity(userIdentity.getUsername());

        assertNotNull("UserIdentity found", fromDB);

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("usernameExist failed for username=test.testesen@test.no");
        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);
    }

    @Test
    public void test_change_all_but_uid_username_and_password() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityExtension userIdentityExtension = new UserIdentityExtension(userIdentity);
        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(false);

        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);

        RDBMSUserIdentity fromDB = userIdentityServiceV2.getUserIdentity(userIdentity.getUsername());

        String newCellPhoneNumber = "+4790090000";
        fromDB.setCellPhone(newCellPhoneNumber);
        fromDB.setFirstName("Firstname");
        fromDB.setLastName("Lastname");
        fromDB.setPersonRef("1234567890");
        fromDB.setEmail("new.email@email.no");

        RDBMSUserIdentity beforeUpdate = userIdentityServiceV2.getUserIdentity(userIdentity.getUsername());

        userIdentityServiceV2.updateUserIdentity(fromDB.getUsername(), fromDB);

        RDBMSUserIdentity updated = userIdentityServiceV2.getUserIdentity(fromDB.getUsername());

        assertEquals(beforeUpdate.getUid(), updated.getUid());
        assertEquals(beforeUpdate.getUsername(), updated.getUsername());
        assertNotEquals(updated.getFirstName(), beforeUpdate.getFirstName());
        assertNotEquals(updated.getLastName(), beforeUpdate.getLastName());
        assertNotEquals(updated.getCellPhone(), beforeUpdate.getCellPhone());
        assertNotEquals(updated.getPersonRef(), beforeUpdate.getPersonRef());
        assertNotEquals(updated.getEmail(), beforeUpdate.getEmail());
    }

    @Test
    public void test_password_change() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityExtension userIdentityExtension = new UserIdentityExtension(userIdentity);

        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);

        RDBMSUserIdentity fromDB = userIdentityServiceV2.getUserIdentity(userIdentity.getUsername());

        String newPassword = passwordGenerator.generate();
        userIdentityServiceV2.changePassword(fromDB.getUsername(), fromDB.getUid(), newPassword);

        RDBMSUserIdentity fromDBWithChangedPassword = userIdentityServiceV2.getUserIdentity(userIdentity.getUsername());
        assertNotEquals(fromDB.getPassword(), fromDBWithChangedPassword.getPassword());
    }

    @Test
    public void test_authenticate() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityExtension userIdentityExtension = new UserIdentityExtension(userIdentity);

        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);

        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);
        RDBMSUserIdentity fromDB = userIdentityServiceV2.getUserIdentity(userIdentity.getUsername());

        String username = fromDB.getUsername();
        String password = fromDB.getPassword();

        RDBMSUserIdentity authenticated = userIdentityServiceV2.authenticate(username, password);

        assertEquals(fromDB, authenticated);
    }

    @Test
    public void test_fail_on_authenticate() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityExtension userIdentityExtension = new UserIdentityExtension(userIdentity);
        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);
        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);
        RDBMSUserIdentity fromDB = userIdentityServiceV2.getUserIdentity(userIdentity.getUsername());

        String username = fromDB.getUsername();
        String password = passwordGenerator.generate();

        RDBMSUserIdentity authenticated = userIdentityServiceV2.authenticate(username, password);

        assertNull("Authentication failed, return null", authenticated);
    }

    @Test
    public void test_delete() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityExtension userIdentityExtension = new UserIdentityExtension(userIdentity);
        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);
        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);
        RDBMSUserIdentity fromDB = userIdentityServiceV2.getUserIdentity(userIdentity.getUsername());

        assertNotNull(fromDB);

        userIdentityServiceV2.deleteUserIdentity(fromDB.getUsername());

        RDBMSUserIdentity deleted = userIdentityServiceV2.getUserIdentity(userIdentity.getUsername());
        assertNull("UserIdentity deleted", deleted);
    }
}
