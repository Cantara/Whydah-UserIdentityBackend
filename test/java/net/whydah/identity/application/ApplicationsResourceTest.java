package net.whydah.identity.application;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import net.whydah.identity.Main;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.util.FileUtils;
import net.whydah.sso.application.Application;
import net.whydah.sso.application.ApplicationSerializer;
import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;

/**
 * End-to-end test against the exposed HTTP endpoint and down to the in-mem HSQLDB.
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-02-01
 */
public class ApplicationsResourceTest {
    private final String appToken1 = "appToken1";
    private final String userToken1 = "userToken1";
    private Main main;

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
    public void testGetApplicationsEmptyList() throws Exception {
        String path = "/{applicationtokenid}/{userTokenId}/applications";
        Response response = given()
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .get(path, appToken1, userToken1);

        String jsonResponse = response.body().asString();
        List<Application> applications = ApplicationSerializer.fromJsonList(jsonResponse);
        assertEquals(applications.size(), 0);
    }

    @Test(dependsOnMethods = "testGetApplicationsEmptyList")
    public void testGetApplicationsOK() throws Exception {
        //Add applications
        int nrOfApplications = 4;
        String createPath = "/{applicationtokenid}/{userTokenId}/application";
        String json;
        for (int i = 0; i < nrOfApplications; i++) {
            json = ApplicationSerializer.toJson(new Application("ignoredId", "appName" + i));
            given()
                    .body(json)
                    .contentType(ContentType.JSON)
                    .log().everything()
                    .expect()
                    .statusCode(200)
                    .log().ifError()
                    .when()
                    .post(createPath, appToken1, userToken1);
        }

        //GET
        String path = "/{applicationtokenid}/{userTokenId}/applications";
        Response response = given()
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .get(path, appToken1, userToken1);

        String jsonResponse = response.body().asString();
        List<Application> applications = ApplicationSerializer.fromJsonList(jsonResponse);
        assertEquals(applications.size(), nrOfApplications);
    }
}
