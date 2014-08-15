package net.whydah.identity.application;

import net.whydah.identity.audit.AuditLogRepository;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
public class ApplicationServiceTest {
    ApplicationRepository applicationRepositoryMock;
    AuditLogRepository auditLogRepositoryMock;
    ApplicationService applicationService;
    @Before
    public void setUp() throws Exception {
        applicationRepositoryMock = mock(ApplicationRepository.class);
        auditLogRepositoryMock = mock(AuditLogRepository.class);
        applicationService = new ApplicationService(applicationRepositoryMock, auditLogRepositoryMock);
    }
    @Test
    public void testCreateApplication() throws Exception {
        Application application = applicationService.createApplication(allApplication);
        assertNotNull(application);
        assertEquals("id1", application.getId());
        assertEquals("test", application.getName());
        assertEquals("default1role", application.getDefaultRoleName());
        assertEquals("defaultorgname", application.getDefaultOrgName());
        List<String> availableOrgNames = application.getAvailableOrgNames();
        assertNotNull(availableOrgNames);
        assertEquals("developer@customer", availableOrgNames.get(0));
        assertEquals("consultant@customer", availableOrgNames.get(1));

    }
    @Test
    public void testGetApplications() throws Exception {
        Application application = applicationService.createApplication(allApplication);
        Application application_2 = applicationService.createApplication(application2);
        List<Application> applications = applicationService.getApplications();
        assertNotNull(applications);
        assertEquals(2,applications.size());
    }

    private final String allApplication = "{\"id\":\"id1\",\"name\":\"test\",\"defaultRoleName\":\"default1role\",\"defaultOrgName\":\"defaultorgname\",\"availableOrgNames\":[\"developer@customer\",\"consultant@customer\"]}";
    private final String application2 = "{\"id\":\"id2\",\"name\":\"test2\",\"defaultRoleName\":\"default1role\",\"defaultOrgName\":\"defaultorgname\",\"availableOrgNames\":[\"developer@customer\",\"consultant@customer\"]}";

}
