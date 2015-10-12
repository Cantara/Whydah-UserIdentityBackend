package net.whydah.identity.user.authentication;

import net.whydah.identity.user.UserRole;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUsertokenId;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUsertokenIdWithStubbedFallback;
import net.whydah.sso.commands.userauth.CommandLogonUserByUserCredentialWithStubbedFallback;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class SecurityTokenServiceHelper {
    private static final Logger log = LoggerFactory.getLogger(SecurityTokenServiceHelper.class);
    private Client client = ClientBuilder.newClient();

    private final WebTarget tokenServiceResource;
    private String myAppTokenXML;
    //private String myAppTokenId;

    @Autowired
    @Configure
    public SecurityTokenServiceHelper(@Configuration("securitytokenservice") String usertokenserviceUri) {
        tokenServiceResource = client.target(usertokenserviceUri);
    }

    public UserToken getUserToken(String appTokenId, String usertokenid){
        String userToken = new CommandGetUsertokenByUsertokenId(tokenServiceResource.getUri(), appTokenId, myAppTokenXML, usertokenid).execute();
        if (userToken!=null && userToken.length()>10) {
            log.debug("usertoken: {}", userToken);
            return new UserToken(userToken);
        }
        log.error("getUserToken failed");
        return null;
    }

    public UserToken getUserToken2(String appTokenId, String usertokenid) {
        //log.debug("usertokenid={}", usertokenid);
        //log.debug("myAppTokenXML={}", myAppTokenXML);
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("usertokenid", usertokenid);
        formData.add("apptoken", myAppTokenXML);    //TODO myAppTokenXML is never set...

        try {
            WebTarget usertokenTarget = tokenServiceResource.path("user").path(appTokenId).path("get_usertoken_by_usertokenid");
            log.info("getUserToken from STS: {}, usertokenid={}, myAppTokenXML={}", usertokenTarget.getUri().toString(), usertokenid, myAppTokenXML);

            Response response = usertokenTarget.request().post(Entity.form(formData), Response.class);
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                String usertoken = response.readEntity(String.class);
                log.debug("usertoken: {}", usertoken);
                return new UserToken(usertoken);
            }
            log.warn("User token NOT ok: {} {}", response.getStatus(), response.toString());
            List<UserRole> roles = new ArrayList<>();
            roles.add(new UserRole("9999", "99999", "mockrole"));
            return new UserToken("MockUserToken", roles);
        } catch (Exception e) {  //was ClientHandlerException when using Jersey 1.6
            if (e.getCause() instanceof java.net.ConnectException) {
                log.error("Could not connect to SecurityTokenService to verify applicationTokenId {}, userTokenId {}", appTokenId, usertokenid);
            } else {
                log.error("getUserToken failed", e);
            }
            return null;
            /*
            if (e.getCause() instanceof java.net.ConnectException) {
                log.error("Could not connect to SecurityTokenService to verify applicationTokenId {}, userTokenId {}", appTokenId, usertokenid);
                return null;
            } else {
                throw e;
            }
            */
        }
    }
    /*
    private String getAppTokenId() {
        if (myAppTokenId != null) {
            ClientResponse response = tokenServiceResource.path(myAppTokenId + "/validate").get(ClientResponse.class);
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                log.trace("Previous applicationtoken is ok");
                return myAppTokenId;
            }
        }
        log.warn("Previous applicationtoken  invalid - Must re-authenticate myself");
        authenticateMyself();
        return myAppTokenId;
    }


    private synchronized void authenticateMyself() {
        String auth = "<applicationcredential><params><applicationID>1</applicationID><applicationSecret>secret</applicationSecret></params></applicationcredential>";
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("applicationcredential", auth);
        String apptoken = tokenServiceResource.path("logon").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(String.class, formData);
        myAppTokenXML = apptoken;
        log.info("apptoken={}", apptoken);
        myAppTokenId = getApplicationTokenIdFromAppToken(apptoken);
    }


    // TODO  change parsing to xpath parsing
    private String getApplicationTokenIdFromAppToken(String appTokenXML) {
        return appTokenXML.substring(appTokenXML.indexOf("<applicationtoken>") + "<applicationtoken>".length(), appTokenXML.indexOf("</applicationtoken>"));
    }
    */
}
