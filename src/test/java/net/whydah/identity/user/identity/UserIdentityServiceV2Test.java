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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
    private static BCryptService bCryptService;

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

        passwordGenerator = new PasswordGenerator();
        AuditLogDao auditLogDao = new AuditLogDao(dataSource);

        String luceneUsersDir = configuration.evaluateToString("lucene.usersdirectory");
        Directory index = new NIOFSDirectory(Paths.get(new File(luceneUsersDir).getPath()));
        FileUtils.deleteDirectories(luceneUsersDir);
        LuceneUserIndexer luceneUserIndexer= new LuceneUserIndexer(index);

        RDBMSLdapUserIdentityDao rdbmsLdapUserIdentityDao = new RDBMSLdapUserIdentityDao(dataSource);

        bCryptService = new BCryptService(configuration.evaluateToString("userdb.password.pepper"), configuration.evaluateToInt("userdb.password.bcrypt.preferredcost"));
        rdbmsLdapUserIdentityRepository = new RDBMSLdapUserIdentityRepository(rdbmsLdapUserIdentityDao, bCryptService, configuration);

        userIdentityServiceV2 = new UserIdentityServiceV2(rdbmsLdapUserIdentityRepository, auditLogDao, luceneUserIndexer, luceneUserSearch, bCryptService);
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
        UserIdentityWithAutomaticPasswordGeneration userIdentityExtension = new UserIdentityWithAutomaticPasswordGeneration(userIdentity);

        RDBMSUserIdentity stored = userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);

        assertNotNull("UserIdentity stored", stored);
    }

    @Test
    public void test_get() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityWithAutomaticPasswordGeneration userIdentityExtension = new UserIdentityWithAutomaticPasswordGeneration(userIdentity);

        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(false);
        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);

        RDBMSUserIdentity fromDB = userIdentityServiceV2.getUserIdentity(userIdentity.getUid());

        assertNotNull("UserIdentity found", fromDB);
        assertEquals(userIdentity.getUid(), fromDB.getUid());
        assertEquals(userIdentity.getUsername(), fromDB.getUsername());
        assertEquals(userIdentity.getFirstName(), fromDB.getFirstName());
        assertEquals(userIdentity.getLastName(), fromDB.getLastName());
        assertEquals(userIdentity.getEmail(), fromDB.getEmail());
        assertEquals(userIdentity.getCellPhone(), fromDB.getCellPhone());
        assertEquals(userIdentity.getPersonRef(), fromDB.getPersonRef());
        assertNull(fromDB.getPassword());
        assertNotNull(fromDB.getPasswordBCrypt());
    }

    @Test
    public void test_reject_useridentity_if_exists() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityWithAutomaticPasswordGeneration userIdentityExtension = new UserIdentityWithAutomaticPasswordGeneration(userIdentity);
        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);

        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);
        RDBMSUserIdentity fromDB = userIdentityServiceV2.getUserIdentity(userIdentity.getUid());

        assertNotNull("UserIdentity found", fromDB);

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("ser already exists, could not create user with username=" + userIdentity.getUsername());
        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);
    }

    @Test
    public void test_update() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityWithAutomaticPasswordGeneration userIdentityExtension = new UserIdentityWithAutomaticPasswordGeneration(userIdentity);
        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(false);

        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);

        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);
        RDBMSUserIdentity beforeUpdate = userIdentityServiceV2.getUserIdentity(userIdentity.getUid());

        RDBMSUserIdentity fromDB = userIdentityServiceV2.getUserIdentity(userIdentity.getUid());
        fromDB.setCellPhone("+4790090000");
        fromDB.setFirstName("Firstname");
        fromDB.setLastName("Lastname");
        fromDB.setPersonRef("1234567890");
        fromDB.setEmail("new.email@email.no");


        userIdentityServiceV2.updateUserIdentity(fromDB.getUid(), fromDB);

        RDBMSUserIdentity updated = userIdentityServiceV2.getUserIdentity(fromDB.getUid());

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
        UserIdentityWithAutomaticPasswordGeneration userIdentityExtension = new UserIdentityWithAutomaticPasswordGeneration(userIdentity);

        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);

        RDBMSUserIdentity fromDB = userIdentityServiceV2.getUserIdentity(userIdentity.getUid());

        assertTrue(bCryptService.verifyPassword(fromDB.getPasswordBCrypt(), userIdentityExtension.getPassword()));

        String newPassword = passwordGenerator.generate();
        userIdentityServiceV2.changePassword(fromDB.getUsername(), fromDB.getUid(), newPassword);

        RDBMSUserIdentity fromDBWithChangedPassword = userIdentityServiceV2.getUserIdentity(userIdentity.getUid());

        assertFalse(bCryptService.verifyPassword(fromDBWithChangedPassword.getPasswordBCrypt(), userIdentityExtension.getPassword()));
        assertTrue(bCryptService.verifyPassword(fromDBWithChangedPassword.getPasswordBCrypt(), newPassword));

        assertNotEquals(newPassword, userIdentityExtension.getPassword());
        assertNotEquals(fromDB.getPasswordBCrypt(), fromDBWithChangedPassword.getPasswordBCrypt());
    }

    @Test
    public void test_password_reset_then_set() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityWithAutomaticPasswordGeneration userIdentityExtension = new UserIdentityWithAutomaticPasswordGeneration(userIdentity);

        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);

        RDBMSUserIdentity fromDB = userIdentityServiceV2.getUserIdentity(userIdentity.getUid());

        assertTrue(bCryptService.verifyPassword(fromDB.getPasswordBCrypt(), userIdentityExtension.getPassword()));

        String preGeneratedPassword = passwordGenerator.generate();
        String preGeneratedSaltPassword = passwordGenerator.generate();

        String changePasswordToken = userIdentityServiceV2.setTempPassword(fromDB.getUsername(), fromDB.getUid(), preGeneratedPassword, preGeneratedSaltPassword);

        boolean authenticateWithChangePasswordToken = userIdentityServiceV2.authenticateWithChangePasswordToken(fromDB.getUsername(), changePasswordToken);
        assertEquals(true, authenticateWithChangePasswordToken);

        String newPassword = passwordGenerator.generate();
        userIdentityServiceV2.changePassword(fromDB.getUsername(), fromDB.getUid(), newPassword);

        RDBMSUserIdentity fromDBWithChangedPassword = userIdentityServiceV2.getUserIdentity(userIdentity.getUid());

        assertFalse(bCryptService.verifyPassword(fromDBWithChangedPassword.getPasswordBCrypt(), userIdentityExtension.getPassword()));
        assertTrue(bCryptService.verifyPassword(fromDBWithChangedPassword.getPasswordBCrypt(), newPassword));

        assertNotEquals(newPassword, userIdentityExtension.getPassword());
        assertNotEquals(fromDB.getPasswordBCrypt(), fromDBWithChangedPassword.getPasswordBCrypt());
    }

    @Test
    public void test_authenticate() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityWithAutomaticPasswordGeneration userIdentityExtension = new UserIdentityWithAutomaticPasswordGeneration(userIdentity);

        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);

        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);
        RDBMSUserIdentity fromDB = userIdentityServiceV2.getUserIdentity(userIdentity.getUid());

        String username = fromDB.getUsername();
        String password = userIdentityExtension.getPassword();

        RDBMSUserIdentity authenticated = userIdentityServiceV2.authenticate(username, password);

        assertEquals(fromDB, authenticated);
    }

    @Test
    public void test_fail_on_authenticate() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityWithAutomaticPasswordGeneration userIdentityExtension = new UserIdentityWithAutomaticPasswordGeneration(userIdentity);
        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);
        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);
        RDBMSUserIdentity fromDB = userIdentityServiceV2.getUserIdentity(userIdentity.getUid());

        String username = fromDB.getUsername();
        String password = passwordGenerator.generate();

        RDBMSUserIdentity authenticated = userIdentityServiceV2.authenticate(username, password);

        assertNull("Authentication failed, return null", authenticated);
    }

    @Test
    public void test_delete() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityWithAutomaticPasswordGeneration userIdentityExtension = new UserIdentityWithAutomaticPasswordGeneration(userIdentity);
        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);
        userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);
        RDBMSUserIdentity fromDB = userIdentityServiceV2.getUserIdentity(userIdentity.getUid());

        assertNotNull(fromDB);

        userIdentityServiceV2.deleteUserIdentity(fromDB.getUsername());

        RDBMSUserIdentity deleted = userIdentityServiceV2.getUserIdentity(userIdentity.getUid());
        assertNull("UserIdentity deleted", deleted);
    }
}
