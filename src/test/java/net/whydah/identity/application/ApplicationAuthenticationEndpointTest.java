package net.whydah.identity.application;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import net.whydah.identity.Main;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.util.FileUtils;
import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import static com.jayway.restassured.RestAssured.given;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-11-21.
 */
public class ApplicationAuthenticationEndpointTest {
    private static final Logger log = LoggerFactory.getLogger(ApplicationAuthenticationEndpointTest.class);

    //testapplications.csv
    private static final String UAS_APPLICATION_ID = "1234";
    private static final String UAS_APPLICATION_SECRET = "9ju592A4t8dzz8mz7a5QQJ7Px";
    private static final String TESTAPP_APPLICATION_ID = "9977";
    private static final String TESTAPP_APPLICATION_SECRET = "33879936R6Jr47D4Hj5R6p9qT";
    private static String AUTH_PATH = "/{stsapplicationtokenId}/application/auth";
    private static final String STS_APPTOKEN_ID_NOT_IN_USE = "stsApptokenIdNotInUse";

    private static final String appToken1 = "appToken1";
    private static final String userToken1 = "userToken1";
    private static Main main;
    private static Application uas;
    private static Application testapp;
    private static BasicDataSource dataSource;

    @BeforeClass
    public static void startServer() {
        ApplicationMode.setTags(ApplicationMode.CI_MODE, ApplicationMode.NO_SECURITY_FILTER);
        final ConstrettoConfiguration configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("file:./useridentitybackend_override.properties"))
                .done()
                .getConfiguration();

        String roleDBDirectory = configuration.evaluateToString("roledb.directory");
        FileUtils.deleteDirectory(roleDBDirectory);
        dataSource = initBasicDataSource(configuration);
        DatabaseMigrationHelper dbHelper = new DatabaseMigrationHelper(dataSource);
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();

        main = new Main(6645);
        main.startJetty();
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;

        createApplications();
    }

    private static void createApplications() {
        uas = createApplication(UAS_APPLICATION_ID, "UAS", UAS_APPLICATION_SECRET);
        testapp = createApplication(TESTAPP_APPLICATION_ID, "TESTAPP", TESTAPP_APPLICATION_SECRET);
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
        try {
        	if(!dataSource.isClosed()) {
        		dataSource.close();
        	}
		} catch (SQLException e) {
			log.error("", e);
		}
    }


    static Application createApplication(String applicationId, String appName, String applicationSecret) {
        Application uas = new Application(applicationId, appName);
        uas.getSecurity().setSecret(applicationSecret);

        String json = ApplicationMapper.toJson(uas);

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
        return ApplicationMapper.fromJson(jsonResponse);
    }

    @Test
    public void testWrongUasSecretReturnsForbidden() throws Exception {
        ApplicationCredential uasAppCredential = new ApplicationCredential(uas.getId(), "","wrongSecret");
        String uasAppCredentialXml = ApplicationCredentialMapper.toXML(uasAppCredential);

        given()
                .formParam(ApplicationAuthenticationEndpoint.UAS_APP_CREDENTIAL_XML, uasAppCredentialXml)
                .contentType(ContentType.URLENC)
                .log().everything()
                .expect()
                .statusCode(403)
                .log().ifError()
                .when()
                .post(AUTH_PATH, STS_APPTOKEN_ID_NOT_IN_USE);
    }

    @Test
    public void testWrongUnknownAppReturnsForbidden() throws Exception {
        ApplicationCredential appCredential = new ApplicationCredential(testapp.getId(), "","wrongSecret");
        String testAppAppCredentialXml = ApplicationCredentialMapper.toXML(appCredential);

        given()
            .formParam(ApplicationAuthenticationEndpoint.APP_CREDENTIAL_XML, testAppAppCredentialXml)
            .contentType(ContentType.URLENC)
            .log().everything()
            .expect()
            .statusCode(403)
            .log().ifError()
            .when()
            .post(AUTH_PATH, STS_APPTOKEN_ID_NOT_IN_USE);
    }

    @Test
    public void testApplicationAuthOK() throws Exception {

        ApplicationCredential uasAppCredential = new ApplicationCredential(uas.getId(), "",UAS_APPLICATION_SECRET);
         String uasAppCredentialXml = ApplicationCredentialMapper.toXML(uasAppCredential);
         ApplicationCredential appCredential = new ApplicationCredential(testapp.getId(), "",TESTAPP_APPLICATION_SECRET);
         String testAppAppCredentialXml = ApplicationCredentialMapper.toXML(appCredential);
         given()
                 .formParam(ApplicationAuthenticationEndpoint.UAS_APP_CREDENTIAL_XML, uasAppCredentialXml)
                 .contentType(ContentType.URLENC)
                 .log().everything()
                 .expect()
                 .statusCode(403)
                 .log().ifError()
                 .when()
                 .post(AUTH_PATH, STS_APPTOKEN_ID_NOT_IN_USE);

    }
}
