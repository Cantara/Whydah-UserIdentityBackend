package net.whydah.identity.application;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import net.whydah.identity.Main;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.util.FileUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.codehaus.jackson.map.ObjectMapper;
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
        System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);
        String roleDBDirectory = AppConfig.appConfig.getProperty("roledb.directory");
        FileUtils.deleteDirectory(roleDBDirectory);
        main = new Main(6644);
        //main.upgradeDatabase();
        BasicDataSource dataSource = initBasicDataSource();
        new DatabaseMigrationHelper(dataSource).upgradeDatabase();
        main.startHttpServer(null);
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.contextpath;
        mapper = new ObjectMapper();
        //mapper.findAndRegisterModules();
    }

    private static BasicDataSource initBasicDataSource() {
        String jdbcdriver = AppConfig.appConfig.getProperty("roledb.jdbc.driver");
        String jdbcurl = AppConfig.appConfig.getProperty("roledb.jdbc.url");
        String roledbuser = AppConfig.appConfig.getProperty("roledb.jdbc.user");
        String roledbpasswd = AppConfig.appConfig.getProperty("roledb.jdbc.password");

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jdbcdriver);
        dataSource.setUrl(jdbcurl);//"jdbc:hsqldb:file:" + basepath + "hsqldb");
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
