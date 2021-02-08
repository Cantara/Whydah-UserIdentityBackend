package net.whydah.identity.user.authentication;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import net.whydah.identity.Main;
import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.application.ApplicationService;
import net.whydah.identity.application.search.LuceneApplicationIndexer;
import net.whydah.identity.application.search.LuceneApplicationSearch;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.dataimport.IamDataImporter;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.LDAPUserIdentity;
import net.whydah.identity.user.identity.LdapAuthenticator;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.role.UserApplicationRoleEntryDao;
import net.whydah.identity.user.search.LuceneUserIndexer;
import net.whydah.identity.util.FileUtils;
import net.whydah.identity.util.PasswordGenerator;
import net.whydah.sso.ddd.model.user.PersonRef;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.types.UserAggregate;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.*;


/**
 * @author <a href="mailto:erik.drolshammer@altran.com">Erik Drolshammer</a>
 * @since 10/18/12
 */
public class UserAuthenticationEndpointTest {
    private static UserApplicationRoleEntryDao userApplicationRoleEntryDao;
    private static UserAdminHelper userAdminHelper;
    private static UserIdentityService userIdentityService;
    private static ApplicationService applicationService;
    private static Main main = null;


    @BeforeClass
    public static void setUp() throws Exception {
    	//ApplicationMode.setTags(ApplicationMode.CI_MODE, ApplicationMode.NO_SECURITY_FILTER);
        ApplicationMode.setCIMode();
        final ConstrettoConfiguration configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("classpath:useridentitybackend-test.properties"))
                .done()
                .getConfiguration();

        String roleDBDirectory = configuration.evaluateToString("roledb.directory");
        String ldapPath = configuration.evaluateToString("ldap.embedded.directory");
        String luceneUserDir = configuration.evaluateToString("lucene.usersdirectory");
        String luceneAppDir = configuration.evaluateToString("lucene.applicationsdirectory");
        FileUtils.deleteDirectories(ldapPath, roleDBDirectory, luceneUserDir, luceneAppDir);

        main = new Main(6649);
        main.startEmbeddedDS(configuration.asMap());
      
        
        BasicDataSource dataSource = Main.initBasicDataSource(configuration);
        new DatabaseMigrationHelper(dataSource).upgradeDatabase();
       
        
        main.startJetty();

        ApplicationDao applicationDao = new ApplicationDao(dataSource);
        AuditLogDao auditLogDao = new AuditLogDao(dataSource);

        Directory appIndex = new NIOFSDirectory(Paths.get(new File(luceneAppDir).getPath()));
        LuceneApplicationIndexer luceneApplicationIndexer = new LuceneApplicationIndexer(appIndex);

        Directory userIndex = new NIOFSDirectory(Paths.get(new File(luceneUserDir).getPath()));
        LuceneUserIndexer luceneUserIndexer = new LuceneUserIndexer(userIndex);

        LuceneApplicationSearch luceneAppSearch = new LuceneApplicationSearch(appIndex);
        
        userApplicationRoleEntryDao = new UserApplicationRoleEntryDao(dataSource);
        applicationService=  new ApplicationService(applicationDao,  auditLogDao, luceneApplicationIndexer, luceneAppSearch);


        new IamDataImporter(dataSource, configuration).importIamData();


        String primaryLdapUrl = configuration.evaluateToString("ldap.primary.url");
        String primaryAdmPrincipal = configuration.evaluateToString("ldap.primary.admin.principal");
        String primaryAdmCredentials = configuration.evaluateToString("ldap.primary.admin.credentials");
        String primaryUidAttribute = configuration.evaluateToString("ldap.primary.uid.attribute");
        String primaryUsernameAttribute = configuration.evaluateToString("ldap.primary.username.attribute");
        String readonly = configuration.evaluateToString("ldap.primary.readonly");

        LdapUserIdentityDao ldapUserIdentityDao = new LdapUserIdentityDao(primaryLdapUrl, primaryAdmPrincipal, primaryAdmCredentials, primaryUidAttribute, primaryUsernameAttribute, readonly);
        LdapAuthenticator ldapAuthenticator = new LdapAuthenticator(primaryLdapUrl, primaryAdmPrincipal, primaryAdmCredentials, primaryUidAttribute, primaryUsernameAttribute);

        PasswordGenerator pwg = new PasswordGenerator();
        userIdentityService = new UserIdentityService(ldapAuthenticator, ldapUserIdentityDao, auditLogDao, pwg, luceneUserIndexer, null);

        userAdminHelper = new UserAdminHelper(ldapUserIdentityDao, luceneUserIndexer, auditLogDao, userApplicationRoleEntryDao, configuration);

        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;
        
    }

    @AfterClass
	public static void tearDown() {
    	if (main != null) {
            main.stop();
        }
    }

    @Test
    public void testAuthenticateUserOK() {
        ApplicationMode.setTags(ApplicationMode.NO_SECURITY_FILTER);
        String userName = "systest";
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                " <usercredential>\n" +
                "    <params>\n" +
                "        <username>" + userName + "</username>\n" +
                "        <password>systest42</password>\n" +
                "    </params>\n" +
                "</usercredential>";

        String path = "/{applicationtokenid}/authenticate/user";
        com.jayway.restassured.response.Response response = given()
                .body(xml)
                .contentType(ContentType.XML)
                .log().everything()
                .expect()
                .statusCode(Response.Status.OK.getStatusCode())
                .log().ifError()
                .when()
                .post(path, "someValidApplicationtokenid");

        String responseAsString = response.body().asString();
        UserAggregate user = UserAggregateMapper.fromXML(responseAsString);

        //test data is fetched from src/test/resources/testdata/users.csv
        assertEquals(user.getUsername(), userName);
        assertEquals(user.getFirstName(), "UserAdmin");
        assertEquals(user.getLastName(), "UserAdminWebApp");
        assertEquals(user.getUid(), "systest");
        assertEquals(user.getEmail(), "whydahadmin@getwhydah.com");
        assertTrue(user.getRoleList().isEmpty());
    }

    @Test
    public void testAuthenticateUserForbidden() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                " <usercredential>\n" +
                "    <params>\n" +
                "        <username>testMe</username>\n" +
                "        <password>wrongPassword</password>\n" +
                "    </params>\n" +
                "</usercredential>";

        String path = "/{applicationtokenid}/authenticate/user";
        given()
                .body(xml)
                .contentType(ContentType.XML)
                .log().everything()
                .expect()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode())
                .log().ifError()
                .when()
                .post(path, "notValidApplicationtokenid");
    }


    @Test
    public void testAuthenticateUsingFacebookCredentials() {
        LDAPUserIdentity newIdentity = new LDAPUserIdentity();
        newIdentity.setUid("demofbidentity");
        String username = "facebookUsername";
        newIdentity.setUsername(username);
        String facebookId = "1234";
        newIdentity.setPassword(facebookId + facebookId);
        newIdentity.setFirstName("firstName");
        newIdentity.setLastName("lastName");
        String email = "e@mail.com";
        newIdentity.setEmail(email);

        UserAggregateService userAggregateService = new UserAggregateService(userIdentityService, userApplicationRoleEntryDao,
                applicationService, null, null);
        UserAuthenticationEndpoint resource = new UserAuthenticationEndpoint(userAggregateService, userAdminHelper, userIdentityService);

        String roleValue = "roleValue";
        Response response = resource.createAndAuthenticateUser(newIdentity, roleValue, false);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());


        String userXml = (String) response.getEntity();
        UserAggregate userAggregate = UserAggregateMapper.fromXML(userXml);

        /*
        Viewable entity = (Viewable) response.getEntity();
        UIBUserAggregate model = (UIBUserAggregate) entity.getModel();
        LDAPUserIdentity identity = model.getIdentity();
        */
        //LDAPUserIdentity identity = userAggregate.getIdentity();
        assertEquals(username, userAggregate.getUsername());
        assertTrue(PersonRef.isValid(userAggregate.getPersonRef()));
        assertEquals(email, userAggregate.getEmail());
        assertNotNull(userAggregate.getUid());

        //TODO Reenable test for properties and roles
        /*
        String applicationId = AppConfig.appConfig.getProperty("adduser.defaultapplication.id");
        String applicationName = AppConfig.appConfig.getProperty("adduser.defaultapplication.name");
        String organizationId = AppConfig.appConfig.getProperty("adduser.defaultorganization.id");
        String organizationName = AppConfig.appConfig.getProperty("adduser.defaultorganization.name");
        String roleName = AppConfig.appConfig.getProperty("adduser.defaultrole.name");
        String facebookRoleName = AppConfig.appConfig.getProperty("adduser.defaultrole.facebook.name");
        */
        /*
        List<UserPropertyAndRole> propsAndRoles = model.getRoles();

        for (UserPropertyAndRole role : propsAndRoles) {
            assertEquals(applicationId, role.getApplicationId());
//            assertEquals(applicationName, role.getApplicationName());
//            assertEquals(organizationId, role.getOrganizationId());
//            assertEquals(organizationName, role.getOrganizationName()); //TODO figure out why orgName is not set.
        }

        assertEquals(2, propsAndRoles.size());

        UserPropertyAndRole role1 = propsAndRoles.get(0);
        assertEquals(roleName, role1.getRoleName());

        UserPropertyAndRole role2 = propsAndRoles.get(1);
        assertEquals(facebookRoleName, role2.getRoleName());
        */
    }


    @Test
    public void testGetFacebookDataAsString() {
        StringBuilder strb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n ");
        strb.append("<user>");
        strb.append("<params>");
        strb.append("<userId>").append("745925666").append("</userId>");
        strb.append("<firstName>").append("Erik").append("</firstName>");
        strb.append("<lastName>").append("Drolshammer").append("</lastName>");
        strb.append("<username>").append("erik.drolshammer").append("</username>");
        strb.append("<email>").append("erik.drolshammer@someprovider.com").append("</email>");
        strb.append("<birthday>").append("08/05/1982").append("</birthday>");
        strb.append("<hometown>").append("Moss, Norway").append("</hometown>");
        strb.append("<location>").append("Oslo, Norway").append("</location>");
        strb.append("</params>");
        strb.append("</user>");

        InputStream input = new ByteArrayInputStream(strb.toString().getBytes());
        String facebookDataAsString = UserAuthenticationEndpoint.getFacebookDataAsString(input);
        assertNotNull(facebookDataAsString);
    }

    @Test
    public void testGetFacebookDataAsStringFromDomDocument() throws Exception {
        StringBuilder strb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n ");
        strb.append("<user>");
        strb.append("<params>");
        String expectedFbUserId = "745925666";
        strb.append("<userId>").append(expectedFbUserId).append("</userId>");
        strb.append("<firstName>").append("Erik").append("</firstName>");
        strb.append("<lastName>").append("Drolshammer").append("</lastName>");
        strb.append("<username>").append("erik.drolshammer").append("</username>");
        strb.append("<email>").append("erik.drolshammer@someprovider.com").append("</email>");
        strb.append("<birthday>").append("08/05/1982").append("</birthday>");
        String expectedHomeTown = "Moss, Norway";
        strb.append("<hometown>").append(expectedHomeTown).append("</hometown>");
        strb.append("<location>").append("Oslo, Norway").append("</location>");
        strb.append("</params>");
        strb.append("</user>");

        InputStream input = new ByteArrayInputStream(strb.toString().getBytes());
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        Document fbUserDoc = builder.parse(input);

        String fbDataValueWithCdata = UserAuthenticationEndpoint.getUserDataAsXmlString(fbUserDoc);
        assertNotNull(fbDataValueWithCdata);

        //Strip cdata wrapper
        String fbDataValue = fbDataValueWithCdata.replace("<![CDATA[", "").replace("]]>", "");

        InputStream fbDataInput = new ByteArrayInputStream(fbDataValue.getBytes());
        Document fbDataDoc = builder.parse(fbDataInput);

        XPath xPath = XPathFactory.newInstance().newXPath();
        String fbUserId = (String) xPath.evaluate("//userId[1]", fbDataDoc, XPathConstants.STRING);
        assertEquals(expectedFbUserId, fbUserId);
        String hometown = (String) xPath.evaluate("//hometown[1]", fbDataDoc, XPathConstants.STRING);
        assertEquals(expectedHomeTown, hometown);
    }
}
