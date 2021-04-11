package net.whydah.identity.user.identity;


import net.whydah.identity.Main;
import net.whydah.identity.application.ApplicationDao;
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
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;


public class UserIdentityServiceV2Test {
    private final static String basepath = "target/data/UserIdentityServiceV2Test/";
    private final static String dbpath = basepath + "hsqldb/roles";

    private static BasicDataSource basicDataSource;
    private static AuditLogDao auditLogDao;
    private static PasswordGenerator passwordGenerator;
    private static LuceneUserIndexer luceneUserIndexer;
    private static LuceneUserSearch luceneUserSearch = Mockito.mock(LuceneUserSearch.class);
    private static DatabaseMigrationHelper dbHelper;
    private static RDBMSLdapUserIdentityRepository rdbmsLdapUserIdentityRepository;
    private static ConstrettoConfiguration constrettoConfiguration;

    private static UserIdentityServiceV2 userIdentityService;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void init() throws Exception {
        constrettoConfiguration = initConstrettoConfiguration();
        basicDataSource = initBasicDataSource();
        dbHelper = new DatabaseMigrationHelper(basicDataSource);
        auditLogDao = new AuditLogDao(basicDataSource);
        passwordGenerator = new PasswordGenerator();

        String luceneUsersDir = constrettoConfiguration.evaluateToString("lucene.usersdirectory");
        Directory index = new NIOFSDirectory(Paths.get(new File(luceneUsersDir).getPath()));
        FileUtils.deleteDirectories(luceneUsersDir);
        luceneUserIndexer = new LuceneUserIndexer(index);

        RDBMSLdapUserIdentityDao rdbmsLdapUserIdentityDao = new RDBMSLdapUserIdentityDao(basicDataSource);
        rdbmsLdapUserIdentityRepository = new RDBMSLdapUserIdentityRepository(rdbmsLdapUserIdentityDao, constrettoConfiguration);

        userIdentityService = new UserIdentityServiceV2(rdbmsLdapUserIdentityRepository, auditLogDao, passwordGenerator, luceneUserIndexer, luceneUserSearch);
    }

    @Before
    public void setUp() throws Exception {
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();
    }

    @After
    public void tearDown() throws Exception {

    }

    private static ConstrettoConfiguration initConstrettoConfiguration() {
        return new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("classpath:useridentitybackend-test.properties"))
                .done()
                .getConfiguration();
    }

    private static BasicDataSource initBasicDataSource() {
        basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        basicDataSource.setUsername("sa");
        basicDataSource.setPassword("");
        basicDataSource.setUrl("jdbc:hsqldb:file:" + dbpath);
        return basicDataSource;
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

        RDBMSUserIdentity stored = userIdentityService.addUserIdentityWithGeneratedPassword(userIdentity);

        assertNotNull("UserIdentity stored", stored);
    }

    @Test
    public void test_get() {
        UserIdentity userIdentity = giveMeTestUserIdentity();

        userIdentityService.addUserIdentityWithGeneratedPassword(userIdentity);

        RDBMSUserIdentity fromDB = userIdentityService.getUserIdentity(userIdentity.getUsername());

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
        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);

        userIdentityService.addUserIdentityWithGeneratedPassword(userIdentity);
        RDBMSUserIdentity fromDB = userIdentityService.getUserIdentity(userIdentity.getUsername());

        assertNotNull("UserIdentity found", fromDB);

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("usernameExist failed for username=test.testesen@test.no");
        userIdentityService.addUserIdentityWithGeneratedPassword(userIdentity);
    }

    @Test
    public void test_change_all_but_uid_username_and_password() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);

        userIdentityService.addUserIdentityWithGeneratedPassword(userIdentity);

        RDBMSUserIdentity fromDB = userIdentityService.getUserIdentity(userIdentity.getUsername());

        String newCellPhoneNumber = "+4790090000";
        fromDB.setCellPhone(newCellPhoneNumber);
        fromDB.setFirstName("Firstname");
        fromDB.setLastName("Lastname");
        fromDB.setPersonRef("1234567890");
        fromDB.setEmail("new.email@email.no");

        RDBMSUserIdentity beforeUpdate = userIdentityService.getUserIdentity(userIdentity.getUsername());

        userIdentityService.updateUserIdentity(fromDB.getUsername(), fromDB);

        RDBMSUserIdentity updated = userIdentityService.getUserIdentity(fromDB.getUsername());

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

        userIdentityService.addUserIdentityWithGeneratedPassword(userIdentity);

        RDBMSUserIdentity fromDB = userIdentityService.getUserIdentity(userIdentity.getUsername());

        String newPassword = passwordGenerator.generate();
        userIdentityService.changePassword(fromDB.getUsername(), fromDB.getUid(), newPassword);

        RDBMSUserIdentity fromDBWithChangedPassword = userIdentityService.getUserIdentity(userIdentity.getUsername());
        assertNotEquals(fromDB.getPassword(), fromDBWithChangedPassword.getPassword());
    }

    @Test
    public void test_authenticate() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);
        userIdentityService.addUserIdentityWithGeneratedPassword(userIdentity);
        RDBMSUserIdentity fromDB = userIdentityService.getUserIdentity(userIdentity.getUsername());

        String username = fromDB.getUsername();
        String password = fromDB.getPassword();

        RDBMSUserIdentity authenticated = userIdentityService.authenticate(username, password);

        assertEquals(fromDB, authenticated);
    }

    @Test
    public void test_fail_on_authenticate() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);
        userIdentityService.addUserIdentityWithGeneratedPassword(userIdentity);
        RDBMSUserIdentity fromDB = userIdentityService.getUserIdentity(userIdentity.getUsername());

        String username = fromDB.getUsername();
        String password = passwordGenerator.generate();

        RDBMSUserIdentity authenticated = userIdentityService.authenticate(username, password);

        assertNull("Authentication failed, return null", authenticated);
    }

    @Test
    public void test_delete() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);
        userIdentityService.addUserIdentityWithGeneratedPassword(userIdentity);
        RDBMSUserIdentity fromDB = userIdentityService.getUserIdentity(userIdentity.getUsername());

        assertNotNull(fromDB);

        userIdentityService.deleteUserIdentity(fromDB.getUsername());

        RDBMSUserIdentity deleted = userIdentityService.getUserIdentity(userIdentity.getUsername());
        assertNull("UserIdentity deleted", deleted);
    }
}
