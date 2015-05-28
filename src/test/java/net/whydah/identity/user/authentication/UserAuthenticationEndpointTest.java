package net.whydah.identity.user.authentication;

import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.user.UserAggregate;
import net.whydah.identity.user.email.PasswordSender;
import net.whydah.identity.user.identity.*;
import net.whydah.identity.user.resource.UserAdminHelper;
import net.whydah.identity.user.role.UserPropertyAndRoleDao;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import net.whydah.identity.user.search.LuceneIndexer;
import net.whydah.identity.util.FileUtils;
import net.whydah.identity.util.PasswordGenerator;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.naming.NamingException;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * @author <a href="mailto:erik.drolshammer@altran.com">Erik Drolshammer</a>
 * @since 10/18/12
 */
public class UserAuthenticationEndpointTest {
    private final static String basepath = "target/UserAuthenticationEndpointTest/";
    private final static String ldappath = basepath + "hsqldb/ldap/";
    private final static String dbpath = basepath + "hsqldb/roles";

    private static EmbeddedADS ads;
    private static UserPropertyAndRoleRepository roleRepository;
    private static UserAdminHelper userAdminHelper;
    private static UserIdentityService userIdentityService;

    @BeforeClass
    public static void setUp() throws Exception {
    	System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);
    	
        int LDAP_PORT = new Integer(AppConfig.appConfig.getProperty("ldap.embedded.port"));
        String LDAP_URL = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";
        
    	FileUtils.deleteDirectory(new File(basepath));

        File ldapdir = new File(ldappath);
        ldapdir.mkdirs();
        ads = new EmbeddedADS(ldappath);
        try {
            ads.startServer(LDAP_PORT);
        } catch (Exception e){

        }

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:file:" + dbpath);

        AuditLogDao auditLogDao = new AuditLogDao(dataSource);

        String readOnly = AppConfig.appConfig.getProperty("ldap.primary.readonly");
        LdapUserIdentityDao ldapUserIdentityDao = new LdapUserIdentityDao(LDAP_URL, "uid=admin,ou=system", "secret", "uid", "initials", readOnly);
        LdapAuthenticator ldapAuthenticator = new LdapAuthenticator(LDAP_URL, "uid=admin,ou=system", "secret", "uid", "initials");

        PasswordGenerator pwg = new PasswordGenerator();
        PasswordSender passwordSender = new PasswordSender(null, null, null);
        userIdentityService = new UserIdentityService(ldapAuthenticator, ldapUserIdentityDao, auditLogDao, pwg, passwordSender, null, null);

        new DatabaseMigrationHelper(dataSource).upgradeDatabase();

        ApplicationDao configDataRepository = new ApplicationDao(dataSource);
        roleRepository = new UserPropertyAndRoleRepository(new UserPropertyAndRoleDao(dataSource), configDataRepository);


        Directory index = new NIOFSDirectory(new File(basepath + "lucene"));
        userAdminHelper = new UserAdminHelper(ldapUserIdentityDao, new LuceneIndexer(index), auditLogDao, roleRepository);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (ads != null) {
            ads.stopServer();
        }
    }


    @Test
    public void testAuthenticateUsingFacebookCredentials() throws NamingException {
        UserIdentity newIdentity = new UserIdentity();
        String username = "facebookUsername";
        newIdentity.setUsername(username);
        String facebookId = "1234";
        newIdentity.setPassword(facebookId + facebookId);
        newIdentity.setFirstName("firstName");
        newIdentity.setLastName("lastName");
        String email = "e@mail.com";
        newIdentity.setEmail(email);

        UserAuthenticationEndpoint resource = new UserAuthenticationEndpoint(roleRepository, userAdminHelper, userIdentityService);

        String roleValue = "roleValue";
        Response response = resource.createAndAuthenticateUser(newIdentity, roleValue, false);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());


        String userXml = (String) response.getEntity();
        UserAggregate userAggregate = UserAggregate.fromXML(userXml);

        /*
        Viewable entity = (Viewable) response.getEntity();
        UserAggregate model = (UserAggregate) entity.getModel();
        UserIdentity identity = model.getIdentity();
        */
        //UserIdentity identity = userAggregate.getIdentity();
        assertEquals(username, userAggregate.getUsername());
        assertEquals(userAggregate.getPersonRef(), "");
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

        String fbDataValueWithCdata = UserAuthenticationEndpoint.getFacebookDataAsXmlString(fbUserDoc);
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
