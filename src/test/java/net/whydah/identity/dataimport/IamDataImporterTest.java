package net.whydah.identity.dataimport;

import net.whydah.identity.Main;
import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.application.ApplicationService;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.RDBMSLdapUserIdentityRepository;
import net.whydah.identity.user.identity.RDBMSUserIdentity;
import net.whydah.identity.user.identity.UserIdentityServiceV2;
import net.whydah.identity.util.FileUtils;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.mappers.UserIdentityMapper;
import net.whydah.sso.user.types.UserAggregate;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserIdentity;
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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;


public class IamDataImporterTest {
    private static final Logger log = LoggerFactory.getLogger(IamDataImporterTest.class);
    private static final String ldapPath = "target/IamDataImporterTest/ldap";

    private static BasicDataSource dataSource;
    private static IamDataImporter dataImporter;
    private static Main main;

    @BeforeClass
    public static void startServer() {
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

        dataImporter = new IamDataImporter(dataSource, config);
    }


    @AfterClass
    public static void stop() {
        if (main != null) {
        }

        try {
            if (!dataSource.isClosed()) {
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
        RDBMSLdapUserIdentityRepository userIdentityRepository = dataImporter.getUserIdentityRepository();

        RDBMSUserIdentity erikdUserIdentity = userIdentityRepository.getUserIdentityWithUsername("erikd");
        assertEquals("Erik", erikdUserIdentity.getFirstName());
        assertEquals("Drolshammer", erikdUserIdentity.getLastName());
        assertEquals("erik.drolshammer", erikdUserIdentity.getUid());

        UserIdentity erikRDBMSdUserIdentity = userIdentityRepository.getUserIdentityWithUsernameOrUid("erikd");
        assertEquals("Erik", erikRDBMSdUserIdentity.getFirstName());
        assertEquals("Drolshammer", erikRDBMSdUserIdentity.getLastName());
        assertEquals("erik.drolshammer", erikRDBMSdUserIdentity.getUid());

        UserIdentityServiceV2 userIdentityServiceV2 = mock(UserIdentityServiceV2.class);

        ApplicationService applicationService = new ApplicationService(new ApplicationDao(dataSource), null, null, null);
        UserAggregateService userAggregateService = new UserAggregateService(userIdentityServiceV2, dataImporter.getUserApplicationRoleEntryDao(),
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
