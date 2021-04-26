package net.whydah.identity.user.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import net.whydah.identity.Main;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.dataimport.IamDataImporter;
import net.whydah.identity.util.FileUtils;
import net.whydah.identity.util.PasswordGenerator;
import net.whydah.sso.user.types.UserIdentity;
import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.net.URI;
import java.sql.SQLException;

import static com.jayway.restassured.RestAssured.given;

public class PasswordResource2EndpointTest {
    private static final Logger log = LoggerFactory.getLogger(PasswordResource2EndpointTest.class);

    private static Client client = ClientBuilder.newClient();
    private static Main main;
    private String luceneUsersDir;
    private BasicDataSource dataSource;

    @Before
    public void init() {
        FileUtils.deleteDirectory(new File("target/data/lucene"));
        FileUtils.deleteDirectory(new File("target/data/lucene"));
        ApplicationMode.setCIMode();
        final ConstrettoConfiguration configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("classpath:useridentitybackend-test.properties"))
                .done()
                .getConfiguration();

        String ldapPath = configuration.evaluateToString("ldap.embedded.directory");
        luceneUsersDir = configuration.evaluateToString("lucene.usersdirectory");
        FileUtils.deleteDirectories(ldapPath, "target/bootstrapdata/", luceneUsersDir);

        main = new Main(6653);
        main.startEmbeddedDS(configuration.asMap());

        dataSource = initBasicDataSource(configuration);
        DatabaseMigrationHelper dbHelper = new DatabaseMigrationHelper(dataSource);
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();

        new IamDataImporter(dataSource, configuration).importIamData();

        //String requiredRoleName = AppConfig.appConfig.getProperty("useradmin.requiredrolename");
        //main.startHttpServer(requiredRoleName);   //TODO
        main.startJetty();

        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;


        URI baseUri = UriBuilder.fromUri("http://localhost/uib/uib/useradmin/").port(main.getPort()).build();
        URI logonUri = UriBuilder.fromUri("http://localhost/uib/").port(main.getPort()).build();

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

    @After
    public void teardown() {
        main.stop();

        try {
            if (!dataSource.isClosed()) {
                dataSource.close();
            }
        } catch (SQLException e) {
            log.error("", e);
        }

        FileUtils.deleteDirectory(new File("target/data/lucene"));
        FileUtils.deleteDirectory(new File("data/lucene"));
        FileUtils.deleteDirectory(new File(luceneUsersDir));
    }

    private void addTestUser() {
        String appTokenId = "applicationTestToken";
        String userTokenId = "userTestToken";
        String path = "/{applicationtokenid}/{userTokenId}/user";
        UserIdentity userIdentity = new UserIdentity("test.me@example.com", "test", "me", null, "test.me@example.com", "+4712312345");

        try {
            String json = new ObjectMapper().writeValueAsString(userIdentity);
            com.jayway.restassured.response.Response response = given()
                    .request().body(json)
                    .request().contentType(MediaType.APPLICATION_JSON)
                    .log().everything()
                    .expect()
                    .statusCode(Response.Status.CREATED.getStatusCode())
                    .log().ifError()
                    .when()
                    .post(path, appTokenId, userTokenId);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void test_resetPassword_ok() throws Exception {
        ApplicationMode.setTags(ApplicationMode.NO_SECURITY_FILTER);
        addTestUser();

        String appTokenId = "test";
        String uid = "test.me@example.com";
        String path = "/{applicationtokenid}/user/{uid}/reset_password";


        com.jayway.restassured.response.Response post =
                given()
                        .log().everything()
                        .expect()
                        .statusCode(Response.Status.OK.getStatusCode())
                        .log().ifError()
                        .when()
                        .post(path, appTokenId, uid);
    }

    @Test
    public void test_authenticateAndChangePasswordUsingToken_ok() throws Exception {
        ApplicationMode.setTags(ApplicationMode.NO_SECURITY_FILTER);
        addTestUser();
        String appTokenId = "test";
        String uid = "test.me@example.com";

        String changePasswordToken = resetPassword(appTokenId, uid);

        String path = "/{applicationtokenid}/user/{uid}/change_password";

        JSONObject requestParams = new JSONObject();
        String generatedPwd = new PasswordGenerator().generate();
        requestParams.put("newpassword", generatedPwd);

        com.jayway.restassured.response.Response post = given()
                .request().queryParam("changePasswordToken", changePasswordToken).body(requestParams.toString())
                .log().everything()
                .expect()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode())
                .log().ifError()
                .when()
                .post(path, appTokenId, uid);
    }

    private String resetPassword(String appTokenId, String uid) {
        String path = "/{applicationtokenid}/user/{uid}/reset_password";
        com.jayway.restassured.response.Response post = given()
                .log().everything()
                .expect()
                .statusCode(Response.Status.OK.getStatusCode())
                .log().ifError()
                .when()
                .post(path, appTokenId, uid);
        String changePasswordToken = post.body().jsonPath().getString("changePasswordToken");
        return changePasswordToken;
    }

    @Test
    public void test_hasUserNameSetPassword_ok() throws Exception {
        ApplicationMode.setTags(ApplicationMode.NO_SECURITY_FILTER);
        addTestUser();
        String appTokenId = "test";
        String uid = "test.me@example.com";
        String path = "/{applicationtokenid}/user/{uid}/password_login_enabled";
        com.jayway.restassured.response.Response response = given()
                .log().everything()
                .expect().statusCode(Response.Status.OK.getStatusCode())
                .log().ifError()
                .when()
                .get(path, appTokenId, uid);
    }
}