package net.whydah.identity.user.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.identity.config.PasswordBlacklist;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.BCryptService;
import net.whydah.identity.user.identity.RDBMSUserIdentity;
import net.whydah.identity.user.identity.UserIdentityServiceV2;
import net.whydah.identity.util.PasswordGenerator;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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

    private final UserIdentityServiceV2 userIdentityServiceV2;
    private final UserAggregateService userAggregateService;

    private final ObjectMapper objectMapper;
    private final BCryptService bCryptService;

    @Context
    private UriInfo uriInfo;


    @Autowired
    public PasswordResource(UserIdentityServiceV2 userIdentityServiceV2,
                            UserAggregateService userAggregateService, ObjectMapper objectMapper, BCryptService bCryptService) {
        this.userIdentityServiceV2 = userIdentityServiceV2;
        this.userAggregateService = userAggregateService;

        this.objectMapper = objectMapper;
        this.bCryptService = bCryptService;
        log.trace("Started: PasswordResource");
    }

    @Deprecated
    @GET
    @Path("/reset/username/{username}")
    public Response resetPassword(@PathParam("username") String username) {
        log.info("Reset password (GET) for username={}", username);

        RDBMSUserIdentity user = null;
        try {
            user = userIdentityServiceV2.getUserIdentity(username);
        } catch (Exception e) {
            log.warn(String.format("User=%s could not be found in DB", username), e);
        }

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
        }

        String preGeneratedPassword = new PasswordGenerator().generate();
        String preGeneratedSaltPassword = new PasswordGenerator().generate();

        String resetPasswordTokenV2 = userIdentityServiceV2.setTempPassword(username, user.getUid(), preGeneratedPassword, preGeneratedSaltPassword);

        try {
            Map<String, String> retVal = new HashMap<>();
            retVal.put("username", username);
            retVal.put("email", user.getEmail());
            retVal.put("cellPhone", user.getCellPhone());
            retVal.put("resetPasswordToken", resetPasswordTokenV2);
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
        RDBMSUserIdentity user = null;
        try {
            user = userIdentityServiceV2.getUserIdentity(username);
        } catch (Exception e) {
            log.warn(String.format("User=%s could not be found in DB", username), e);
        }

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
        }

        String preGeneratedPassword = new PasswordGenerator().generate();
        String preGeneratedSaltPassword = new PasswordGenerator().generate();

        String resetPasswordTokenV2 = userIdentityServiceV2.setTempPassword(username, user.getUid(), preGeneratedPassword, preGeneratedSaltPassword);

        try {
            Map<String, String> map = new HashMap<>();
            map.put(RDBMSUserIdentity.UID, user.getUid());
            map.put(EMAIL_KEY, user.getEmail());
            map.put(CELLPHONE_KEY, user.getCellPhone());
            map.put(CHANGE_PASSWORD_TOKEN, resetPasswordTokenV2);
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

        RDBMSUserIdentity user = null;
        try {
            user = userIdentityServiceV2.getUserIdentity(username);
        } catch (Exception e) {
            log.warn(String.format("User=%s could not be found in DB", username), e);
        }

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
        }

        String newpassword;
        try {
            JSONObject jsonobj = new JSONObject(passwordJson);
            newpassword = jsonobj.getString("newpassword");
            if (PasswordBlacklist.pwList.contains(newpassword)) {
                log.error("changePasswordForUser-Weak password for username={}", username);
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }
        } catch (JSONException e) {
            log.error("Bad json", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }


        boolean ok_DB = false;
        try {
            ok_DB = userIdentityServiceV2.authenticateWithChangePasswordToken(username, changePasswordTokenAsString);
            if (!ok_DB) {
                log.info("Authentication failed while changing password for username={} in DB", username);
            } else {
                userIdentityServiceV2.changePassword(username, user.getUid(), newpassword);
                UserApplicationRoleEntry pwRole = new UserApplicationRoleEntry();
                pwRole.setApplicationId(PasswordResource2.PW_APPLICATION_ID);  //UAS
                pwRole.setApplicationName(PasswordResource2.PW_APPLICATION_NAME);
                pwRole.setOrgName(PasswordResource2.PW_ORG_NAME);
                pwRole.setRoleName(PasswordResource2.PW_ROLE_NAME);
                pwRole.setRoleValue(PasswordResource2.PW_ROLE_VALUE);
                UserApplicationRoleEntry updatedRole = userAggregateService.addRoleIfNotExist(user.getUid(), pwRole);
            }
        } catch (Exception e) {
            log.error("changePasswordForUser-RuntimeException username={}, message={}", username, e.getMessage(), e);
            //return Response.status(Response.Status.BAD_REQUEST).build();
        }


        if (ok_DB) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                String userAsJson = mapper.writeValueAsString(user);
                //TODO Ensure password is not returned. Expect UserAdminService to trigger resetPassword.
                return Response.status(Response.Status.OK).entity(userAsJson).build();
            } catch (Exception e) {
                log.error("Mapping failed", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Mapping issues").build();
            }
        } else {
            log.error("newpassword failed. DB update={}", ok_DB);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/change/{adminUserTokenId}/user/username/{username}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response changePasswordbyAdmin(@PathParam("applicationtokenid") String applicationtokenid, @PathParam("adminUserTokenId") String adminUserTokenId,
                                          @PathParam("username") String username, String password) {
        log.info("Admin Changing password for {}", username);

        // TODO KCG: this should be looked at after we now have removed ldap
        //FIXME baardl: implement verification that admin is allowed to update this password.
        //Find the admin user token, based on tokenid
        /*
        if (!userIdentityService.allowedToUpdate(applicationtokenid, adminUserTokenId)) {
            String adminUserName = userIdentityService.findUserByTokenId(adminUserTokenId);
            log.info("Not allowed to update password. adminUser {}, user to update {}", adminUserName, username);
            return Response.status(Response.Status.FORBIDDEN).build();
        }
       */

        RDBMSUserIdentity user = null;
        try {
            user = userIdentityServiceV2.getUserIdentity(username);
        } catch (Exception e) {
            log.warn(String.format("User=%s could not be found in DB", username), e);
        }

        if (user == null) {
            log.trace("No user found for username {}, can not update password.", username);
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }

        log.debug("Found user: {}", user);

        try {
            userIdentityServiceV2.changePassword(username, user.getUid(), password);
            return Response.ok().build();
        } catch (Exception e) {
            log.error("changePasswordForUser failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
