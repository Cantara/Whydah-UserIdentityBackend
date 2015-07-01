package net.whydah.identity.dataimport;

import net.whydah.identity.util.FileUtils;
import net.whydah.sso.application.Application;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ApplicationImporterTest {

	@Test
	public void parseRoles() {
		String applicationsSource = "testapplications.csv";
        InputStream applicationStream = FileUtils.openFileOnClasspath(applicationsSource);
		List<Application> applications = ApplicationImporter.parseApplications(applicationStream);
		
//		#applicationId, applicationName, defaultRole, defaultOrgId
//		1, WhydahUserAdmin, WhydahUserAdmin, 1
//		2, Mobilefirst, Client, 2
//		3, Development, Developer, 2
		
		assertEquals("All organizations must be found.", 6, applications.size());

        Application application1 = applications.get(0);
		assertEquals("applicationId must be set.", "11", application1.getId());
		assertEquals("applicationName must be set.", "SecurityTokenService", application1.getName());
		assertEquals("defaultRole must be set.", "SSOApplication", application1.getDefaultRoleName());
		assertEquals("defaultOrgName must be set.", "Whydah", application1.getDefaultOrganizationName());


        Application application3 = applications.get(2);
		assertEquals("applicationId must be set.", "15", application3.getId());
		assertEquals("applicationName must be set.", "SSOLoginWebApplication", application3.getName());
		assertEquals("defaultRole must be set.", "SSOApplication", application3.getDefaultRoleName());
		assertEquals("defaultOrgName must be set.", "Whydah", application3.getDefaultOrganizationName());

	}
}
