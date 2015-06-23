package net.whydah.identity.user.resource;

import net.whydah.identity.config.PasswordBlacklist;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.identity.UserIdentityService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
@Component
@Path("/password/{applciationtokenid}")
public class PasswordResource {
    private static final Logger log = LoggerFactory.getLogger(PasswordResource.class);

    private final UserIdentityService userIdentityService;

    @Context
    private UriInfo uriInfo;


    @Autowired
    public PasswordResource(UserIdentityService userIdentityService) {
        this.userIdentityService = userIdentityService;
        log.info("Started: PasswordResource");
    }

    @GET
    @Path("/reset/username/{username}")
    public Response resetPassword(@PathParam("username") String username) {
        log.info("Reset password (GET) for username={}", username);
        try {
            UserIdentity user = userIdentityService.getUserIdentity(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }

            userIdentityService.resetPassword(username, user.getUid(), user.getEmail());
            return Response.ok().build();
        } catch (Exception e) {
            log.error("resetPassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/reset/username/{username}")
    public Response resetPasswordPOST(@PathParam("username") String username) {
        log.info("Reset password (POST) for username={}", username);
        try {
            UserIdentity user = userIdentityService.getUserIdentity(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }

            userIdentityService.resetPassword(username, user.getUid(), user.getEmail());
            return Response.ok().build();
        } catch (Exception e) {
            log.error("resetPassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/reset/username/{username}/newpassword/{token}")
    public Response setPassword(@PathParam("username") String username, @PathParam("token") String changePasswordTokenAsString, String passwordJson) {
        log.info("Set new password for username={}, changePasswordTokenAsString={}", username, changePasswordTokenAsString);
        try {
            UserIdentity user = userIdentityService.getUserIdentity(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }
            boolean ok;
            try {
                ok = userIdentityService.authenticateWithChangePasswordToken(username, changePasswordTokenAsString);
            } catch (RuntimeException re) {
                log.error("changePasswordForUser-RuntimeException username={}, message={}", username, re.getMessage(), re);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            if (!ok) {
                log.info("Authentication failed while changing password for username={}", username);
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            try {
                JSONObject jsonobj = new JSONObject(passwordJson);
                String newpassword = jsonobj.getString("newpassword");
                //if (AppConfig.pwList.contains(newpassword)) {
                if (PasswordBlacklist.pwList.contains(newpassword)) {
                    log.error("changePasswordForUser-Weak password for username={}", username);
                    return Response.status(Response.Status.NOT_ACCEPTABLE).build();

                }
                userIdentityService.changePassword(username, user.getUid(), newpassword);
            } catch (JSONException e) {
                log.error("Bad json", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            return Response.ok().build();
        } catch (Exception e) {
            log.error("newpassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/change/{adminUserTokenId}/user/username/{username}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePasswordbyAdmin(@PathParam("applicationtokenid") String applicationtokenid, @PathParam("adminUserTokenId") String adminUserTokenId,
                                          @PathParam("username") String username, String password) {
        log.info("Admin Changing password for {}", username);
        //FIXME baardl: implement verification that admin is allowed to update this password.
        //Find the admin user token, based on tokenid
        if (!userIdentityService.allowedToUpdate(applicationtokenid, adminUserTokenId)) {
            String adminUserName = userIdentityService.findUserByTokenId(adminUserTokenId);
            log.info("Not allowed to update password. adminUser {}, user to update {}", adminUserName, username);
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            UserIdentity user = userIdentityService.getUserIdentity(username);

            if (user == null) {
                log.trace("No user found for username {}, can not update password.", username);
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }
            log.debug("Found user: {}", user.toString());

            userIdentityService.changePassword(username, user.getUid(), password);
            return Response.ok().build();
        } catch (Exception e) {
            log.error("changePasswordForUser failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
