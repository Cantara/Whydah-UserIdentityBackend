package net.whydah.identity.security;

import net.whydah.identity.application.authentication.ApplicationTokenService;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.user.UserRole;
import net.whydah.identity.user.authentication.SecurityTokenServiceHelper;
import net.whydah.identity.user.authentication.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sjekker om request path krever autentisering, og i såfall sjekkes authentication.
 * Secured paths are added as comma separated list in filterConfig. Required role is also configured with filterConfig.
 */
public class SecurityFilter implements Filter {
    public static final String OPEN_PATH = "/authenticate";
    public static final String TEST_PATH = "/test";
    public static final String AUTHENTICATE_USER_PATH = "/authenticate";
    public static final String PASSWORD_RESET_PATH = "/password";
    public static final String SECURED_PATHS_PARAM = "securedPaths";
    public static final String REQUIRED_ROLE_USERS = "WhydahUserAdmin";
    public static final String REQUIRED_ROLE_APPLICATIONS = "WhydahUserAdmin";
    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    private final SecurityTokenServiceHelper securityTokenHelper;
    private final ApplicationTokenService applicationTokenService;
    private List<String> securedPaths = new ArrayList<>();
    private String requiredRole;

    public SecurityFilter(SecurityTokenServiceHelper securityTokenHelper, ApplicationTokenService applicationTokenService) {
        this.securityTokenHelper = securityTokenHelper;
        this.applicationTokenService = applicationTokenService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        requiredRole = REQUIRED_ROLE_USERS;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        String pathInfo = servletRequest.getPathInfo();
        logger.trace("filter path {}", pathInfo);
        if (pathInfo == null) {
            logger.trace("No data in input - probably due to error in URL.  Configured (eg: ...:9995/uib)");
            return;
        } else if (isOpenPath(pathInfo)) {
            logger.trace("accessing open path {}", pathInfo);
            chain.doFilter(request, response);
        } else if (isTestPath(pathInfo)) {
            logger.trace("accessing test path {}", pathInfo);
            if ("true".equals(AppConfig.appConfig.getProperty("testpage"))) {
                chain.doFilter(request, response);
            } else {
                logger.info("Test resource available with configuration testpage=true");
                setResponseStatus((HttpServletResponse) response, HttpServletResponse.SC_FORBIDDEN);
            }
        } else {
            if (isAuthenticateUserPath(pathInfo)) {
                //Verify applicationTokenId
                String applicationTokenId = findPathElement(pathInfo, 1);
                if (applicationTokenService.verifyApplication(applicationTokenId)) {
                    logger.trace("application verified {}. Moving to next in chain.", applicationTokenId);
                    chain.doFilter(request, response);
                } else {
                    logger.trace("Application not Authorized=" + applicationTokenId);
                    setResponseStatus((HttpServletResponse) response, HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            } else if (isPasswordPath(pathInfo)) {
                //TODO bli: Needs improvement -aka dont repeat your self.
                String applicationTokenId = findPathElement(pathInfo, 2);
                if (applicationTokenService.verifyApplication(applicationTokenId)) {
                    logger.trace("application verified {}. Moving to next in chain.", applicationTokenId);
                    chain.doFilter(request, response);
                } else {
                    logger.trace("Application not Authorized=" + applicationTokenId);
                    setResponseStatus((HttpServletResponse) response, HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            } else {
                //Verify userTokenId
                String usertokenId = findUserTokenId(pathInfo);
                String applicationTokenId = findApplicationTokenId(pathInfo);
                logger.debug("usertokenid: {} from path={} ", usertokenId, pathInfo);
                if (usertokenId == null) {
                    logger.trace("Token not found");
                    setResponseStatus((HttpServletResponse) response, HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                logger.trace("ApplicationMode -{}-", ApplicationMode.getApplicationMode());
                if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
                    logger.warn("Running in DEV mode, security is ommited for users.");
                    Authentication.setAuthenticatedUser(buildMockedUserToken());
                } else {
                    UserToken userToken = securityTokenHelper.getUserToken(applicationTokenId, usertokenId);

                    if (userToken == null) {
                        logger.trace("Could not find token with tokenID=" + usertokenId);
                        setResponseStatus((HttpServletResponse) response, HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    if (!userToken.hasRole(requiredRole)) {
                        logger.trace("Missing required role {}", requiredRole);
                        setResponseStatus((HttpServletResponse) response, HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    logger.debug("setAuthenticatedUser with usertoken: {}", userToken);
                    Authentication.setAuthenticatedUser(userToken);
                }
                chain.doFilter(request, response);
            }

        }
        Authentication.clearAuthentication();
    }


    private UserToken buildMockedUserToken() {

        List<UserRole> roles = new ArrayList<>();
        roles.add(new UserRole("9999", "99999", "mockrole"));
        return new UserToken("MockUserToken", roles);
    }

    private boolean isAuthenticateUserPath(String pathInfo) {
        boolean isAuthenticateUserPath = false;
        String pathElement = findPathElement(pathInfo, 2);
        if (pathElement != null) {
            isAuthenticateUserPath = pathElement.startsWith(AUTHENTICATE_USER_PATH);
        }
        return isAuthenticateUserPath;
    }

    private boolean isPasswordPath(String pathInfo) {
        boolean authenticataApplicationOnly = false;
        String pathElement = findPathElement(pathInfo, 1);
        if (pathElement != null) {
            authenticataApplicationOnly = pathElement.startsWith(PASSWORD_RESET_PATH);
        }
        return authenticataApplicationOnly;
    }

    protected String findPathElement(String pathInfo, int elementNumber) {
        String pathElement = null;
        if (pathInfo != null) {
            String[] pathElements = pathInfo.split("/");
            if (pathElements.length > elementNumber) {
                pathElement = "/" + pathElements[elementNumber];
            }
        }
        return pathElement;
    }

    protected boolean isOpenPath(String pathInfo) {
        return pathInfo.startsWith(OPEN_PATH);
    }

    private boolean isTestPath(String pathInfo) {
        return pathInfo.startsWith(TEST_PATH);
    }

    private void setResponseStatus(HttpServletResponse response, int statuscode) {
        response.setStatus(statuscode);
    }

    /**
     * Plukker element 2 fra path som usertokenid. F.eks. /useradmin/1kj2h1j12jh/users/add gir 1kj2h1j12jh.
     *
     * @param pathInfo fra servletRequest.getPathInfo()
     * @return authentication
     */
    protected String findUserTokenId(String pathInfo) {
        if (pathInfo != null && !pathInfo.startsWith("/")) {
            logger.error("Call to UIB does not start with '/' which can be because of configuration problem. Problematic Path: {}", pathInfo);
        }
        String tokenIdPath = findPathElement(pathInfo, 2);
        String tokenId = null;
        if (tokenIdPath != null) {
            tokenId = tokenIdPath.substring(1);
        }
        return tokenId;

    }

    protected String findApplicationTokenId(String pathInfo) {
        String tokenIdPath = findPathElement(pathInfo, 1);
        String tokenId = null;
        if (tokenIdPath != null) {
            tokenId = tokenIdPath.substring(1);
        }
        return tokenId;

    }

    private boolean isSecuredPath(String pathInfo) {
        for (String securedPath : securedPaths) {
            if (pathInfo.startsWith(securedPath)) {
                logger.info("Secured: {}", pathInfo);
                return true;
            }
        }
        logger.info("Not secured: {}", pathInfo);
        return false;
    }

    @Override
    public void destroy() {
    }
}
