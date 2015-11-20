package net.whydah.identity.user.password;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import net.whydah.identity.config.PasswordBlacklist;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.identity.UserIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Jax-RS resource responsible for user password management.
 * See also https://wiki.cantara.no/display/whydah/Password+management.
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-11-15.
 */
@Component
@Path("/{applicationtokenid}")
public class PasswordResource2 {
    static final String CHANGE_PASSWORD_TOKEN = "changePasswordToken";
    static final String NEW_PASSWORD_KEY = "newpassword";

    private static final Logger log = LoggerFactory.getLogger(PasswordResource2.class);
    private final UserIdentityService userIdentityService;
    private final ObjectMapper objectMapper;

    @Autowired
    public PasswordResource2(UserIdentityService userIdentityService, ObjectMapper objectMapper) {
        this.userIdentityService = userIdentityService;
        this.objectMapper = objectMapper;
    }


    /**
     * Any user can reset password without logging in. UAS will support finduser for uid, username or email.
     * @param uid   unique user id
     * @return  json with uid and change password token
     */
    @POST
    @Path("/user/{uid}/reset_password")
    public Response resetPassword(@PathParam("uid") String uid) {
        log.info("Reset password for uid={}", uid);
        try {
            UserIdentity user = userIdentityService.getUserIdentityForUid(uid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }

            String changePasswordToken = userIdentityService.setTempPassword(uid, user.getUid());
            Map<String, String> map = new HashMap<>();
            map.put(UserIdentity.UID, user.getUid());
            map.put(CHANGE_PASSWORD_TOKEN, changePasswordToken);
            String json = objectMapper.writeValueAsString(map);
            // ED: I think this information should be communicated with uri, but BLI does not agree, so keep it his way for now.
            // link: rel=changePW, url= /user/uid123/password?token=124abcdhg

            return Response.ok().entity(json).build();
        } catch (Exception e) {
            log.error("resetPassword failed for uid={}", uid, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * Change password using changePasswordToken.
     * @param uid  to change password for
     * @param changePasswordToken   expected as queryParam
     * @param json  expected to contain newpassword
     * @return  201 No Content if successful
     */
    @POST
    @Path("/user/{uid}/change_password")
    public Response authenticateAndChangePasswordUsingToken(@PathParam("uid") String uid,
                                                            @QueryParam("changePasswordToken") String changePasswordToken, String json) {
        log.info("authenticateAndChangePasswordUsingToken for uid={}", uid);
        try {
            UserIdentity user = userIdentityService.getUserIdentityForUid(uid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }

            String newpassword;
            try {
                Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
                newpassword =  JsonPath.read(document, NEW_PASSWORD_KEY);
            } catch (RuntimeException e) {
                log.info("authenticateAndChangePasswordUsingToken failed, bad json", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (PasswordBlacklist.pwList.contains(newpassword)) {
                log.info("authenticateAndChangePasswordUsingToken failed, weak password for username={}", uid);
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }

            boolean authenticated;
            String username = user.getUsername();
            try {
                authenticated = userIdentityService.authenticateWithChangePasswordToken(username, changePasswordToken);
            } catch (RuntimeException re) {
                log.info("changePasswordForUser-RuntimeException username={}, message={}", username, re.getMessage(), re);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (!authenticated) {
                log.info("Authentication failed using changePasswordToken for username={}", username);
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            userIdentityService.changePassword(username, user.getUid(), newpassword);
            return Response.noContent().build();
        } catch (Exception e) {
            log.error("authenticateAndChangePasswordUsingToken failed.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
