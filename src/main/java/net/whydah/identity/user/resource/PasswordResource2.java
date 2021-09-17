package net.whydah.identity.user.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import net.whydah.identity.config.PasswordBlacklist;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.BCryptService;
import net.whydah.identity.user.identity.LDAPUserIdentity;
import net.whydah.identity.user.identity.RDBMSUserIdentity;
import net.whydah.identity.user.identity.UserIdentityServiceV2;
import net.whydah.identity.util.PasswordGenerator;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jax-RS resource responsible for user password management.
 * See also https://wiki.cantara.no/display/whydah/Password+management.
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-11-15.
 */
@Component
@Path("/{applicationtokenid}")
public class PasswordResource2 {
    static final String CHANGE_PASSWORD_TOKEN = "changePasswordToken";
    static final String NEW_PASSWORD_KEY = "newpassword";
    static final String EMAIL_KEY = "email";
    static final String CELLPHONE_KEY = "cellPhone";

    private static final Logger log = LoggerFactory.getLogger(PasswordResource2.class);
    public static final String PW_APPLICATION_ID = "2212";
    public static final String PW_APPLICATION_NAME = "Whydah-UserAdminService";
    public static final String PW_ORG_NAME = "Whydah";
    public static final String PW_ROLE_NAME = "PW_SET";
    public static final String PW_ROLE_VALUE = "true";
    public static final int MIN_PW_LENGTH = 8;
    private final UserIdentityServiceV2 userIdentityServiceV2;
    private final UserAggregateService userAggregateService;
    private final ObjectMapper objectMapper;
    private final BCryptService bCryptService;

    @Autowired
    public PasswordResource2(UserIdentityServiceV2 userIdentityServiceV2,
                             UserAggregateService userAggregateService, ObjectMapper objectMapper,
                             BCryptService bCryptService) {
        this.userIdentityServiceV2 = userIdentityServiceV2;
        this.userAggregateService = userAggregateService;

        this.objectMapper = objectMapper;
        this.bCryptService = bCryptService;
    }


    /**
     * Any user can reset password without logging in. UAS will support finduser for uid, username or email.
     *
     * @param uid unique user id
     * @return json with uid and change password token
     */
    @POST
    @Path("/user/{uid}/reset_password")
    public Response resetPassword(@PathParam("uid") String uid) {
        log.info("Reset password for uid={}", uid);

        RDBMSUserIdentity user = null;
        try {
            user = userIdentityServiceV2.getUserIdentityForUid(uid);
        } catch (Exception e) {
            log.warn("resetPassword user={} not found in DB", uid);
        }

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
        }

        String preGeneratedPassword = new PasswordGenerator().generate();
        String preGeneratedSaltPassword = new PasswordGenerator().generate();

        String changePasswordToken = null;
        try {
            changePasswordToken = userIdentityServiceV2.setTempPassword(user.getUsername(), user.getUid(),
                    preGeneratedPassword, preGeneratedSaltPassword);
        } catch (Exception e) {
            log.warn(String.format("Unable to set password in db for username=%s, uid=%s", user.getUsername(), user.getUid()), e);
        }

        if (changePasswordToken == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        Map<String, String> map = new HashMap<>();
        map.put(LDAPUserIdentity.UID, user.getUid());
        map.put(EMAIL_KEY, user.getEmail());
        map.put(CELLPHONE_KEY, user.getCellPhone());
        map.put(CHANGE_PASSWORD_TOKEN, changePasswordToken);

        String json = null;
        try {
            json = objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            json = "mapping error";
            log.error("Mapping error", e);
        }
        // ED: I think this information should be communicated with uri, but BLI does not agree, so keep it his way for now.
        // link: rel=changePW, url= /user/uid123/password?token=124abcdhg
        return Response.ok().entity(json).build();
    }


    /**
     * Change password using changePasswordToken.
     *
     * @param uid                 to change password for
     * @param changePasswordToken expected as queryParam
     * @param json                expected to contain newpassword
     * @return 201 No Content if successful
     */
    @POST
    @Path("/user/{uid}/change_password")
    public Response authenticateAndChangePasswordUsingToken(@PathParam("uid") String uid,
                                                            @QueryParam("changePasswordToken") String changePasswordToken, String json) {
        log.info("authenticateAndChangePasswordUsingToken for uid={}", uid);

        RDBMSUserIdentity user = null;
        try {
            user = userIdentityServiceV2.getUserIdentityForUid(uid);

        } catch (Exception e) {
            log.warn("authenticateAndChangePasswordUsingToken user={} not found in DB", uid);
        }

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
        }

        String newpassword;
        try {
            Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
            newpassword = JsonPath.read(document, NEW_PASSWORD_KEY);
        } catch (RuntimeException e) {
            log.info("authenticateAndChangePasswordUsingToken failed, bad json", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (PasswordBlacklist.pwList.contains(newpassword)) {
            log.info("authenticateAndChangePasswordUsingToken failed, weak password for username={}", uid);
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        if (newpassword == null || newpassword.length() < MIN_PW_LENGTH) {
            log.info("authenticateAndChangePasswordUsingToken failed, weak password for username={}", uid);
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        boolean authenticated = false;
        String username = user.getUsername();
        try {
            authenticated = userIdentityServiceV2.authenticateWithChangePasswordToken(username, changePasswordToken);
        } catch (Exception e) {
            log.warn("Authentication failed using changePasswordToken for username={} in LDAP", username);
        }

        if (!authenticated) {
            log.info("Authentication failed using changePasswordToken for username={}", username);
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            UserApplicationRoleEntry pwRole = new UserApplicationRoleEntry();
            pwRole.setApplicationId(PW_APPLICATION_ID);  //UAS
            pwRole.setApplicationName(PW_APPLICATION_NAME);
            pwRole.setOrgName(PW_ORG_NAME);
            pwRole.setRoleName(PW_ROLE_NAME);
            pwRole.setRoleValue(PW_ROLE_VALUE);

            UserApplicationRoleEntry updatedRole = userAggregateService.addUserApplicationRoleEntryIfNotExist(uid, pwRole);
        } catch (RuntimeException re) {
            log.info("changePasswordForUser-RuntimeException username={}, message={}", username, re.getMessage(), re);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            userIdentityServiceV2.changePassword(username, user.getUid(), newpassword);
            return Response.noContent().build();
        } catch (Exception e) {
            log.error("authenticateAndChangePasswordUsingToken failed.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * Any user can reset password without logging in. UAS will support finduser for uid, username or email.
     *
     * @param username unique user id
     * @return true/false
     */
    @GET
    @Path("/user/{username}/password_login_enabled")
    public Response hasUserNameSetPassword(@PathParam("username") String username) {
        log.info("password_login_enabled for uid={}", username);

        RDBMSUserIdentity user = null;
        try {
            user = userIdentityServiceV2.getUserIdentity(username);
        } catch (Exception e) {
            log.warn("password_login_enabled false for uid={} in LDAP", username);
        }

        if (user == null) {
            log.info("password_login_enabled false for uid={}", username);
            return Response.ok().entity(Boolean.toString(false)).build();
        }

        try {
            List<UserApplicationRoleEntry> roles = userAggregateService.getUserApplicationRoleEntries(user.getUid());
            for (UserApplicationRoleEntry role : roles) {
                log.info("Checking role: getApplicationId():{}, getApplicationName(){}, getApplicationRoleName(){}, getApplicationRoleValue(){} against 2212, UserAdminService, PW_SET, true ", role.getApplicationId(), role.getApplicationName(), role.getRoleName(), role.getRoleValue());
                if (role.getRoleName().equalsIgnoreCase(PW_ROLE_NAME)) {
                    if (role.getRoleValue().equalsIgnoreCase(PW_ROLE_VALUE)) {  // Found a true value
                        log.info("password_login_enabled true for uid={}", username);
                        return Response.ok().entity(Boolean.toString(true)).build();
                    }
                }
            }
            log.info("password_login_enabled false for uid={}", username);
            return Response.ok().entity(Boolean.toString(false)).build();
        } catch (Exception e) {
            log.error("hasUserNameSetPassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
