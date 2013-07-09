package no.freecode.iam.service.dataimport;

import static org.junit.Assert.assertEquals;

import java.util.List;

import no.freecode.iam.service.dataimport.WhydahUserIdentityImporter;
import no.freecode.iam.service.domain.WhydahUserIdentity;

import org.junit.Test;

public class WhydahUserIdentityImporterTest {

	@Test
	public void parseUsers() {
		String userImportSource = "testusers.csv";
		
		List<WhydahUserIdentity> users = WhydahUserIdentityImporter.parseUsers(userImportSource);
		
		assertEquals("All users must be found.", 2, users.size());
		
		WhydahUserIdentity user1 = users.get(0);
		assertEquals("UserId must be set.", "thomas.pringle@altran.com", user1.getUid());
		assertEquals("UserName must be set.", "thomasp", user1.getUsername());
		assertEquals("Password must be set.", "logMeInPlease", user1.getPassword());
		assertEquals("Firstname must be set.", "Thomas", user1.getFirstName());
		assertEquals("Lastname must be set.", "Pringle", user1.getLastName());
		assertEquals("email must be set.", "thomas.pringle@altran.com", user1.getEmail());
		assertEquals("cellPhone must be set.", "+46707771841", user1.getCellPhone());
		assertEquals("personRef must be set.", "1", user1.getPersonRef());
		
		WhydahUserIdentity user2 = users.get(1);
		assertEquals("UserId must be set.", "erik.drolshammer", user2.getUid());
		assertEquals("UserName must be set.", "erikd", user2.getUsername());
		assertEquals("cellPhone must be set.", "+47123456", user2.getCellPhone());
		assertEquals("personRef must be set.", "2", user2.getPersonRef());
	}
}
