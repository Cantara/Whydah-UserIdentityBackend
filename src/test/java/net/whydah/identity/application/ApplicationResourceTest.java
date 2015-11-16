package net.whydah.identity.application;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import net.whydah.identity.Main;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.util.FileUtils;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationAvailableOrganizationNames;
import net.whydah.sso.application.types.ApplicationAvailableRoleNames;
import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

/**
 * End-to-end test against the exposed HTTP endpoint and down to the in-mem HSQLDB.
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-02-01
 */
public class ApplicationResourceTest {
    private final String appToken1 = "appToken1";
    private final String userToken1 = "userToken1";
    private Main main;
    private String appId1FromCreatedResponse;
    private Application app;

    @BeforeClass
    public void startServer() {
        ApplicationMode.setTags(ApplicationMode.CI_MODE, ApplicationMode.NO_SECURITY_FILTER);
        final ConstrettoConfiguration configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("file:./useridentitybackend_override.properties"))
                .done()
                .getConfiguration();

        String roleDBDirectory = configuration.evaluateToString("roledb.directory");
        FileUtils.deleteDirectory(roleDBDirectory);
        BasicDataSource dataSource = initBasicDataSource(configuration);
        DatabaseMigrationHelper dbHelper = new DatabaseMigrationHelper(dataSource);
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();

        main = new Main(6644);
        main.start();
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;
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
    public void stop() {
        if (main != null) {
            main.stop();
        }
    }

    @Test
    public void testCreateApplication() throws Exception {
        app = new Application("ignoredId", "appName1");
        app.getSecurity().setSecret("secret1");
        app.setDefaultRoleName("originalDefaultRoleName");
        app.setApplicationUrl("https://myapp.net/");
        app.addRole(new ApplicationAvailableRoleNames("roleId1", "roleName1"));
        app.addRole(new ApplicationAvailableRoleNames("roleId2", "roleName2"));
        app.addOrganizationName(new ApplicationAvailableOrganizationNames("ordid1", "orgname1"));
        app.addOrganizationName(new ApplicationAvailableOrganizationNames("ordid2","orgname2"));
        app.addOrganizationName(new ApplicationAvailableOrganizationNames("ordid3","orgname3"));

        String json = ApplicationMapper.toJson(app);

        String path = "/{applicationtokenid}/{userTokenId}/application";
        Response response = given()
                .body(json)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .post(path, appToken1, userToken1);

        String jsonResponse = response.body().asString();
        Application applicationResponse = ApplicationMapper.fromJson(jsonResponse);

        assertNotNull(applicationResponse.getId());
        assertNotEquals(app.getId(), applicationResponse.getId());
        appId1FromCreatedResponse = applicationResponse.getId();
        assertEquals(applicationResponse.getName(), app.getName());
        assertEquals(applicationResponse.getSecurity().getSecret(), app.getSecurity().getSecret());
        assertEquals(applicationResponse.getDefaultRoleName(), app.getDefaultRoleName());
    }

    @Test(dependsOnMethods = "testCreateApplication")
    public void testGetApplicationOK() throws Exception {
        String path = "/{applicationtokenid}/{userTokenId}/application/{applicationId}";
        Response response = given()
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .get(path, appToken1, userToken1, appId1FromCreatedResponse);

        String jsonResponse = response.body().asString();
        Application applicationResponse = ApplicationMapper.fromJson(jsonResponse);
        assertNotNull(applicationResponse.getId());
        assertEquals(appId1FromCreatedResponse, applicationResponse.getId());
        assertEquals(applicationResponse.getName(), app.getName());
        assertEquals(applicationResponse.getDefaultRoleName(), "originalDefaultRoleName");
    }

    @Test(dependsOnMethods = "testGetApplicationOK")
    public void testUpdateApplicationNotFound() throws Exception {
        String json = ApplicationMapper.toJson(app);

        String path = "/{applicationtokenid}/{userTokenId}/application/{applicationId}";
        given()
                .body(json)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(404)
                .log().ifError()
                .when()
                .put(path, appToken1, userToken1, appId1FromCreatedResponse);
    }

    @Test(dependsOnMethods = "testUpdateApplicationNotFound")
    public void testUpdateApplicationNoContent() throws Exception {
        app.setId(appId1FromCreatedResponse);
        app.setDefaultRoleName("anotherRoleName");
        String json = ApplicationMapper.toJson(app);

        String path = "/{applicationtokenid}/{userTokenId}/application/{applicationId}";
        Response response = given()
                .body(json)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(204)
                .log().ifError()
                .when()
                .put(path, appToken1, userToken1, appId1FromCreatedResponse);
    }


    @Test(dependsOnMethods = "testUpdateApplicationNoContent")
    public void testDeleteApplication() throws Exception {
        String path = "/{applicationtokenid}/{userTokenId}/application/{applicationId}";
        given()
                .log().everything()
                .expect()
                .statusCode(204)
                .log().ifError()
                .when()
                .delete(path, appToken1, userToken1, appId1FromCreatedResponse);
    }
    @Test(dependsOnMethods = "testDeleteApplication")
    public void testDeleteApplicationNotFound() throws Exception {
        String path = "/{applicationtokenid}/{userTokenId}/application/{applicationId}";
        given()
                .log().everything()
                .expect()
                .statusCode(404)
                .log().ifError()
                .when()
                .delete(path, appToken1, userToken1, appId1FromCreatedResponse);
    }

    @Test(dependsOnMethods = "testDeleteApplication")
    public void testGetApplicationNotFound() throws Exception {
        String path = "/{applicationtokenid}/{userTokenId}/application/{applicationId}";
        given()
                .log().everything()
                .expect()
                .statusCode(404)
                .log().ifError()
                .when()
                .get(path, appToken1, userToken1, appId1FromCreatedResponse);
    }
}
