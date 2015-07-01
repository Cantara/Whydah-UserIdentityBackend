package net.whydah.identity;

import com.jayway.restassured.RestAssured;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.util.FileUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Ignore
public class WadlAndWelcomePageTest {
    private static URI baseUri;
    private Client client = ClientBuilder.newClient();

    //private final static String basepath = "target/LogonServiceTest/";
    //private final static String ldappath = basepath + "hsqldb/ldap/";
    //private final static String dbpath = basepath + "hsqldb/roles";
    //    private final static int LDAP_PORT = 10937;
    //private static String LDAP_URL; // = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";

    //private static EmbeddedADS ads;
    //private static LdapUserIdentityDao ldapUserIdentityDao;
    //private static LdapAuthenticator ldapAuthenticator;
    //private static UserPropertyAndRoleRepository roleRepository;
    //private static UserAdminHelper userAdminHelper;

    private static Main main = null;

    @BeforeClass
    public static void startServer() {
        //System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);
        //System.setProperty(ApplicationMode.CONSTRETTO_TAGS, ConfigTags.DEV_MODE);
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

        main.start();
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;

        baseUri = UriBuilder.fromUri("http://localhost/uib/").port(main.getPort()).build();
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
    @BeforeClass
    public static void setUp() throws Exception {
        //System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);

        System.setProperty(ConfigTags.CONSTRETTO_TAGS, ConfigTags.DEV_MODE);
        final ConstrettoConfiguration configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("file:./useridentitybackend_override.properties"))
                .done()
                .getConfiguration();



        int HTTP_PORT = configuration.evaluateToInt("service.port");
        int LDAP_PORT = new Integer(AppConfig.appConfig.getProperty("ldap.embedded.port"));
        LDAP_URL = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";

        FileUtils.deleteDirectory(new File(basepath));

        File ldapdir = new File(ldappath);
        ldapdir.mkdirs();
        ads = new EmbeddedADS(ldappath);
        try {
            ads.startServer(LDAP_PORT);
        } catch (Exception e){

        }
        String readOnly = AppConfig.appConfig.getProperty("ldap.primary.readonly");
        ldapUserIdentityDao = new LdapUserIdentityDao(LDAP_URL, "uid=admin,ou=system", "secret", "uid", "initials", readOnly);
        //ldapAuthenticator = new LdapAuthenticator(LDAP_URL, "uid=admin,ou=system", "secret", "uid", "initials");



        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:file:" + dbpath);

        new DatabaseMigrationHelper(dataSource).upgradeDatabase();

        ApplicationDao configDataRepository = new ApplicationDao(dataSource);
        roleRepository = new UserPropertyAndRoleRepository(new UserPropertyAndRoleDao(dataSource), configDataRepository);

        AuditLogDao auditLogDao = new AuditLogDao(dataSource);
        Directory index = new NIOFSDirectory(new File(basepath + "lucene"));
        //userAdminHelper = new UserAdminHelper(ldapUserIdentityDao, new LuceneIndexer(index), auditLogRepository, roleRepository);
        try {
            main = new Main(HTTP_PORT);
            //main.importUsersAndRoles();
            new IamDataImporter(dataSource, ldapUserIdentityDao, basepath + "lucene").importIamData();
        } catch (Exception e){

        }

        //String sslVerification = AppConfig.appConfig.getProperty("sslverification");
        String requiredRoleName = AppConfig.appConfig.getProperty("useradmin.requiredrolename");
        //main.startHttpServer(requiredRoleName);
        main.start();

        baseUri = UriBuilder.fromUri("http://localhost/uib/").port(HTTP_PORT).build();
    }
    */


    @AfterClass
    public static void stop() {
        if (main != null) {
            main.stop();
        }
    }

    @Test
    public void welcome() {
        WebTarget webResource = client.target(baseUri);
        String s = webResource.request().get(String.class);
        assertTrue(s.contains("Whydah"));
        assertTrue(s.contains("<FORM"));
        assertFalse(s.contains("backtrace"));
    }

    /**
     * Test if a WADL document is available at the relative path
     * "application.wadl".
     */
    @Test
    public void testApplicationWadl() {
        WebTarget webResource = client.target(baseUri);
        //String serviceWadl = webResource.path("application.wadl").accept(MediaTypes.WADL).get(String.class);
        String serviceWadl = webResource.path("application.wadl").request().get(String.class);  //TODO MediaTypes.WADL
        assertTrue(serviceWadl.length() > 60);
    }
}
