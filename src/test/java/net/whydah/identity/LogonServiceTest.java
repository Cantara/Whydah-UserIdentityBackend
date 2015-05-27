package net.whydah.identity;

/*
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.header.MediaTypes;
import com.sun.jersey.core.util.MultivaluedMapImpl;
*/

import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.dataimport.IamDataImporter;
import net.whydah.identity.user.identity.EmbeddedADS;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.user.role.UserPropertyAndRoleDao;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import net.whydah.identity.util.FileUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.glassfish.jersey.client.ClientResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Ignore
public class LogonServiceTest {
    private static URI baseUri;
    private Client client = ClientBuilder.newClient();

    private final static String basepath = "target/LogonServiceTest/";
    private final static String ldappath = basepath + "hsqldb/ldap/";
    private final static String dbpath = basepath + "hsqldb/roles";
    //    private final static int LDAP_PORT = 10937;
    private static String LDAP_URL; // = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";

    private static EmbeddedADS ads;
    private static LdapUserIdentityDao ldapUserIdentityDao;
    //private static LdapAuthenticator ldapAuthenticator;
    private static UserPropertyAndRoleRepository roleRepository;
    //private static UserAdminHelper userAdminHelper;
    private static Main main = null;


    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);

        int HTTP_PORT = new Integer(AppConfig.appConfig.getProperty("service.port"));
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
        ldapUserIdentityDao = new LdapUserIdentityDao(LDAP_URL, "uid=admin,ou=system", "secret", "uid", "initials",Boolean.parseBoolean(AppConfig.appConfig.getProperty("ldap.primary.readonly")));
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
            main = new Main(Integer.valueOf(AppConfig.appConfig.getProperty("service.port")));
            //main.importUsersAndRoles();
            new IamDataImporter(dataSource).importIamData();
        } catch (Exception e){

        }

        //String sslVerification = AppConfig.appConfig.getProperty("sslverification");
        String requiredRoleName = AppConfig.appConfig.getProperty("useradmin.requiredrolename");
        //main.startHttpServer(requiredRoleName);
        main.start();

        baseUri = UriBuilder.fromUri("http://localhost/uib/").port(HTTP_PORT).build();
    }


    @AfterClass
    public static void teardown()  {
        if (ads != null) {
            ads.stopServer();
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
//        System.out.println("WADL:"+serviceWadl);
        assertTrue(serviceWadl.length() > 60);
    }

    @Test
    public void formLogonOK() throws IOException {
        WebTarget webResource = client.target(baseUri);
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("username", "thomasp");
        formData.add("password", "logMeInPlease");
        //ClientResponse response = webResource.path("logon").type("application/x-www-form-urlencoded").post(ClientResponse.class, formData);
        ClientResponse response = webResource.path("logon").request().post(Entity.form(formData), ClientResponse.class);
        String responseBody = response.readEntity(String.class);
        System.out.println(responseBody);
        //assertTrue(responseBody.contains("Logon ok"));
        assertTrue(responseBody.contains("username@emailaddress.com"));
    }

    @Test
    public void formLogonFail() throws IOException {
        WebTarget webResource = client.target(baseUri);
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("username", "thomasp");
        formData.add("password", "vrangt");
        //ClientResponse response = webResource.path("logon").type("application/x-www-form-urlencoded").post(ClientResponse.class, formData);
        ClientResponse response = webResource.path("logon").request().post(Entity.form(formData), ClientResponse.class);

        String responseBody = response.readEntity(String.class);
        System.out.println(responseBody);

        assertTrue(responseBody.contains("failed"));
        assertFalse(responseBody.contains("freecodeUser"));
    }

    @Test
    public void XMLLogonOK() throws IOException {
        WebTarget webResource = client.target(baseUri);
        String payload = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><authgreier><auth><dilldall>dilldall</dilldall><user><username>thomasp</username><coffee>yes please</coffee><password>logMeInPlease</password></user></auth></authgreier>";
        //ClientResponse response = webResource.path("logon").type("application/xml").post(ClientResponse.class, payload);
        ClientResponse response = webResource.path("logon").request().post(Entity.xml(payload), ClientResponse.class);
        String responseXML = response.readEntity(String.class);
        System.out.println(responseXML);
        assertTrue(responseXML.contains("thomasp"));

    }

    @Test
    public void XMLLogonFail() throws IOException {
        WebTarget webResource = client.target(baseUri);
        String payload = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><authgreier><auth><dilldall>dilldall</dilldall><user><username>thomasp</username><coffee>yes please</coffee><password>vrangt</password></user></auth></authgreier>";
        //ClientResponse response = webResource.path("logon").type("application/xml").post(ClientResponse.class, payload);
        ClientResponse response = webResource.path("logon").request().post(Entity.xml(payload), ClientResponse.class);
        String responseXML = response.readEntity(String.class);
        //System.out.println(responseXML);
        assertTrue(responseXML.contains("logonFailed"));
        assertFalse(responseXML.contains("thomasp"));
    }

}
