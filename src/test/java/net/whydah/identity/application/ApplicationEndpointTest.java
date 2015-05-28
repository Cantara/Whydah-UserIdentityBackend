package net.whydah.identity.application;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import net.whydah.identity.Main;
import net.whydah.identity.config.ConfigTags;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.util.FileUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.codehaus.jackson.map.ObjectMapper;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.junit.Before;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.*;

/**
 * End-to-end test against the exposed HTTP endpoint and down to the in-mem HSQLDB.
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-02-01
 */
public class ApplicationEndpointTest {
    private Main main;
    private ObjectMapper mapper;


    @Before
    public void startServer() {
        //System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);
        System.setProperty(ConfigTags.CONSTRETTO_TAGS, ConfigTags.DEV_MODE);
        final ConstrettoConfiguration configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("file:./useridentitybackend_override.properties"))
                .done()
                .getConfiguration();

        //String roleDBDirectory = AppConfig.appConfig.getProperty("roledb.directory");
        String roleDBDirectory = configuration.evaluateToString("roledb.directory");
        FileUtils.deleteDirectory(roleDBDirectory);

        BasicDataSource dataSource = initBasicDataSource(configuration);
        new DatabaseMigrationHelper(dataSource).upgradeDatabase();

        main = new Main(6644);
        main.start();
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;
        mapper = new ObjectMapper();
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
    public void testCreateApplication() throws Exception {
        //String url = "http://localhost:" + main.getPort() + "/applicationtokenid1/userTokenId1/application";
        //Application application = new Application("id1", "appName1");
        //String json = mapper.writeValueAsString(application);
        String json = "{\"id\":\"id1\",\"name\":\"appName1\",\"description\":null,\"availableRoles\":[],\"defaultRoleName\":null,\"availableOrgNames\":[],\"defaultOrgName\":null,\"availableRoleNames\":null}";

        String path = "/{applicationtokenid}/{userTokenId}/application";
        Response response = given()
                .body(json)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .post(path, "appToken1", "userToken1");

        String jsonResponse = response.body().asString();
        Application applicationResponse = mapper.readValue(jsonResponse, Application.class);
        assertNotNull(applicationResponse.getId());
        assertFalse("expect incoming id to be ignored by UIB", "id1".equals(applicationResponse.getId()));
        assertEquals(applicationResponse.getName(), "appName1");
        assertNull(applicationResponse.getSecret());    //secret should never be returned
    }
}
