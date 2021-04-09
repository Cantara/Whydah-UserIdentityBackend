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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
    public void test_add_new_useridentity() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityServiceV2 userIdentityService = new UserIdentityServiceV2(rdbmsLdapUserIdentityRepository, auditLogDao, passwordGenerator, luceneUserIndexer, luceneUserSearch);
        RDBMSUserIdentity stored = userIdentityService.addUserIdentityWithGeneratedPassword(userIdentity);
        assertNotNull("UserIdentity stored", stored);
    }

    @Test
    public void test_add_and_get_new_useridentity() {
        UserIdentity userIdentity = giveMeTestUserIdentity();

        UserIdentityServiceV2 userIdentityService = new UserIdentityServiceV2(rdbmsLdapUserIdentityRepository, auditLogDao, passwordGenerator, luceneUserIndexer, luceneUserSearch);
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
        UserIdentityServiceV2 userIdentityService = new UserIdentityServiceV2(rdbmsLdapUserIdentityRepository, auditLogDao, passwordGenerator, luceneUserIndexer, luceneUserSearch);

        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);

        userIdentityService.addUserIdentityWithGeneratedPassword(userIdentity);
        RDBMSUserIdentity fromDB = userIdentityService.getUserIdentity(userIdentity.getUsername());
        assertNotNull("UserIdentity found", fromDB);

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("usernameExist failedd for username=test.testesen@test.no");
        userIdentityService.addUserIdentityWithGeneratedPassword(userIdentity);
    }

    @Test
    public void test_change_cellphone() {
        UserIdentity userIdentity = giveMeTestUserIdentity();
        UserIdentityServiceV2 userIdentityService = new UserIdentityServiceV2(rdbmsLdapUserIdentityRepository, auditLogDao, passwordGenerator, luceneUserIndexer, luceneUserSearch);

        when(luceneUserSearch.usernameExists(userIdentity.getUsername())).thenReturn(true);

        userIdentityService.addUserIdentityWithGeneratedPassword(userIdentity);
        RDBMSUserIdentity fromDB = userIdentityService.getUserIdentity(userIdentity.getUsername());

        String newCellPhoneNumber = "+4790090000";
        fromDB.setCellPhone(newCellPhoneNumber);
        userIdentityService.updateUserIdentity(fromDB.getUsername(), fromDB);

        RDBMSUserIdentity updated = userIdentityService.getUserIdentity(fromDB.getUsername());
        assertEquals(fromDB.getUid(), updated.getUid());
        assertEquals(fromDB.getUsername(), updated.getUsername());

        assertEquals(newCellPhoneNumber, updated.getCellPhone());


    }



}
