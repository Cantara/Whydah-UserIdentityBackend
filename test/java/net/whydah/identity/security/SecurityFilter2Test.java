package net.whydah.identity.security;

import net.whydah.identity.application.authentication.ApplicationTokenService;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.user.authentication.SecurityTokenServiceHelper;
import net.whydah.identity.user.authentication.UserToken;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-06-30
 */
public class SecurityFilter2Test {
    private static ApplicationTokenService applicationTokenService;
    private SecurityTokenServiceHelper stsHelper;
    private SecurityFilter securityFilter;

    private final static String userAdminUserTokenId ="au123";
    private final static String tokenBrukeradmin = "<application ID=\"1\"><organizationName>2</organizationName><role name=\"WhydahUserAdmin\"/></application>";

    @BeforeClass
    public static void setDefaultApplicationMode() {
        applicationTokenService = new ApplicationTokenService();
    }

    @Before
    public void setup() throws ServletException {
        ApplicationMode.clearTags();
        stsHelper = mock(SecurityTokenServiceHelper.class);
        securityFilter = new SecurityFilter(stsHelper, applicationTokenService);
    }

    @Test
    public void testHealthEndpoint() {
        assertNull(securityFilter.authenticateAndAuthorizeRequest("/health"));
    }

    @Test
    public void testSkipSecurityFilter() {
        ApplicationMode.setTags(ApplicationMode.NO_SECURITY_FILTER);
        assertNull(securityFilter.authenticateAndAuthorizeRequest("/anyPath"));
        assertEquals(Authentication.getAuthenticatedUser().getName(), "MockUserToken");
    }

    @Test
    public void testPasswordEndpoint() {
        assertNull(securityFilter.authenticateAndAuthorizeRequest("/password/appTokenIdPassword"));
    }
    @Test
    public void testAuthenticateUserEndpoint() {
        assertNull(securityFilter.authenticateAndAuthorizeRequest("/appTokenIdUser/authenticate/user"));
    }

    @Test
    public void testUsertokenIdAuthenticationOK() {
        String appTokenId = "appTokenIdUser";
        when(stsHelper.getUserToken(appTokenId, userAdminUserTokenId)).thenReturn(new UserToken(tokenBrukeradmin));
        assertNull(securityFilter.authenticateAndAuthorizeRequest("/" + appTokenId + "/" + userAdminUserTokenId + "/user"));
        assertEquals(Authentication.getAuthenticatedUser().getUserRoles().get(0).getRoleName(), "WhydahUserAdmin");
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
