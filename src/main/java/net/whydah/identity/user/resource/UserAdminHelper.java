package net.whydah.identity.user.resource;

import com.google.inject.Inject;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogRepository;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.security.Authentication;
import net.whydah.identity.user.authentication.UserToken;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import net.whydah.identity.user.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Helper class to avoid code duplication between UserResource and UserAuthenticationEndpoint.
 *
 * @author <a href="mailto:erik.drolshammer@altran.com">Erik Drolshammer</a>
 * @since 10/4/12
 */
public class UserAdminHelper {

    private static final Logger logger = LoggerFactory.getLogger(UserAdminHelper.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    private final LdapUserIdentityDao ldapUserIdentityDao;
    private final Indexer indexer;
    private final AuditLogRepository auditLogRepository;
    private final UserPropertyAndRoleRepository roleRepository;

    @Inject
    public UserAdminHelper(LdapUserIdentityDao ldapUserIdentityDao, Indexer indexer, AuditLogRepository auditLogRepository, UserPropertyAndRoleRepository roleRepository) {
        this.ldapUserIdentityDao = ldapUserIdentityDao;
        this.indexer = indexer;
        this.auditLogRepository = auditLogRepository;
        this.roleRepository = roleRepository;
    }

    public Response addUser(UserIdentity newIdentity) {
        String username = newIdentity.getUsername();
        logger.trace("Adding new user: {}", username);

        try {
            if (ldapUserIdentityDao.usernameExist(username)) {
                logger.info("User already exists, could not create user " + username);
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }

            newIdentity.setUid(UUID.randomUUID().toString());
            ldapUserIdentityDao.addUserIdentity(newIdentity);
            logger.info("Added new user: {}", username);
        } catch (Exception e) {
            logger.error("Could not create user " + username, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        addDefaultWhydahUserRole(newIdentity);

        try {
            indexer.addToIndex(newIdentity);
            audit(ActionPerformed.ADDED, "user", newIdentity.toString());
        } catch (Exception e) {
            logger.error("Error with lucene indexing or audit loggin for " + username, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
    }

    public static UserIdentity createWhydahUserIdentity(Document fbUserDoc) {
        XPath xPath = XPathFactory.newInstance().newXPath();

        try {
            String fbUserId = (String) xPath.evaluate("//userId", fbUserDoc, XPathConstants.STRING);
            String firstName = (String) xPath.evaluate("//firstName", fbUserDoc, XPathConstants.STRING);
            String lastName = (String) xPath.evaluate("//lastName", fbUserDoc, XPathConstants.STRING);
            String username = (String) xPath.evaluate("//username", fbUserDoc, XPathConstants.STRING);
            String email = (String) xPath.evaluate("//email", fbUserDoc, XPathConstants.STRING);
            logger.debug("From fbuserXml, fbUserId=" + fbUserId + ", firstName=" + firstName + ", lastName=" + lastName);

            UserIdentity userIdentity = new UserIdentity();
            userIdentity.setUid(fbUserId.trim());
            userIdentity.setUsername(username.trim());
            userIdentity.setFirstName(firstName.trim());
            userIdentity.setLastName(lastName.trim());
            userIdentity.setEmail(email.trim());

            String password = calculateFacebookPassword(fbUserId);
            userIdentity.setPassword(password);
            return userIdentity;
        } catch (XPathExpressionException e) {
            logger.error("", e);
            return null;
        }
    }

    public static String calculateFacebookPassword(String fbId) {
        return fbId + fbId;
    }

    public static String calculateNetIQPassword(String netIQAccessToken) {
        return (netIQAccessToken + netIQAccessToken);
    }


    public void addDefaultWhydahUserRole(UserIdentity userIdentity) {
        UserPropertyAndRole role = new UserPropertyAndRole();

        String applicationId = AppConfig.appConfig.getProperty("adduser.defaultapplication.id");
        String applicationName = AppConfig.appConfig.getProperty("adduser.defaultapplication.name");
        String organizationName = AppConfig.appConfig.getProperty("adduser.defaultorganization.name");
        String roleName = AppConfig.appConfig.getProperty("adduser.defaultrole.name");
        String roleValue = AppConfig.appConfig.getProperty("adduser.defaultrole.value");

        role.setUid(userIdentity.getUid());
        role.setApplicationId(applicationId);
        role.setApplicationName(applicationName);
        role.setOrganizationName(organizationName);
        role.setApplicationRoleName(roleName);
//        role.setRoleValue(roleValue);
        role.setApplicationRoleValue(userIdentity.getEmail());  // Provide NetIQ identity as rolevalue
        logger.debug("Adding Role: {}", role);

        if (roleRepository.hasRole(userIdentity.getUid(), role)) {
            logger.warn("Role already exist. " + role.toString());
            return;
        }

        roleRepository.addUserPropertyAndRole(role);
        String value = "uid=" + userIdentity + ", username=" + userIdentity.getUsername() + ", appid=" + role.getApplicationId() + ", role=" + role.getApplicationRoleName();
        audit(ActionPerformed.ADDED, "role", value);
    }

    public void addDefaultRoles(UserIdentity userIdentity, String roleValue) {
        boolean facebook = true;
        boolean netiq = false;
        if (roleValue.indexOf("netIQAccessToken") > 0) {
            facebook = false;
            netiq = true;
        }
        UserPropertyAndRole role = new UserPropertyAndRole();

        String applicationId = AppConfig.appConfig.getProperty("adduser.defaultapplication.id");
        String applicationName = AppConfig.appConfig.getProperty("adduser.defaultapplication.name");
        String organizationName = AppConfig.appConfig.getProperty("adduser.defaultorganization.name");
        String roleName = AppConfig.appConfig.getProperty("adduser.defaultrole.name");
        String droleValue = AppConfig.appConfig.getProperty("adduser.defaultrole.value");
        role.setUid(userIdentity.getUid());
        role.setApplicationId(applicationId);
        role.setApplicationName(applicationName);
        role.setOrganizationName(organizationName);
        role.setApplicationRoleName(roleName);
        role.setApplicationRoleValue(droleValue);
        addDefaultRole(userIdentity, role);

        if (facebook) {
            role = new UserPropertyAndRole();
            String fbapplicationId = AppConfig.appConfig.getProperty("adduser.facebook.defaultapplication.id");
            String fbapplicationName = AppConfig.appConfig.getProperty("adduser.facebook.defaultapplication.name");
            String fborganizationName = AppConfig.appConfig.getProperty("adduser.facebook.defaultorganization.name");
            String fbRoleName = AppConfig.appConfig.getProperty("adduser.facebook.defaultrole.name");
            role.setUid(userIdentity.getUid());
            role.setApplicationId(fbapplicationId);
            role.setApplicationName(fbapplicationName);
            role.setOrganizationName(fborganizationName);
            role.setApplicationRoleName(fbRoleName);
            role.setApplicationRoleValue(roleValue);
            addDefaultRole(userIdentity, role);
        }
        if (netiq) {
            role = new UserPropertyAndRole();
            String netIQapplicationId = AppConfig.appConfig.getProperty("adduser.netiq.defaultapplication.id");
            String netIQapplicationName = AppConfig.appConfig.getProperty("adduser.netiq.defaultapplication.name");
            String netIQorganizationName = AppConfig.appConfig.getProperty("adduser.netiq.defaultorganization.name");
            String netIQRoleName = AppConfig.appConfig.getProperty("adduser.netiq.defaultrole.name");
            role.setUid(userIdentity.getUid());
            role.setApplicationId(netIQapplicationId);
            role.setApplicationName(netIQapplicationName);
            role.setOrganizationName(netIQorganizationName);
            role.setApplicationRoleName(netIQRoleName);
            role.setApplicationRoleValue(userIdentity.getEmail());  // Provide NetIQ identity as rolevalue
            addDefaultRole(userIdentity, role);
        }
    }

    private void addDefaultRole(UserIdentity userIdentity, UserPropertyAndRole role) {
        logger.debug("Adding Role: {}", role);

        if (roleRepository.hasRole(userIdentity.getUid(), role)) {
            logger.warn("Role already exist. " + role.toString());
            // roleRepository.deleteUserRole(userIdentity.getUid(), role.getApplicationId(), role.getOrganizationId(), role.getRoleName());
        }

        String value = "uid=" + userIdentity + ", username=" + userIdentity.getUsername() + ", appid=" + role.getApplicationId() + ", role=" + role.getApplicationRoleName();
        try {
            if (userIdentity!= null){
            roleRepository.addUserPropertyAndRole(role);
            audit(ActionPerformed.ADDED, "role", value);
            }
        } catch (Exception e) {
            logger.warn("Failed to add role:" + value,e);
        }
    }


    private void audit(String action, String what, String value) {
        UserToken authenticatedUser = Authentication.getAuthenticatedUser();
        if (authenticatedUser == null) {
            logger.error("authenticatedUser is not set. Auditing failed for action=" + action + ", what=" + what + ", value=" + value);
            return;
        }
        String userId = authenticatedUser.getName();
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(userId, now, action, what, value);
        auditLogRepository.store(actionPerformed);
    }


}
