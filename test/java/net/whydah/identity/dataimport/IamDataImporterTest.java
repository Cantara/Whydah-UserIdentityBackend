package net.whydah.identity.dataimport;

import com.jayway.restassured.RestAssured;
import net.whydah.identity.Main;
import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.application.ApplicationService;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.user.UserAggregate;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.util.FileUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class IamDataImporterTest {
    private BasicDataSource dataSource;
    private IamDataImporter dataImporter;
    private Main main;

    @BeforeClass
    public void startServer() {
        ApplicationMode.setCIMode();
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


        String ldapEmbeddedpath = configuration.evaluateToString("ldap.embedded.directory");
        Integer ldapPort = configuration.evaluateToInt("ldap.embedded.port");

        main = new Main(6655);
        main.startEmbeddedDS(ldapEmbeddedpath, ldapPort);

        dataImporter = new IamDataImporter(dataSource, configuration);

        main.start();
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;
    }

    private BasicDataSource initBasicDataSource(ConstrettoConfiguration configuration) {
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
    public void testDataIsImported() throws Exception {
        dataImporter.importIamData();
        LdapUserIdentityDao ldapUserIdentityDao = dataImporter.getLdapUserIdentityDao();
        UserIdentity thomaspUserIdentity = ldapUserIdentityDao.getUserIndentity("thomasp");
        assertEquals("Thomas", thomaspUserIdentity.getFirstName());
        assertEquals("Pringle", thomaspUserIdentity.getLastName());
        assertEquals("username@emailaddress.com", thomaspUserIdentity.getUid());

        UserIdentity erikdUserIdentity = ldapUserIdentityDao.getUserIndentity("erikd");
        assertEquals("Erik", erikdUserIdentity.getFirstName());
        assertEquals("Drolshammer", erikdUserIdentity.getLastName());
        assertEquals("erik.drolshammer", erikdUserIdentity.getUid());

        ApplicationService applicationService = new ApplicationService(new ApplicationDao(dataSource), null);
        UserAggregateService userAggregateService = new UserAggregateService(null, dataImporter.getUserPropertyAndRoleDao(),
                applicationService, null, null);
        UserAggregate userAggregate1 = new UserAggregate(thomaspUserIdentity, userAggregateService.getUserPropertyAndRoles(thomaspUserIdentity.getUid()));
        List<UserPropertyAndRole> propsAndRoles = userAggregate1.getRoles();

        //List<UserPropertyAndRole> propsAndRoles = userPropertyAndRoleDao.getUserPropertyAndRoles(thomaspUserIdentity.getUid());

        assertEquals(3, propsAndRoles.size());
        assertTrue(containsRoleMapping(propsAndRoles, "username@emailaddress.com", "12", "UserAdminService", "Altran", "developer", "30"));
        assertTrue(containsRoleMapping(propsAndRoles, "username@emailaddress.com", "15", "SSOLoginWebApplication", "Whydah", "developer", "20"));

        UserAggregate userAggregate2 = new UserAggregate(erikdUserIdentity, userAggregateService.getUserPropertyAndRoles(erikdUserIdentity.getUid()));
        
        List<UserPropertyAndRole> propsAndRoles2 = userAggregate2.getRoles();
        assertEquals(1, propsAndRoles2.size());
        assertTrue(containsRoleMapping(propsAndRoles2, "erik.drolshammer", "12", "UserAdminService", "Altran", "admin", "70"));
    }

    private boolean containsRoleMapping(List<UserPropertyAndRole> propsAndRoles, String uid, String appId, String appName, String orgName, String roleName, String roleValue) {
        for (UserPropertyAndRole role : propsAndRoles) {
            if (role.getApplicationId().equals(appId) &&
			   role.getApplicationName().equals(appName) && 
			   role.getOrganizationName().equals(orgName) && 
			   role.getApplicationRoleName().equals(roleName) &&
                    role.getApplicationRoleValue().equals(roleValue) &&
                    role.getUid().equals(uid)) {
                return true;
			}
		}
		return false;
	}
}
