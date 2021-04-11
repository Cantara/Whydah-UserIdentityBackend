package net.whydah.identity.user.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.identity.config.PasswordBlacklist;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.LDAPUserIdentity;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.identity.UserIdentityServiceV2;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
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
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
@Component
@Path("/password/{applicationtokenid}")
public class PasswordResource {
    private static final Logger log = LoggerFactory.getLogger(PasswordResource.class);
    static final String CHANGE_PASSWORD_TOKEN = "changePasswordToken";
    static final String NEW_PASSWORD_KEY = "newpassword";
    static final String EMAIL_KEY = "email";
    static final String CELLPHONE_KEY = "cellPhone";

    private final UserIdentityService userIdentityService;
    private final UserIdentityServiceV2 userIdentityServiceV2;
    private final UserAggregateService userAggregateService;

    private final ObjectMapper objectMapper;

    @Context
    private UriInfo uriInfo;


    @Autowired
    public PasswordResource(UserIdentityService userIdentityService, UserIdentityServiceV2 userIdentityServiceV2, UserAggregateService userAggregateService, ObjectMapper objectMapper) {
        this.userIdentityService = userIdentityService;
        this.userIdentityServiceV2 = userIdentityServiceV2;
        this.userAggregateService = userAggregateService;

        this.objectMapper = objectMapper;
        log.trace("Started: PasswordResource");
    }

    @Deprecated
    @GET
    @Path("/reset/username/{username}")
    public Response resetPassword(@PathParam("username") String username) {
        log.info("Reset password (GET) for username={}", username);
        try {
            LDAPUserIdentity user = userIdentityService.getUserIdentity(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }

            String resetPasswordToken = userIdentityService.setTempPassword(username, user.getUid());
            Map<String,String> retVal = new HashMap<>();
            retVal.put("username", username);
            retVal.put("email", user.getEmail());
            retVal.put("cellPhone", user.getCellPhone());
            retVal.put("resetPasswordToken", resetPasswordToken);
            String retValJson = objectMapper.writeValueAsString(retVal);
            return Response.ok().entity(retValJson).build();
        } catch (Exception e) {
            log.error("resetPassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Deprecated
    @POST
    @Path("/reset/username/{username}")
    public Response resetPasswordPOST(@PathParam("username") String username) {
        log.info("Reset password (POST) for username={}", username);
        try {
            LDAPUserIdentity user = userIdentityService.getUserIdentity(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }

            String resetPasswordToken = userIdentityService.setTempPassword(username, user.getUid());
            Map<String, String> map = new HashMap<>();
            map.put(LDAPUserIdentity.UID, user.getUid());
            map.put(EMAIL_KEY, user.getEmail());
            map.put(CELLPHONE_KEY, user.getCellPhone());
            map.put(CHANGE_PASSWORD_TOKEN, resetPasswordToken);
            String json = objectMapper.writeValueAsString(map);

            return Response.ok().entity(json).build();
        } catch (Exception e) {
            log.error("resetPassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Deprecated
    @POST
    @Path("/reset/username/{username}/newpassword/{token}")
    public Response setPassword(@PathParam("username") String username, @PathParam("token") String changePasswordTokenAsString, String passwordJson) {
        log.info("Set new password for username={}, changePasswordTokenAsString={}", username, changePasswordTokenAsString);
        try {
            LDAPUserIdentity user = userIdentityService.getUserIdentity(username);
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
                UserApplicationRoleEntry pwRole = new UserApplicationRoleEntry();
                pwRole.setApplicationId(PasswordResource2.PW_APPLICATION_ID);  //UAS
                pwRole.setApplicationName(PasswordResource2.PW_APPLICATION_NAME);
                pwRole.setOrgName(PasswordResource2.PW_ORG_NAME);
                pwRole.setRoleName(PasswordResource2.PW_ROLE_NAME);
                pwRole.setRoleValue(PasswordResource2.PW_ROLE_VALUE);

                UserApplicationRoleEntry updatedRole = userAggregateService.addRoleIfNotExist(user.getUid(), pwRole);

                
                ObjectMapper mapper = new ObjectMapper();
                String userAsJson = mapper.writeValueAsString(user);
                //TODO Ensure password is not returned. Expect UserAdminService to trigger resetPassword.
                return Response.status(Response.Status.OK).entity(userAsJson).build();

                
            } catch (JSONException e) {
                log.error("Bad json", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

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
            LDAPUserIdentity user = userIdentityService.getUserIdentity(username);

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
