package net.whydah.identity.security;

import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.health.HealthCheckService;
import net.whydah.identity.user.authentication.SecurityTokenServiceClient;
import net.whydah.sso.user.mappers.UserTokenMapper;
import org.junit.Before;
import org.junit.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-06-30
 */
public class SecurityFilter2Test {
    private SecurityTokenServiceClient stsHelper;
    private AuthenticationService authenticationService;
    private SecurityFilter securityFilter;
    private HealthCheckService healthCheckService;

    private final static String userAdminUserTokenId = UUID.randomUUID().toString();
    private final static String tokenBrukeradmin = "<application ID=\"1322\"><organizationName>2323</organizationName><role name=\"WhydahUserAdmin\"/></application>";

    String userTokenXML = "<usertoken xmlns:ns2=\"http://www.w3.org/1999/xhtml\" id=\"a96a517f-cef3-4be7-92f5-f059b65e4071\">\n" +
            "    <uid>myuid</uid>\n" +
            "    <timestamp></timestamp>\n" +
            "    <lifespan>3600000</lifespan>\n" +
            "    <issuer>/token/issuer/tokenverifier</issuer>\n" +
            "    <securitylevel>0</securitylevel>\n" +
            "    <username>test</username>\n" +
            "    <cellphone>90088900</cellphone>\n" +
            "    <firstname>Olav</firstname>\n" +
            "    <lastname>Nordmann</lastname>\n" +
            "    <email></email>\n" +
            "    <personRef></personRef>\n" +
            "    <lastSeen></lastSeen>  <!-- Whydah 2.1 date and time of last registered user session -->\n" +
            "    <application ID=\"2349785543\">\n" +
            "        <applicationName>Whydah.net</applicationName>\n" +
            "           <organizationName>Kunde 3</organizationName>\n" +
            "              <role name=\"WhydahUserAdmin\" value=\"\"/>\n" +
            "              <role name=\"president\" value=\"\"/>\n" +
            "           <organizationName>Kunde 4</organizationName>\n" +
            "              <role name=\"styremedlem\" value=\"\"/>\n" +
            "    </application>\n" +
            "    <application ID=\"appa\">\n" +
            "        <applicationName>whydag.org</applicationName>\n" +
            "        <organizationName>Kunde 1</organizationName>\n" +
            "        <role name=\"styremedlem\" value=\"Valla\"/>\n" +
            "    </application>\n" +
            " \n" +
            "    <ns2:link type=\"application/xml\" href=\"/\" rel=\"self\"/>\n" +
            "    <hash type=\"MD5\">8a37ef9624ed93db4873035b0de3d1ca</hash>\n" +
            "</usertoken>";

    @Before
    public void setup() throws ServletException {
        ApplicationMode.clearTags();
        stsHelper = mock(SecurityTokenServiceClient.class);
        authenticationService = mock(AuthenticationService.class);
        healthCheckService = mock(HealthCheckService.class);
        securityFilter = new SecurityFilter(stsHelper, authenticationService, healthCheckService);
        SecurityFilter.setCIFlag(true);
    }

    @Test
    public void testOpenEndpoints() {
        assertNull(securityFilter.authenticateAndAuthorizeRequest("/health"));
    }

    @Test
    public void testSkipSecurityFilter() {
        ApplicationMode.setTags(ApplicationMode.NO_SECURITY_FILTER);
        assertNull(securityFilter.authenticateAndAuthorizeRequest("/anyPath"));
        assertEquals(Authentication.getAuthenticatedUser().getUserName(), "MockUserToken");
    }

    @Test
    public void testPathsWithoutAuth() {
        assertNull(securityFilter.authenticateAndAuthorizeRequest("/health"));

    }


    @Test
    public void testPathsWithoutUserTokenIdOK() {

        assertNull(securityFilter.authenticateAndAuthorizeRequest("/appTokenIdUser/authenticate/user"));
        assertNull(securityFilter.authenticateAndAuthorizeRequest("/appTokenIdUser/signup/user"));
        assertNull(securityFilter.authenticateAndAuthorizeRequest("/appTokenIdUser/user/someUid/reset_password"));
        assertNull(securityFilter.authenticateAndAuthorizeRequest("/appTokenIdUser/user/someUid/change_password"));
        assertNull(securityFilter.authenticateAndAuthorizeRequest("/appTokenIdUser/applications"));
        assertNull(securityFilter.authenticateAndAuthorizeRequest("/appTokenIdUser/applications/find"));
        assertNotNull(securityFilter.authenticateAndAuthorizeRequest("/appTokenIdUser/" + userAdminUserTokenId + "/application"));

    }

    @Test
    public void testUsertokenIdAuthenticationOK() {
        String appTokenId = UUID.randomUUID().toString();
        when(stsHelper.getUserToken(userAdminUserTokenId)).thenReturn(UserTokenMapper.fromUserTokenXml(userTokenXML));
        assertNull(securityFilter.authenticateAndAuthorizeRequest("/" + appTokenId + "/" + userAdminUserTokenId + "/user"));
        assertEquals(Authentication.getAuthenticatedUser().getRoleList().get(0).getRoleName(), "WhydahUserAdmin");
        assertNull(securityFilter.authenticateAndAuthorizeRequest("/" + appTokenId + "/" + userAdminUserTokenId + "/application"));

    }


    @Test
    public void testDoFilterAuthenticateAndAuthorizeRequestReturnsNull() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        //chain.doFilter
        when(request.getPathInfo()).thenReturn("/health");
        securityFilter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    //TODO testDoFilterAuthenticateAndAuthorizeRequestReturnsNull
}
