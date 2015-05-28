package net.whydah.identity.dataimport;

import com.jayway.restassured.RestAssured;
import net.whydah.identity.Main;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.user.UserAggregate;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import net.whydah.identity.util.FileUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IamDataImporterTest {
    private final static String basepath = "target/UserAuthenticationEndpointTest/";
    /*
	private static final String lucenePath = basepath + "lucene";
    private final static String ldappath = basepath + "hsqldb/ldap/";
    private final static String dbpath = basepath + "hsqldb/roles";
    */

    //private static EmbeddedADS ads;

    //must be static to use JUnit BeforeClass, switch to TestNG?
    private static BasicDataSource dataSource;
    private static IamDataImporter dataImporter;
    private static Main main;


    /*
    @BeforeClass
    public static void init() throws Exception {
        System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);
    	FileUtils.deleteDirectory(new File(basepath + "/hsqldb"));
        FileUtils.deleteDirectory(new File(lucenePath));

        int LDAP_PORT = new Integer(AppConfig.appConfig.getProperty("ldap.embedded.port"));
        String LDAP_URL = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";
        
        File ldapdir = new File(ldappath);
        ldapdir.mkdirs();
        ads = new EmbeddedADS(ldappath);
        ads.startServer(LDAP_PORT);
        String readOnly = AppConfig.appConfig.getProperty("ldap.primary.readonly");
        ldapUserIdentityDao = new LdapUserIdentityDao(LDAP_URL, "uid=admin,ou=system", "secret", "uid", "initials", readOnly);


        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:file:" + dbpath);
        //QueryRunner queryRunner = new QueryRunner(dataSource);

        new DatabaseMigrationHelper(dataSource).upgradeDatabase();

        ApplicationDao configDataRepository = new ApplicationDao(dataSource);
        roleRepository = new UserPropertyAndRoleRepository(new UserPropertyAndRoleDao(dataSource), configDataRepository);

        //index = new NIOFSDirectory(new File(lucenePath));
    }
    */
    @BeforeClass
    public static void startServer() {
        //System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);
        //System.setProperty(ConfigTags.CONSTRETTO_TAGS, ConfigTags.DEV_MODE);
        ApplicationMode.setDevMode();
        final ConstrettoConfiguration configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("file:./useridentitybackend_override.properties"))
                .done()
                .getConfiguration();

        //String roleDBDirectory = AppConfig.appConfig.getProperty("roledb.directory");
        String ldapEmbeddedpath = configuration.evaluateToString("ldap.embedded.directory");
        String roleDBDirectory = configuration.evaluateToString("roledb.directory");
        FileUtils.deleteDirectories(roleDBDirectory, ldapEmbeddedpath);

        Integer ldapPort = configuration.evaluateToInt("ldap.embedded.port");

        main = new Main(6655);
        main.startEmbeddedDS(ldapEmbeddedpath, ldapPort);


        dataSource = initBasicDataSource(configuration);
        new DatabaseMigrationHelper(dataSource).upgradeDatabase();

        dataImporter = new IamDataImporter(dataSource, configuration);

        main.start();
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;
        //mapper = new ObjectMapper();
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

    /*
    @AfterClass
    public static void tearDown() throws Exception {
        if (ads != null) {
            ads.stopServer();
        }
    }
    */
    @AfterClass
    public static void stop() {
        if (main != null) {
            main.stop();
        }
    }
    
    @Test
    public void testDataIsImported() throws Exception {
		//IamDataImporter iamDataImporter = new IamDataImporter(applicationImporter, organizationImporter, userImporter, roleMappingImporter);
        //new IamDataImporter(dataSource, ldapUserIdentityDao, lucenePath).importIamData();
        dataImporter.importIamData();
        LdapUserIdentityDao ldapUserIdentityDao = dataImporter.getLdapUserIdentityDao();
        UserPropertyAndRoleRepository roleRepository = dataImporter.getUserPropertyAndRoleRepository();

        UserIdentity thomaspUserIdentity = ldapUserIdentityDao.getUserIndentity("thomasp");
        assertEquals("Name must be set", "Thomas", thomaspUserIdentity.getFirstName());
        assertEquals("Lastname must be set", "Pringle", thomaspUserIdentity.getLastName());
        assertEquals("UserId must be set", "username@emailaddress.com", thomaspUserIdentity.getUid());

        UserIdentity erikdUserIdentity = ldapUserIdentityDao.getUserIndentity("erikd");
        assertEquals("Name must be set", "Erik", erikdUserIdentity.getFirstName());
        assertEquals("Lastname must be set", "Drolshammer", erikdUserIdentity.getLastName());
        assertEquals("UserId must be set", "erik.drolshammer", erikdUserIdentity.getUid());
        
        UserAggregate userAggregate1 = new UserAggregate(thomaspUserIdentity, roleRepository.getUserPropertyAndRoles(thomaspUserIdentity.getUid()));
        
        List<UserPropertyAndRole> propsAndRoles = userAggregate1.getRoles();
        assertEquals("All roles must be found", 3, propsAndRoles.size());
        assertTrue("The role must be found", containsRoleMapping(propsAndRoles, "username@emailaddress.com", "12", "UserAdminService", "Altran", "developer", "30"));
        assertTrue("The role must be found", containsRoleMapping(propsAndRoles, "username@emailaddress.com", "15", "SSOLoginWebApplication", "Whydah", "developer", "20"));

        UserAggregate userAggregate2 = new UserAggregate(erikdUserIdentity, roleRepository.getUserPropertyAndRoles(erikdUserIdentity.getUid()));
        
        List<UserPropertyAndRole> propsAndRoles2 = userAggregate2.getRoles();
        assertEquals("All roles must be found", 1, propsAndRoles2.size());
        assertTrue("The role must be found", containsRoleMapping(propsAndRoles2, "erik.drolshammer", "12", "UserAdminService", "Altran", "admin", "70"));

    }

    private boolean containsRoleMapping(List<UserPropertyAndRole> propsAndRoles, String uid, String appId, String appName, String orgName, String roleName, String roleValue) {
        for (UserPropertyAndRole role : propsAndRoles) {
            if(role.getApplicationId().equals(appId) &&
			   role.getApplicationName().equals(appName) && 
			   role.getOrganizationName().equals(orgName) && 
			   role.getApplicationRoleName().equals(roleName) &&
                    role.getApplicationRoleValue().equals(roleValue) &&
                    role.getUid().equals(uid)) {

                return true;
			}
		}
		return false;
	}
}
