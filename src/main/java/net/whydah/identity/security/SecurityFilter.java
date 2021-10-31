package net.whydah.identity.security;

import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.health.HealthCheckService;
import net.whydah.identity.user.authentication.SecurityTokenServiceClient;
import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sso.util.WhydahUtil;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * Sjekker om request path krever autentisering, og i såfall sjekkes authentication.
 * Secured paths are added as comma separated list in filterConfig. Required role is also configured with filterConfig.
 */
@Component
public class SecurityFilter implements Filter {
	private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

	public static final String APPLICATION_CREDENTIALS_HEADER_XML = "uas-app-credentials/xml";
	public static final String APPLICATION_CREDENTIALS_HEADER = "uas-app-credentials";
	public static final String pwPattern = "/user/.+/(reset|change)_password";
	// /password/6f485dd168bb999c7fb9696c75fad3c3/reset/username/totto@cantara.no
	//    public static final String pwPattern2 = "/password/(.*)/reset/username/(.*)";
	public static final String pwPattern2 = "(.*)/reset/username/(.*)";
	public static final String pwPattern3 = "/user/.+/password_login_enabled";
	public static final String userAuthPattern = "/authenticate/user(|/.*)";
	public static final String applicationAuthPatten = "/application/auth";
	public static final String applicationListPatten = "/applications";
	public static final String applicationSearchPatten = "/applications/find";
	public static final String applicationSearchPatten_ = "/applications/find/(.*?)";
	public static final String applicationSearchPatten2 = "/find/applications/(.*?)";
	public static final String applicationSearchPatten2_ = "/find/applications";
	public static final String userSignupPattern = "/signup/user";
	public static final String[] patternsWithoutUserTokenId = {applicationAuthPatten, pwPattern, pwPattern2, pwPattern3, userAuthPattern, userSignupPattern, applicationListPatten, applicationSearchPatten, applicationSearchPatten2, applicationSearchPatten_, applicationSearchPatten2_};
	public static final String HEALH_PATH = "/health";

	private final SecurityTokenServiceClient securityTokenHelper;
	private final AuthenticationService authenticationService;
	private final HealthCheckService healthCheckService;

	private static boolean isCI = false;
	//public static final String OPEN_PATH = "/authenticate";
	//public static final String AUTHENTICATE_USER_PATH = "/authenticate";
	//public static final String PASSWORD_RESET_PATH = "/password";
	//public static final String SECURED_PATHS_PARAM = "securedPaths";
	//public static final String REQUIRED_ROLE_USERS = "WhydahUserAdmin";
	//public static final String REQUIRED_ROLE_APPLICATIONS = "WhydahUserAdmin";
	//private List<String> securedPaths = new ArrayList<>();
	//private String requiredRole;

	@Autowired
	public SecurityFilter(SecurityTokenServiceClient securityTokenHelper, AuthenticationService authenticationService, HealthCheckService healthCheckService) {
		this.securityTokenHelper = securityTokenHelper;
		this.authenticationService = authenticationService;
		this.healthCheckService = healthCheckService;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		//requiredRole = REQUIRED_ROLE_USERS;
		SecurityFilter.setCIFlag(true);
	}


	/**
	 * @param pathInfo the path to apply the filter to
	 * @return HttpServletResponse.SC_UNAUTHORIZED if authentication fails, otherwise null
	 */
	Integer authenticateAndAuthorizeRequest(String pathInfo) {
		log.debug("filter path {}", pathInfo);

		//match /
		if (pathInfo == null || pathInfo.equals("/")) {
			return HttpServletResponse.SC_NOT_FOUND;
		}

		String path = pathInfo.substring(1); //strip leading /

		//Open paths without authentication
		if (path.startsWith(HEALH_PATH.substring(1))) {
			log.debug("{} was matched to {}. SecurityFilter passed.", path, HEALH_PATH);
			return null;
		}

		if (ApplicationMode.skipSecurityFilter()) {
			log.warn("Running in noSecurityFilter mode, security is omitted for users.");
			Authentication.setAuthenticatedUser(buildMockedUserToken());
			return null;
		}


		// /{stsApplicationtokenId}/application/auth        //ApplicationAuthenticationEndpoint


		//strip applicationTokenId from pathInfo
		if (path.indexOf("/") > 0) {
			path = path.substring(path.indexOf("/"));
		}


		//paths without userTokenId verification
		/*
        /{applicationTokenId}/user/{uid}/reset_password     //PasswordResource2
        /{applicationTokenId}/user/{uid}/change_password    //PasswordResource2
        /{applicationTokenId}/authenticate/user/*           //UserAuthenticationEndpoint
        /{stsApplicationtokenId}/application/auth")         //Applicationcredential verification endpoint  (ApplicationAuthenticationEndpoint)
        /{applicationtokenid}/applications    //ApplicationsResource
        /{applicationTokenId}/signup/user                   //UserSignupEndpoint
		 */
		for (String pattern : patternsWithoutUserTokenId) {
			if (Pattern.compile(pattern).matcher(path).matches()) {
				log.debug("{} was matched to {}. SecurityFilter passed.", path, pattern);
				return null;
			}
		}


		/*
        /{applicationtokenid}/{userTokenId}/application     //ApplicationResource
        /{applicationtokenid}/{userTokenId}/user            //UserResource
        /{applicationtokenid}/{usertokenid}/useraggregate   //UserAggregateResource
        /{applicationtokenid}/{usertokenid}/users           //UsersResource
		 */
		//paths WITH userTokenId verification
		try {
			String applicationTokenId = findPathElement(pathInfo, 1).substring(1);
			String usertokenId = findPathElement(pathInfo, 2).substring(1);
			if (applicationTokenId != null) {
				// Not calling myself
				if (!applicationTokenId.equalsIgnoreCase(securityTokenHelper.getActiveUibApplicationTokenId())) {
					UserToken userToken = securityTokenHelper.getUserToken(usertokenId);
					if (userToken == null || userToken.toString().length() < 10) {
						return HttpServletResponse.SC_UNAUTHORIZED;
					}

					// We are happy here :)
					Authentication.setAuthenticatedUser(userToken);
				} else {
					log.info("UIB is calling itself - shuld be OK");
				}
			} else {
				log.warn("Missing applicationTokenId in request ");
				return HttpServletResponse.SC_UNAUTHORIZED;
			}
		} catch (Exception e) {
			log.info("Unable to find expected elements in request path (applicationtokenid/userTokenId/) pathInfo: {}", pathInfo);
		}

		return null;
	}

	UserToken buildMockedUserToken() {
		UserApplicationRoleEntry role = new UserApplicationRoleEntry();
		role.setApplicationName("mockrole");
		role.setApplicationId("9999");
		role.setId("99999");
		UserToken mockToken = new UserToken();
		mockToken.setUserName("MockUserToken");
		mockToken.addApplicationRoleEntry(role);
		return mockToken;
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

	private void logHTTPHeaders(ServletRequest request) {
		if (request == null) {
			return;
		}
		HttpServletRequest servletRequest = (HttpServletRequest) request;
		Enumeration headerNames = servletRequest.getHeaderNames();
		if (headerNames == null) {
			return;
		}
		StringBuilder strb = new StringBuilder("HTTP headers: \n");
		while (headerNames.hasMoreElements()) {
			String headerName = (String) headerNames.nextElement();
			strb.append(headerName).append(" : ").append(servletRequest.getHeader(headerName)).append("\n");
		}
		log.trace(strb.toString());
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest servletRequest = (HttpServletRequest) request;

		logHTTPHeaders(request);
		String applicationCredentialXmlEncoded = servletRequest.getHeader(APPLICATION_CREDENTIALS_HEADER_XML);
		if (applicationCredentialXmlEncoded == null || applicationCredentialXmlEncoded.length() < 10) {
			applicationCredentialXmlEncoded = servletRequest.getHeader(APPLICATION_CREDENTIALS_HEADER);
		}
		boolean isUas = false;
		//Enable tests to pass through.
		if (ApplicationMode.skipSecurityFilter()) {
			log.warn("Running in noSecurityFilter mode, non-authorized applications may access UIB!");
			isUas = true;
		}
		log.trace("Header appCred: {}", applicationCredentialXmlEncoded);
		if (applicationCredentialXmlEncoded != null && !applicationCredentialXmlEncoded.isEmpty()) {
			isUas = requestViaUas(applicationCredentialXmlEncoded);
			log.trace("Request via UAS {}", isUas);
			if (!isUas) {
				notifyFailedAttempt(servletRequest);
			}
		} else {
			String pathInfo = servletRequest.getPathInfo();
			if (!isHealthPath(pathInfo)) {
				notifyFailedAnonymousAttempt(servletRequest);
			}
		}


		String pathInfo = servletRequest.getPathInfo();

		if (isUas) {
			Integer statusCode = authenticateAndAuthorizeRequest(pathInfo);
			if (statusCode == null) {
				chain.doFilter(request, response);
			} else {
				notifyFailedAttempt(servletRequest);
				((HttpServletResponse) response).setStatus(statusCode);
			}
		} else {
			if (isHealthPath(pathInfo)) {
				chain.doFilter(request, response);
			} else {
				notifyFailedAnonymousAttempt(servletRequest);
				log.warn("FORBIDDEN b/c isUas={}, pathInfo={}", String.valueOf(isUas), pathInfo);
				((HttpServletResponse) response).setStatus(Response.SC_FORBIDDEN);
			}
		}
	}

	protected boolean isHealthPath(String pathInfo) {
		boolean isHealthPath = false;
		if (pathInfo != null) {
			String path = pathInfo.substring(1);
			if (path != null && path.startsWith(HEALH_PATH.replace("/", ""))) {
				isHealthPath = true;
			}
		}
		return isHealthPath;
	}

	public static String getClientIp(HttpServletRequest request) {


		String remoteAddr = "";

		if (request != null) {
			remoteAddr = request.getHeader("X-FORWARDED-FOR");
			if (remoteAddr == null || "".equals(remoteAddr)) {
				remoteAddr = request.getRemoteAddr();
			}
		}

		return remoteAddr;
	}

	protected void notifyFailedAnonymousAttempt(HttpServletRequest request) {
		try {
			if(SecurityTokenServiceClient.getSecurityTokenServiceClient().getWAS()!=null) {
				SecurityTokenServiceClient.getSecurityTokenServiceClient().getWAS().reportThreatSignal(getClientIp(request), WhydahUtil.getMyIPAddresssesString(), "notifyFailedAttempt - Failed intrusion detected. Header is missing");
			}
		} catch (Exception e) {
			log.warn("Trouble issuing a thread warning", e);
		}
		healthCheckService.addIntrusionAnonymous();
	}

	protected void notifyFailedAttempt(HttpServletRequest request) {
		try {
			if(SecurityTokenServiceClient.getSecurityTokenServiceClient().getWAS()!=null) {
				SecurityTokenServiceClient.getSecurityTokenServiceClient().getWAS().reportThreatSignal(getClientIp(request), WhydahUtil.getMyIPAddresssesString(), "notifyFailedAttempt - Failed intrusion detected. Header is missing");
			}
		} catch (Exception e) {
			log.warn("Trouble issuing a thread warning", e);
		}
		healthCheckService.addIntrusion();
	}

	/*
    Read the credentialXml, and validate this content towards the credentials stored in applicationdatabase.
	 */
	protected boolean requestViaUas(String applicationCredentialXml) {
		log.debug("Found applicationCredentialXml:{}", applicationCredentialXml);
		boolean isUAS = false;
		if (isCI) {
			isUAS = true;
		}
		if (applicationCredentialXml != null && !applicationCredentialXml.isEmpty()) {
			String realData = null;
			try {
				realData = java.net.URLDecoder.decode(applicationCredentialXml, "UTF-8");
				applicationCredentialXml = realData;
			} catch (UnsupportedEncodingException e) {
				// Nothing to do, it should not happen as you supplied a standard one
			}

			ApplicationCredential applicationCredential = ApplicationCredentialMapper.fromXml(applicationCredentialXml);
			log.debug("Found ApplicationCredential:{}", applicationCredential.toString());
			isUAS = authenticationService.isAuthenticatedAsUAS(applicationCredential);
		}
		return isUAS;
	}


	public static void setCIFlag(boolean flag) {
		isCI = flag;

	}

	@Override
	public void destroy() {
	}
}
