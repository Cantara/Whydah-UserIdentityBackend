package net.whydah.identity.application;

import net.whydah.identity.health.HealthCheckService;
import net.whydah.identity.health.HealthResource;
import net.whydah.identity.user.authentication.SecurityTokenServiceClient;
import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.commands.threat.CommandSendThreatSignal;
import net.whydah.sso.session.WhydahApplicationSession;
import net.whydah.sso.session.WhydahApplicationSession2;
import net.whydah.sso.util.WhydahUtil;
import net.whydah.sso.whydah.ThreatSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-11-21.
 */
@Component
@Path("/{applicationtokenId}/application/auth")
public class ApplicationAuthenticationEndpoint {
    static final String UAS_APP_CREDENTIAL_XML = "uasAppCredentialXml";
    static final String APP_CREDENTIAL_XML = "appCredentialXml";
    private static final Logger log = LoggerFactory.getLogger(ApplicationAuthenticationEndpoint.class);
    private final SecurityTokenServiceClient securityTokenHelper;
    private final HealthCheckService healthCheckService;
    private final ApplicationService authenticationService;

    @Autowired
    public ApplicationAuthenticationEndpoint(SecurityTokenServiceClient securityTokenHelper, ApplicationService authenticationService, HealthCheckService healthCheckService) {
        this.securityTokenHelper = securityTokenHelper;
        this.authenticationService = authenticationService;
        this.healthCheckService = healthCheckService;
    }


    /**
     * Authenticate UAS application using app credential
     * @param applicationtokenId not in use, expected to be useful in the future
     * @param uasAppCredentialXml   application credential for UAS, only UAS is allowed to communicate with UIB
     * @param authCredentialXml  application credential for application to authenticate
     * @return  204 No Content if successful, otherwise 401 Forbidden
     */
    @POST
    //@Path("/{stsApplicationtokenId}/application/auth")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    //@Produces(MediaType.TEXT_PLAIN)
    public Response authenticateApplication(@PathParam("applicationtokenId") String applicationtokenId,
                                            @FormParam(UAS_APP_CREDENTIAL_XML) String uasAppCredentialXml,
                                            @FormParam(APP_CREDENTIAL_XML) String authCredentialXml) {

        try {
            //verify uasAppCredentialXml for UAS
            ApplicationCredential uasAppCredential = ApplicationCredentialMapper.fromXml(uasAppCredentialXml);
            log.info("UAS ApplicationCredential lookup for applicationID:{}", uasAppCredential.getApplicationID());
            Application uasApplication = authenticationService.authenticate(uasAppCredential);
            if (uasApplication == null) {
                log.warn("UAS Application authentication failed for {}. Returning {}", uasAppCredential, Response.Status.FORBIDDEN);
                return Response.status(Response.Status.FORBIDDEN).build();
            }


            //verify appCredentialXml for UAS
            ApplicationCredential authAppCredential = ApplicationCredentialMapper.fromXml(authCredentialXml);
            log.info("ApplicationCredential lookup for applicationID:{}", authAppCredential.getApplicationID());
            Application authApplication = authenticationService.authenticate(authAppCredential);
            if (authApplication == null) {
                log.warn("Application authentication failed for {}. Returning {}", authAppCredential, Response.Status.FORBIDDEN);
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            log.info("Application Authentication ok for {}", authAppCredential);
            return Response.status(Response.Status.NO_CONTENT).build();

        } catch (Exception e) {
            //return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.status(Response.Status.FORBIDDEN).build();

    }

    protected void notifyFailedAttempt(String text) {
    	WhydahApplicationSession2 was = SecurityTokenServiceClient.getSecurityTokenServiceClient().getWAS();
    	if(was!=null) {
    		String tokenServiceUri = was.getSTS();
    		String myApplicationTokenID = SecurityTokenServiceClient.getSecurityTokenServiceClient().getActiveUibApplicationTokenId();
    		new CommandSendThreatSignal(URI.create(tokenServiceUri), myApplicationTokenID, createThreat(text)).queue();
    		healthCheckService.addIntrusion();
    	} else {
    		log.error("WhydahApplicationSession failed to establish for UIB");
    	}
    }

    private ThreatSignal createThreat(String text) {
        ThreatSignal threatSignal = new ThreatSignal();
        threatSignal.setSource("");
        threatSignal.setSignalEmitter(SecurityTokenServiceClient.getSecurityTokenServiceClient().getWAS().getActiveApplicationName() + " [ApplicationAuthenticationEndpoint:" + WhydahUtil.getMyIPAddresssesString() + "]");
        threatSignal.setAdditionalProperty("DEFCON", SecurityTokenServiceClient.getSecurityTokenServiceClient().getWAS().getDefcon());
        threatSignal.setAdditionalProperty("Version", HealthResource.getVersion());
        threatSignal.setInstant(Instant.now().toString());
        threatSignal.setText(text);
        return threatSignal;
    }

}
