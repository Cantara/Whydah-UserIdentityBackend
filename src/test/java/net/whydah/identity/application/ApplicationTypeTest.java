package net.whydah.identity.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.sso.application.helpers.ApplicationHelper;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationACL;
import net.whydah.sso.application.types.ApplicationAvailableOrganizationNames;
import net.whydah.sso.application.types.ApplicationAvailableRoleNames;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ApplicationTypeTest {
    private static final Logger log = LoggerFactory.getLogger(ApplicationTypeTest.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static Application app1;

    @BeforeClass
    public static void initTestData() {
        app1 = new Application("appId1", "applicationName1");
        app1.setDescription("description of application");
        app1.setApplicationUrl("https://webtest.exapmle.com/test.png");
        app1.setLogoUrl("https://webtest.example.com");
        app1.addRole(new ApplicationAvailableRoleNames("roleId1", "roleName1"));
        app1.addOrganizationName(new ApplicationAvailableOrganizationNames("orgId", "organizationName1"));
        app1.setDefaultRoleName("defaultRoleName");
        app1.setDefaultRoleName("roleName1");
        app1.setDefaultOrganizationName("defaultOrgName");
        app1.addOrganizationName(new ApplicationAvailableOrganizationNames("orgidxx", app1.getDefaultOrganizationName()));
        app1.addAcl(new ApplicationACL("11", "/user", "READ"));

        app1.getSecurity().setSecret("veryVerySecret");

    }

    @Test
    public void testToFromJson() throws Exception {

        System.out.println(ApplicationMapper.toJson(app1));

        String indented = ApplicationMapper.toPrettyJson(app1);
        log.debug("\n" + indented);

        Application applicationFromJson = ApplicationMapper.fromJson(indented);
        assertEquals(app1, applicationFromJson);
    }

    @Test
    public void testToApplicationList() throws Exception {
        Application app2 = new Application("appId2", "applicationName2");
        String json = ApplicationMapper.toJson(Arrays.asList(new Application[]{app1, app2}));
        String indented = ApplicationMapper.toPrettyJson(Arrays.asList(new Application[]{app1, app2}));
        log.debug("\n" + indented);

        List<Application> applications = ApplicationMapper.fromJsonList(json);
        assertEquals(applications.size(), 2);
    }


    @Test
    public void fromRealJson() throws Exception {
        Application applicationFromJson = ApplicationMapper.fromJson(ApplicationHelper.getDummyApplicationJson());
        String jsonString = ApplicationMapper.toPrettyJson(applicationFromJson);
        log.debug(jsonString);
        Application applicationBackFromJson = ApplicationMapper.fromJson(jsonString);
        assertEquals(applicationFromJson, applicationBackFromJson);
    }

    @Test
    public void fromRealJsonList() throws Exception {
        List<Application> applications = ApplicationMapper.fromJsonList(ApplicationHelper.getDummyAppllicationListJson());
        for (Application application : applications) {
            log.debug(ApplicationMapper.toJson(application));
        }

    }
}

