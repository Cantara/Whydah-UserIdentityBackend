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


    private static Main main = null;

    @BeforeClass
    public static void startServer() {
        //System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);
        //System.setProperty(ApplicationMode.CONSTRETTO_TAGS, ConfigTags.DEV_MODE);
        ApplicationMode.setCIMode();
        final ConstrettoConfiguration configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("classpath:useridentitybackend-test.properties"))
                .done()
                .getConfiguration();


        String roleDBDirectory = configuration.evaluateToString("roledb.directory");
        String ldapPath = configuration.evaluateToString("ldap.embedded.directory");
        String luceneDir = configuration.evaluateToString("lucene.usersdirectory");
        FileUtils.deleteDirectories(ldapPath, roleDBDirectory, luceneDir);

        main = new Main(6644);

        BasicDataSource dataSource = initBasicDataSource(configuration);
        DatabaseMigrationHelper dbHelper = new DatabaseMigrationHelper(dataSource);
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();

        main.startJetty();
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
