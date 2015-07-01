package net.whydah.identity.dataimport;

import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.util.FileUtils;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RoleMappingImporterTest {

	@Test
	public void parseRoles() {
		String roleMappingSource = "testrolemappings.csv";

        InputStream roleMappingStream = FileUtils.openFileOnClasspath(roleMappingSource);
		List<UserPropertyAndRole> roleMappings = RoleMappingImporter.parseRoleMapping(roleMappingStream);
		
//		#userId, applicationId, applicationName, organizationId, organizationName, roleName, roleValue
//		username@emailaddress.com, 42, mobilefirst, 23, altran, developer, 30
//		username@emailaddress.com, 42, mobilefirst, 23, altran, client, 10
//		username@emailaddress.com, 11, whydah, 7, whydah, developer, 20
//		erik.drolshammer, 42, mobilefirst, 23, altran, admin, 70

		assertEquals("All rolemappings must be found.", 4, roleMappings.size());
		
		UserPropertyAndRole roleMapping1 = roleMappings.get(0);
		assertEquals("UserId must be set.", "username@emailaddress.com", roleMapping1.getUid());
		assertEquals("applicationId must be set.", "2", roleMapping1.getApplicationId());
		assertEquals("applicationName must be set.", "Mobilefirst", roleMapping1.getApplicationName());
		assertEquals("organizationName must be set.", "Altran", roleMapping1.getOrganizationName());
		assertEquals("roleName must be set.", "developer", roleMapping1.getApplicationRoleName());
		assertEquals("roleValue must be set.", "30", roleMapping1.getApplicationRoleValue());
		
		UserPropertyAndRole roleMapping4 = roleMappings.get(3);
		assertEquals("UserId must be set.", "erik.drolshammer", roleMapping4.getUid());
		assertEquals("applicationId must be set.", "2", roleMapping4.getApplicationId());
		assertEquals("applicationName must be set.", "Mobilefirst", roleMapping4.getApplicationName());
		assertEquals("organizationName must be set.", "Altran", roleMapping4.getOrganizationName());
		assertEquals("roleName must be set.", "admin", roleMapping4.getApplicationRoleName());
		assertEquals("roleValue must be set.", "70", roleMapping4.getApplicationRoleValue());
		

	}
}
