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

import javax.ws.rs.DELETE;
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

    // passwordSender.sendResetPasswordEmail(username, token, userEmail);

    /**
     * Any user can reset password using username or uid without logging in. Possible extension: reset using email.
     */
    //@POST
    //@Path("/password/{username}/reset")
    //@Path("/user/{uid}/password_reset")

    @DELETE
    @Path("/user/{uid}/password")
    public Response resetPassword(@PathParam("username") String username) {
        log.info("Reset password for usernameOrUid={}", username);
        try {
            UserIdentity user = userIdentityService.getUserIdentity(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }

            String changePasswordToken = userIdentityService.setTempPassword(username, user.getUid());
            //TODO: How should the response look like?
            Map<String, String> map = new HashMap<>();
            map.put(UserIdentity.UID, user.getUid());
            //map.put(UserIdentity.USERNAME, user.getUsername());
            map.put(CHANGE_PASSWORD_TOKEN, changePasswordToken);
            String json = objectMapper.writeValueAsString(map);
            // Uri to reset password makes sense.
            // link: rel=changePW, url= /user/uid123/password?token=124abcdhg

            return Response.ok().entity(json).build();
        } catch (Exception e) {
            log.error("resetPassword failed for username={}", username, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Change password using changePasswordToken.
     * @param username  to change password for
     * @param json  expected to contain changePasswordToken and newpassword
     * @return  201 No Content if successful
     */
    @POST
    //@Path("/password/{username}/change/{changePasswordToken}")
    @Path("/user/{uid}/password")
    public Response authenticateAndChangePasswordUsingToken(@PathParam("username") String username,
                                                            @QueryParam("changePasswordToken") String changePasswordToken, String json) {
        log.info("authenticateAndChangePasswordUsingToken for username={}", username);
        try {
            UserIdentity user = userIdentityService.getUserIdentity(username);
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
                log.info("authenticateAndChangePasswordUsingToken failed, weak password for username={}", username);
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }

            boolean authenticated;
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
