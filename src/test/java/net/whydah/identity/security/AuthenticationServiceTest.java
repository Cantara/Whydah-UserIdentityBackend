package net.whydah.identity.security;

import net.whydah.identity.application.ApplicationCredentialRepository;
import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.types.ApplicationCredential;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 23.11.15.
 */
public class AuthenticationServiceTest {
    private static final Logger log = getLogger(AuthenticationServiceTest.class);

    private AuthenticationService authenticationService;
    private ApplicationCredentialRepository appCredRepo;
    private ApplicationCredential storedUasApplicationCredential = new ApplicationCredential("2212","Whydah-UserAdminService","adsfasdfasdasdfasd");

    @Before
    public void setUp() throws Exception {
        appCredRepo = mock(ApplicationCredentialRepository.class);
        authenticationService = new AuthenticationService(appCredRepo);
    }

    @Test
    public void testIsAuthenticatedAsUAS() throws Exception {
//        log.info("xml:" + ApplicationCredentialMapper.toXML(storedUasApplicationCredential));
        when(appCredRepo.getUasAppCred()).thenReturn(storedUasApplicationCredential);
        ApplicationCredential validAppCred = ApplicationCredentialMapper.fromXml(applicatinCredential);
        assertTrue(authenticationService.isAuthenticatedAsUAS(validAppCred));

        try {
            ApplicationCredential inValidAppCred = ApplicationCredentialMapper.fromXml(applicatinCredentialMissingSecred);
            assertFalse(authenticationService.isAuthenticatedAsUAS(inValidAppCred));

        } catch (Exception e) {
            // Should end here is ApplicationCredential creation will fail
        }
    }

    private static String applicatinCredential = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n" +
            " <applicationcredential>\n" +
            "    <params>\n" +
            "        <applicationID>2212</applicationID>\n" +
            "        <applicationName>Whydah-UserAdminService</applicationName>\n" +
            "        <applicationSecret>adsfasdfasdasdfasd</applicationSecret>\n" +
            "    </params> \n" +
            "</applicationcredential>";

    private static String applicatinCredentialMissingSecred = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n" +
            " <applicationcredential>\n" +
            "    <params>\n" +
            "        <applicationID>2212</applicationID>\n" +
            "        <applicationName>Whydah-UserAdminService</applicationName>\n" +
            "        <applicationSecret></applicationSecret>\n" +
            "    </params> \n" +
            "</applicationcredential>";
}