package net.whydah.identity.health;

import net.whydah.identity.user.authentication.SecurityTokenServiceClient;
import net.whydah.sso.session.WhydahApplicationSession;
import net.whydah.sso.session.WhydahApplicationSession2;
import net.whydah.sso.util.WhydahUtil;
import org.apache.lucene.util.Version;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Properties;

/**
 * Endpoint for health check.
 */
@Component
@Path("/health")
public class HealthResource {
    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);
    private final HealthCheckService healthCheckService;
    private static SecurityTokenServiceClient securityTokenServiceClient;
    private static String applicationInstanceName;
    private static boolean ok_db = true;
    private static int numberOfUsers_DB = 0;
    private static long numberOfApplications = 0;

    @Autowired
    @Configure
    public HealthResource(SecurityTokenServiceClient securityTokenHelper, HealthCheckService healthCheckService, @Configuration("applicationname") String applicationname) {
        this.securityTokenServiceClient = securityTokenHelper;
        this.healthCheckService = healthCheckService;
        this.applicationInstanceName = applicationname;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response isHealthy() {
        ok_db = healthCheckService.isOK_DB();
        try {
        	if(securityTokenServiceClient.getWAS()==null) {//unhealthy
        		log.debug("isHealthy={}, {}", false, "App session failed to establish for UIB. securityTokenServiceClient.getWAS() = null");
        		return Response.ok(getSimpleTextJson()).build();
        	}
            String statusText = getPrintableStatus(securityTokenServiceClient.getWAS());
            log.debug("isHealthy={}, {}", ok_db, statusText);
            if (ok_db) {
                //return Response.status(Response.Status.NO_CONTENT).build();
                return Response.ok(getHealthTextJson()).build();
            } else {
                //Intentionally not returning anything the client can use to determine what's the error for security reasons.
                return Response.ok(getSimpleTextJson()).build();
            }
        } catch (Exception e) {
            return Response.ok(getSimpleTextJson()).build();

        }
    }
    
    public static String getPrintableStatus(WhydahApplicationSession2 was) {

        String statusString = "Whydah session:\n" +
                " DEFCON: " + was.getDefcon() + "\"\n" +
                " - STS: " + was.getSTS() + "\"\n" +
                " - UAS: " + was.getUAS() + "\"\n" +
                " - running since: " + WhydahUtil.getRunningSince() + "\"\n" +
                " - hasApplicationToken: " + Boolean.toString(was.getActiveApplicationTokenId() != null) + "\n" +
                " - hasValidApplicationToken: " + Boolean.toString(was.hasActiveSession()) + "\n" +
                " - hasApplicationsMetadata:" + Boolean.toString(was.getApplicationList().size() > 2) + "\n";
        return statusString;

    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("intrusions")
    public Response countIntrusions() {
        long intrusions = healthCheckService.countIntrusionAttempts();
        long anonymousIntrusions = healthCheckService.countAnonymousIntrusionAttempts();

        return Response.ok("{\"intrusionAttempt\":" + intrusions + ",\"anonymousIntrusionAttempt\":" + anonymousIntrusions + "}").build();

    }

    public String getHealthTextJson() {
        if (SecurityTokenServiceClient.getSecurityTokenServiceClient().getWAS() != null) {
            return "{\n" +
                    "  \"Status\": \"" + ok_db + "\",\n" +
                    "  \"Version\": \"" + getVersion() + "\",\n" +
                    "  \"DEFCON\": \"" + SecurityTokenServiceClient.getSecurityTokenServiceClient().getWAS().getDefcon() + "\",\n" +
                    "  \"STS\": \"" + SecurityTokenServiceClient.getSecurityTokenServiceClient().getWAS().getSTS() + "\",\n" +
                    "  \"hasApplicationToken\": \"" + Boolean.toString(SecurityTokenServiceClient.getSecurityTokenServiceClient().getWAS().getActiveApplicationTokenId() != null) + "\",\n" +
                    // "  \"hasValidApplicationToken\": \"" + Boolean.toString(SecurityTokenServiceClient.getSecurityTokenServiceClient().getWAS().checkActiveSession()) + "\",\n" +
                    "  \"users (DB)\": \"" + numberOfUsers_DB + "\",\n" +
                    "  \"applications\": \"" + numberOfApplications + "\",\n" +
                    "  \"now\": \"" + Instant.now() + "\",\n" +
                    "  \"running since\": \"" + WhydahUtil.getRunningSince() + "\",\n\n" +
                    "  \"intrusionAttemptsDetected\": " + healthCheckService.countIntrusionAttempts() + ",\n" +
                    "  \"anonymousIntrusionAttemptsDetected\": " + healthCheckService.countAnonymousIntrusionAttempts() + ",\n" +
                    "  \"lucene version\": \"" + getLuceneVersion() + "\"\n" +
                    "}\n";

        }  // Else, return uninitialized was result
        return "{\n" +
                "  \"Status\": \"" + ok_db + "\",\n" +
                "  \"Version\": \"" + getVersion() + "\",\n" +
                "  \"DEFCON\": \"" + "N/A" + "\",\n" +
                "  \"STS\": \"" + "N/A" + "\",\n" +
                "  \"hasApplicationToken\": \"" + "false" + "\",\n" +
                "  \"hasValidApplicationToken\": \"" + "false" + "\",\n" +
                "  \"users (DB)\": \"" + numberOfUsers_DB + "\",\n" +
                "  \"applications\": \"" + numberOfApplications + "\",\n" +
                "  \"now\": \"" + Instant.now() + "\",\n" +
                "  \"running since\": \"" + WhydahUtil.getRunningSince() + "\",\n\n" +
                "  \"intrusionAttemptsDetected\": " + healthCheckService.countIntrusionAttempts() + ",\n" +
                "  \"anonymousIntrusionAttemptsDetected\": " + healthCheckService.countAnonymousIntrusionAttempts() + ",\n" +
                "  \"lucene version\": \"" + getLuceneVersion() + "\"\n" +
                "}\n";
    }

    public String getSimpleTextJson() {
        return "{\n" +
                "  \"Status\": \"" + ok_db + "\",\n" +
                "  \"users (DB)\": \"" + numberOfUsers_DB + "\",\n" +
                "  \"Version\": \"" + getVersion() + "\",\n" +
                "  \"now\": \"" + Instant.now() + "\",\n" +
                "  \"running since\": \"" + WhydahUtil.getRunningSince() + "\"\n" +
                "}\n";
    }

    public static String getVersion() {
        Properties mavenProperties = new Properties();
        String resourcePath = "/META-INF/maven/net.whydah.identity/UserIdentityBackend/pom.properties";
        URL mavenVersionResource = HealthResource.class.getResource(resourcePath);
        if (mavenVersionResource != null) {
            try {
                mavenProperties.load(mavenVersionResource.openStream());
                return mavenProperties.getProperty("version", "missing version info in " + resourcePath) + " [" + applicationInstanceName + " - " + WhydahUtil.getMyIPAddresssesString() + "]";
            } catch (IOException e) {
                log.warn("Problem reading version resource from classpath: ", e);
            }
        }
        return "(DEV VERSION)" + " [" + applicationInstanceName + " - " + WhydahUtil.getMyIPAddresssesString() + "]";
    }

    public static void setNumberOfUsersDB(int numberOfUsers) {
        HealthResource.numberOfUsers_DB = numberOfUsers;
    }
    
    public static int getNumberOfUsersDB() {
        return HealthResource.numberOfUsers_DB;
    }

    public static long getNumberOfApplications() {
        return numberOfApplications;
    }

    public static void setNumberOfApplications(long numberOfApplications) {
        HealthResource.numberOfApplications = numberOfApplications;
    }


    private static final String getLuceneVersion() {
        return Version.LATEST.toString();
    }
}

