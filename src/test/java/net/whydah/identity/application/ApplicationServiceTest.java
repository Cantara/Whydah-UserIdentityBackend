package net.whydah.identity.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.identity.audit.AuditLogDao;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
public class ApplicationServiceTest {
    ApplicationDao applicationDaoMock;
    AuditLogDao auditLogDaoMock;
    ApplicationService applicationService;
    ObjectMapper mapper = new ObjectMapper();

    private final String allApplication = "{\"id\":\"11\",\"name\":\"SecurityTokenService\",\"defaultRoleName\":\"WhydahDefaultUser\",\"defaultOrgName\":\"Whydah\",\"availableOrgNames\":[\"Whydah\",\"ACSOrganization\"],\"availableRoleNames\":[\"WhydahDefaultUser\",\"WhydahUserAdmin\"]}";
    private final String application2 = "{\"id\":\"12\",\"name\":\"UserAdminService\",\"defaultRoleName\":\"WhydahUserAdmin\",\"defaultOrgName\":\"Whydah\",\"availableOrgNames\":[\"Whydah\",\"ACSOrganization\"],\"availableRoleNames\":[\"WhydahDefaultUser\",\"WhydahUserAdmin\"]}";


    //private final String minimumApplication = "{\"id\":\"id1\",\"name\":\"test\",\"defaultRoleName\":null,\"defaultOrgName\":null,\"availableOrgNames\":[]}";
    //private final String mostApplication = "{\"id\":\"id1\",\"name\":\"test\",\"defaultRoleName\":\"defaultrole\",\"defaultOrgName\":\"defaultorgname\",\"availableOrgNames\":[]}";
    //private final String allApplication = "{\"id\":\"id1\",\"name\":\"test\",\"defaultRoleName\":\"defaultrole\",\"defaultOrgName\":\"defaultorgname\",\"availableOrgNames\":[\"developer@customer\",\"consultant@customer\"]}";

    private static final String minApplication = "{\"id\":\"id1\",\"name\":\"name1\",\"description\":null,\"availableRoles\":[],\"defaultRoleName\":null,\"availableOrgNames\":[],\"defaultOrgName\":null,\"availableRoleNames\":null}";
    private static final String maxApplication = "{\"id\":\"appId1\",\"name\":\"appName1\",\"description\":\"description1\",\"availableRoles\":[{\"id\":\"roleId1\",\"name\":\"roleName1\"},{\"id\":\"roleId2\",\"name\":\"roleName2\"}],\"defaultRoleName\":\"defaultRoleName1\",\"availableOrgNames\":[\"orgName1\",\"orgName2\",\"orgName3\"],\"defaultOrgName\":\"defaultOrgName1\",\"availableRoleNames\":[\"roleName1\",\"roleName2\"]}";


    @Before
    public void setUp() throws Exception {
        applicationDaoMock = mock(ApplicationDao.class);
        auditLogDaoMock = mock(AuditLogDao.class);
        applicationService = new ApplicationService(applicationDaoMock, auditLogDaoMock);
    }

    @Test
    public void testToJsonMinimum() throws Exception {
        Application application = new Application("id1", "name1");
        application.setSecret("verySecretKeyHere");
        String json = ApplicationService.toJson(application);
        JSONAssert.assertEquals(minApplication, json, false);
        assertFalse(json.contains(application.getSecret()));
    }

    @Test
    public void testToJsonComplete() throws Exception {
        Application application = new Application("appId1", "appName1");
        application.setSecret("verySecretKeyHere");
        application.setDefaultRoleName("defaultRoleName1");
        application.setDefaultOrgName("defaultOrgName1");
        application.setDescription("description1");
        application.addRole(new Role("roleId1", "roleName1"));
        application.addRole(new Role("roleId2", "roleName2"));
        application.setAvailableOrgNames(Arrays.asList("orgName1", "orgName2", "orgName3"));
        String json = ApplicationService.toJson(application);
        JSONAssert.assertEquals(maxApplication, json, false);
        assertFalse(json.contains(application.getSecret()));
    }


    /*
    @Test
    public void testToJson() throws Exception {
        Application application = new Application("id1", "test");
        JSONAssert.assertEquals(minimumApplication, ApplicationService.toJson(application), false);
        application = new Application("id1", "test");
        application.setDefaultRoleName("defaultrole");
        application.setDefaultOrgName("defaultorgname");
        JSONAssert.assertEquals(mostApplication, ApplicationService.toJson(application), false);
        List<String> availableOrgNames = new ArrayList<>();
        availableOrgNames.add("developer@customer");
        availableOrgNames.add("consultant@customer");
        application.setAvailableOrgNames(availableOrgNames);
        JSONAssert.assertEquals(allApplication, ApplicationService.toJson(application), false);
    }
    */

    /*
    @Test
    public void testCreateApplication() throws Exception {
        Application application = applicationService.createApplication(allApplication);
        assertNotNull(application);
        assertEquals("11", application.getId());
        assertEquals("SecurityTokenService", application.getName());
        assertEquals("WhydahDefaultUser", application.getDefaultRoleName());
        assertEquals("Whydah", application.getDefaultOrgName());
        List<String> availableOrgNames = application.getAvailableOrgNames();
        assertNotNull(availableOrgNames);
        assertEquals("Whydah", availableOrgNames.get(0));
        assertEquals("ACSOrganization", availableOrgNames.get(1));
        Application aapplication = applicationService.createApplication(allApplication);

    }

    @Test
    public void testCreateDuplicateApplication() throws Exception {
        Application application = applicationService.createApplication(allApplication);
        applicationService.createApplication(allApplication);
        applicationService.createApplication(allApplication);
        applicationService.createApplication(allApplication);
    }

    @Test
    public void testGetApplications() throws Exception {
        Application application = applicationService.createApplication(allApplication);
        Application application_2 = applicationService.createApplication(application2);
        List<Application> applications = new LinkedList();
        applications.add(application);
        applications.add(application_2);
        assertNotNull(applications);
        assertEquals(2,applications.size());

        //System.out.println(buildApplicationsJson(applications));
    }
    */


    protected String buildApplicationsJson(List<Application> applications) {
        String applicationsCreatedJson = null;
        try {
            applicationsCreatedJson = mapper.writeValueAsString(applications);
        } catch (IOException e) {
        }
        return applicationsCreatedJson;
    }
}
