package net.whydah.identity.application.authentication;

import net.whydah.sso.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by baardl on 22.03.14.
 * This not a RESTful endpoint. This is a http RPC endpoint.
 *
 * NOT IN USE YET!
 */
//@Path("/authenticate/application")
public class ApplicationAuthenticationEndpoint {
    private final static Logger log = LoggerFactory.getLogger(ApplicationAuthenticationEndpoint.class);

    //TODO baardl preparing for Application Authorization.
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response authenticateApplication(@QueryParam("appCredentialXml") String appCredentialXml){
        log.trace("authenticateApplication {}", appCredentialXml);
        //FIXME real implementation of ApplicationAuthenticationEndpoint.

        Application mockApplication = new Application("9999", "applicationNameMock");
        //String applicationXml = mockApplication.toXML();
        //String applicationXml = "";
        //log.debug("Application authentication ok. XML: {}", applicationXml);
        //return Response.status(Response.Status.OK).entity(applicationXml).build();
        /*
        ApplicationToken applicationToken = new ApplicationToken(new MockApplicationCredential().toXML());
        log.warn("Accessing insecure mock. Application Validation is Ommited!");
        return Response.status(Response.Status.OK).entity(applicationToken.toXML()).build();
        */
        return null;
    }

    //@Path("/{applicationtokenid}/{userTokenId}/")
    /*
    @POST
    @Path("/verifyApplicationAuth")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response verifyApplicationAuth(String applicationCredential) {
        log.trace("verifyApplicationAuth is called ");

        //FIXME check applicationSecret against applicationID
        return Response.ok().build();
    }
    */
}
