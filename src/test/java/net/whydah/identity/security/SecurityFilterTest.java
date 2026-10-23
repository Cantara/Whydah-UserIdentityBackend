package net.whydah.identity.security;

import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.health.HealthCheckService;
import net.whydah.identity.user.authentication.SecurityTokenServiceClient;
import net.whydah.sso.user.mappers.UserTokenMapper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@Ignore
public class SecurityFilterTest {
    private static final Logger log = LoggerFactory.getLogger(SecurityFilterTest.class);
    private SecurityFilter securityFilter;
    private SecurityTokenServiceClient tokenHelper;
    private AuthenticationService authenticationService;
    private HealthCheckService healthCheckService;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    private final static String tokenOther = "<application ID=\"1\"><organizationName>2</organizationName><role name=\"Vaktmester\"/></application>";
    private final static String tokenBrukeradmin = "<application ID=\"1\"><organizationName>2</organizationName><role name=\"WhydahUserAdmin\"/></application>";
    private final static String applicationTokenId="abcdefgid";
    private final static String userAdminUserTokenId ="au123";
    private final static String userTokenInvalid ="uti123";
    private final static String userTokenMissingGroup ="utig123";

    //ED: Why should this be necessary?
    /*
    private static String iamMode = null;

    @BeforeClass
    public static void fetchProperties() throws Exception {
        iamMode = System.getenv(ApplicationMode.IAM_MODE_KEY);
        log.info("Fetching original property {}, value {}", ApplicationMode.IAM_MODE_KEY, iamMode);
        System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.TEST);
    }

    @AfterClass
    public static void setOriginalProperties() throws Exception {
        log.info("Setting original property {}, value {}", ApplicationMode.IAM_MODE_KEY, iamMode);
        if (iamMode != null) {
            System.setProperty(ApplicationMode.IAM_MODE_KEY, iamMode);
        }
    }
    */
    @BeforeClass
    public static void setDefaultApplicationMode() {
        ApplicationMode.clearTags();
    }


    @Before
    public void init() throws ServletException {
        tokenHelper = mock(SecurityTokenServiceClient.class);
        authenticationService = mock(AuthenticationService.class);
        healthCheckService = mock(HealthCheckService.class);
        securityFilter = new SecurityFilter(tokenHelper, authenticationService, healthCheckService);

        securityFilter.init(null);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);

    }

    @Test
    public void testSecuredTokenNotValid() throws Exception {
        when(request.getPathInfo()).thenReturn("/" +userTokenInvalid +"/users");
        when(tokenHelper.getUserToken(userTokenInvalid)).thenReturn(null);
        securityFilter.doFilter(request, response, chain);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoMoreInteractions(chain);
    }

    @Ignore
    @Test
    public void testSecuredTokenOkMissingGroup() throws Exception {
        when(request.getPathInfo()).thenReturn("/apptoken/" +userTokenMissingGroup +"/users");
        when(tokenHelper.getUserToken(userTokenMissingGroup)).thenReturn(UserTokenMapper.fromUserTokenXml(tokenOther));
        securityFilter.doFilter(request, response, chain);
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void testSecuredTokenOkRoleOk() throws Exception {
        when(request.getPathInfo()).thenReturn("/apptoken/" + userAdminUserTokenId +"/users");
        when(tokenHelper.getUserToken(userAdminUserTokenId)).thenReturn(UserTokenMapper.fromUserTokenXml(tokenBrukeradmin));
        securityFilter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    public void userOk() throws Exception {
        when(request.getPathInfo()).thenReturn("/apptoken/" + userAdminUserTokenId +"/user");
        when(tokenHelper.getUserToken( userAdminUserTokenId)).thenReturn(UserTokenMapper.fromUserTokenXml(tokenBrukeradmin));
        securityFilter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
    @Test
    public void applicationOk() throws Exception {
        when(request.getPathInfo()).thenReturn("/apptoken/" + userAdminUserTokenId +"/application");
        when(tokenHelper.getUserToken(userAdminUserTokenId)).thenReturn(UserTokenMapper.fromUserTokenXml(tokenBrukeradmin));
        securityFilter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
    @Test
    public void applicationsOk() throws Exception {
        when(request.getPathInfo()).thenReturn("/apptoken/" + userAdminUserTokenId +"/user/admin/applications");
        when(tokenHelper.getUserToken( userAdminUserTokenId)).thenReturn(UserTokenMapper.fromUserTokenXml(tokenBrukeradmin));
        securityFilter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }


    @Test
    public void testFindPathElement() throws Exception {
        assertEquals("/usertoken", securityFilter.findPathElement("/123/usertoken/", 2));
        assertEquals("/123", securityFilter.findPathElement("/123/usertoken/", 1));
        assertEquals(null, securityFilter.findPathElement("/123/usertoken/", 3));
        assertEquals(null, securityFilter.findPathElement("", 3));
        assertEquals(null, securityFilter.findPathElement(null, 1));
    }

    @Test
    public void findUserTokenId() throws Exception {
        assertEquals(null, findUserTokenId(null));
        assertEquals("1234", findUserTokenId("/appd/1234"));
    }
    /**
     * Plukker element 2 fra path som usertokenid. F.eks. /useradmin/1kj2h1j12jh/users/add gir 1kj2h1j12jh.
     *
     * @param pathInfo fra servletRequest.getPathInfo()
     * @return authentication
     */
    protected String findUserTokenId(String pathInfo) {
        if (pathInfo != null && !pathInfo.startsWith("/")) {
            log.error("Call to UIB does not startJetty with '/' which can be because of configuration problem. Problematic Path: {}", pathInfo);
        }
        String tokenIdPath = securityFilter.findPathElement(pathInfo, 2);
        String tokenId = null;
        if (tokenIdPath != null) {
            tokenId = tokenIdPath.substring(1);
        }
        return tokenId;

    }

    @Test
    public void verifyAuthenticateApplicationUrl() throws Exception {
        when(request.getPathInfo()).thenReturn("/authenticate/application");
        securityFilter.doFilter(request, response,chain);
        verify(chain).doFilter(request, response);
        log.debug("Status {}", response.getStatus());
    }


    @Test
    public void verifyAuthenticateUserUrl() throws Exception {
        when(request.getPathInfo()).thenReturn("/" +applicationTokenId + "/authenticate/user");
        //when(applicationTokenService.verifyApplication(anyString())).thenReturn(true);
        securityFilter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        log.debug("Status {}", response.getStatus());
    }
    @Test
    public void verifyAuthenticateUserMissingApplicationTokenId() throws Exception {
        when(request.getPathInfo()).thenReturn("//usertoken/");
        securityFilter.doFilter(request, response,chain);
        log.debug("Status {}", response.getStatus());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoMoreInteractions(chain);
    }
    @Test
    public void verifyUserTokenUrlWrongUrl() throws Exception {
        when(request.getPathInfo()).thenReturn("/usertoken/");
        securityFilter.doFilter(request, response,chain);
        log.debug("Status {}", response.getStatus());
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verifyNoMoreInteractions(chain);
    }
}
