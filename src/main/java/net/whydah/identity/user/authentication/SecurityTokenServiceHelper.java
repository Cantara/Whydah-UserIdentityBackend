package net.whydah.identity.user.authentication;

import net.whydah.identity.user.UserRole;
import org.glassfish.jersey.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

public class SecurityTokenServiceHelper {
    private static final Logger log = LoggerFactory.getLogger(SecurityTokenServiceHelper.class);
    private Client client = ClientBuilder.newClient();

    private final WebTarget tokenServiceResource;
    private String myAppTokenXML;
    //private String myAppTokenId;

    public SecurityTokenServiceHelper(String usertokenserviceUri) {
        tokenServiceResource = client.target(usertokenserviceUri);
    }

    public UserToken getUserToken(String appTokenId, String usertokenid) {
        log.debug("usertokenid={}", usertokenid);
        log.debug("myAppTokenXML={}", myAppTokenXML);
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("usertokenid", usertokenid);
        formData.add("apptoken", myAppTokenXML);    //TODO myAppTokenXML is never set...
        try {
            WebTarget usertokenTarget = tokenServiceResource.path("user").path(appTokenId).path("get_usertoken_by_usertokenid");
            //MediaType.APPLICATION_FORM_URLENCODED_TYPE
            ClientResponse response = usertokenTarget.request().post(Entity.form(formData), ClientResponse.class);
            log.info("Accessing:" + "tokenservice/user/" + appTokenId + "/get_usertoken_by_usertokenid");
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                //String usertoken = response.getEntity(String.class);
                String usertoken = response.readEntity(String.class);
                log.debug("usertoken: {}", usertoken);
                return new UserToken(usertoken);
            }
            log.warn("User token NOT ok: {} {}", response.getStatus(), response.toString());
            List<UserRole> roles = new ArrayList<>();
            roles.add(new UserRole("9999", "99999", "mockrole"));
            return new UserToken("MockUserToken", roles);
        } catch (Exception che) {  //was ClientHandlerException when using Jersey 1.6
            if (che.getCause() instanceof java.net.ConnectException) {
                log.error("Could not connect to SecurityTokenService to verify applicationTokenId {}, userTokenId {}", appTokenId, usertokenid);
                return null;
            } else {
                throw che;
            }
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
