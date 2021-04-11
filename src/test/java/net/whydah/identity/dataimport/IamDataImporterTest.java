package net.whydah.identity.dataimport;

import net.whydah.identity.Main;
import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.application.ApplicationService;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.ldapserver.EmbeddedADS;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.LDAPUserIdentity;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.util.FileUtils;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.mappers.UserIdentityMapper;
import net.whydah.sso.user.types.UserAggregate;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class IamDataImporterTest {
    private static final Logger log = LoggerFactory.getLogger(IamDataImporterTest.class);
    private static final String ldapPath = "target/IamDataImporterTest/ldap";

    private BasicDataSource dataSource;
    private IamDataImporter dataImporter;
    private Main main;

    @BeforeClass
    public void startServer() {
        ApplicationMode.setCIMode();
        final ConstrettoConfiguration config = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("classpath:useridentitybackend-test.properties"))
                .done()
                .getConfiguration();


        String roleDBDirectory = config.evaluateToString("roledb.directory");
        FileUtils.deleteDirectory(roleDBDirectory);

        dataSource = Main.initBasicDataSource(config);
        DatabaseMigrationHelper dbHelper = new DatabaseMigrationHelper(dataSource);
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();

        main = new Main(6649);

        Map<String, String> ldapProperties = Main.ldapProperties(config);
        ldapProperties.put("ldap.embedded.directory", ldapPath);
        ldapProperties.put(EmbeddedADS.PROPERTY_BIND_PORT, "10489");
        ldapProperties.put("ldap.primary.url", "ldap://localhost:10489/dc=people,dc=whydah,dc=no");
        FileUtils.deleteDirectories(ldapPath);

        main.startEmbeddedDS(ldapProperties);

        dataImporter = new IamDataImporter(dataSource, config, ldapProperties);
    }


    @AfterClass
    public void stop() {
        if (main != null) {
            main.stopEmbeddedDS();
        }
        
        try {
        	if(!dataSource.isClosed()) {
        		dataSource.close();
        	}
		} catch (SQLException e) {
			log.error("", e);
		}

        FileUtils.deleteDirectories(ldapPath);
    }
    
    @Test
    public void testDataIsImported() throws Exception {
        dataImporter.importIamData();
        LdapUserIdentityDao ldapUserIdentityDao = dataImporter.getLdapUserIdentityDao();

        LDAPUserIdentity erikdUserIdentity = ldapUserIdentityDao.getUserIndentity("erikd");
        assertEquals("Erik", erikdUserIdentity.getFirstName());
        assertEquals("Drolshammer", erikdUserIdentity.getLastName());
        assertEquals("erik.drolshammer", erikdUserIdentity.getUid());

        ApplicationService applicationService = new ApplicationService(new ApplicationDao(dataSource), null, null, null);
        UserAggregateService userAggregateService = new UserAggregateService(null, null, dataImporter.getUserApplicationRoleEntryDao(),
                applicationService, null, null);


        UserAggregate userAggregate2 = UserAggregateMapper.fromUserIdentityJson(UserIdentityMapper.toJson(erikdUserIdentity));
        List<UserApplicationRoleEntry> userApplicationRoleEntries = userAggregateService.getUserApplicationRoleEntries(erikdUserIdentity.getUid());
        userAggregate2.setRoleList(userApplicationRoleEntries);

        List<UserApplicationRoleEntry> propsAndRoles2 = userAggregate2.getRoleList();
        assertEquals(1, propsAndRoles2.size());
        //false data assessment
        //assertTrue(containsRoleMapping(propsAndRoles2, "erik.drolshammer", "2212", "Whydah-UserAdminService", "Capra Consulting", "WhydahUserAdmin", "70"));
    }

    private boolean containsRoleMapping(List<UserApplicationRoleEntry> propsAndRoles, String uid, String appId, String appName, String orgName, String roleName, String roleValue) {
        for (UserApplicationRoleEntry role : propsAndRoles) {
            if (role.getApplicationId().equals(appId) &&
			   role.getApplicationName().equals(appName) &&
                    role.getOrgName().equals(orgName) &&
                    role.getRoleName().equals(roleName) &&
                    role.getRoleValue().equals(roleValue) &&
                    role.getUserId().equals(uid)) {
                return true;
			}
		}
		return false;
	}
}
