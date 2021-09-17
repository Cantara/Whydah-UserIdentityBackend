package net.whydah.identity.user.authentication;

import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.security.Authentication;
import net.whydah.identity.user.identity.BCryptService;
import net.whydah.identity.user.identity.RDBMSUserIdentity;
import net.whydah.identity.user.identity.RDBMSUserIdentityDao;
import net.whydah.identity.user.role.UserApplicationRoleEntryDao;
import net.whydah.identity.user.search.LuceneUserIndexer;
import net.whydah.sso.ddd.model.user.Email;
import net.whydah.sso.ddd.model.user.UserName;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;
import org.constretto.ConstrettoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
 * Should probably be refactored to a more proper service.
 *
 * @author <a href="mailto:erik.drolshammer@altran.com">Erik Drolshammer</a>
 * @since 10/4/12
 */
@Service
public class UserAdminHelper {
    private static final Logger logger = LoggerFactory.getLogger(UserAdminHelper.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    private final RDBMSUserIdentityDao rdbmsUserIdentityDao;
    private final LuceneUserIndexer luceneIndexer;
    private final AuditLogDao auditLogDao;
    private final UserApplicationRoleEntryDao userApplicationRoleEntryDao;
    private final BCryptService bCryptService;

    private String defaultApplicationId;
    private String defaultApplicationName;
    private String defaultOrganizationName;
    private String defaultRoleName;
    private String defaultRoleValue;
    private String netIQapplicationId;
    private String netIQapplicationName;
    private String netIQorganizationName;
    private String netIQRoleName;
    private String fbapplicationId;
    private String fbapplicationName;
    private String fborganizationName;
    private String fbRoleName;

    @Autowired
    public UserAdminHelper(RDBMSUserIdentityDao rdbmsUserIdentityDao,
                           LuceneUserIndexer luceneIndexer, AuditLogDao auditLogDao,
                           UserApplicationRoleEntryDao userApplicationRoleEntryDao,
                           BCryptService bCryptService,
                           ConstrettoConfiguration configuration) {
        this.rdbmsUserIdentityDao = rdbmsUserIdentityDao;
        this.luceneIndexer = luceneIndexer;
        this.auditLogDao = auditLogDao;
        this.userApplicationRoleEntryDao = userApplicationRoleEntryDao;
        this.bCryptService = bCryptService;

        //AppConfig configuration = AppConfig.appConfig;
        initAddUserDefaults(configuration);
    }

    private void initAddUserDefaults(ConstrettoConfiguration configuration) {
        this.defaultApplicationId = configuration.evaluateToString("adduser.defaultapplication.id");
        this.defaultApplicationName = configuration.evaluateToString("adduser.defaultapplication.name");
        this.defaultOrganizationName = configuration.evaluateToString("adduser.defaultorganization.name");
        this.defaultRoleName = configuration.evaluateToString("adduser.defaultrole.name");
        this.defaultRoleValue = configuration.evaluateToString("adduser.defaultrole.value");

        this.netIQapplicationId = configuration.evaluateToString("adduser.netiq.defaultapplication.id");
        this.netIQapplicationName = configuration.evaluateToString("adduser.netiq.defaultapplication.name");
        this.netIQorganizationName = configuration.evaluateToString("adduser.netiq.defaultorganization.name");
        this.netIQRoleName = configuration.evaluateToString("adduser.netiq.defaultrole.name");

        this.fbapplicationId = configuration.evaluateToString("adduser.facebook.defaultapplication.id");
        this.fbapplicationName = configuration.evaluateToString("adduser.facebook.defaultapplication.name");
        this.fborganizationName = configuration.evaluateToString("adduser.facebook.defaultorganization.name");
        this.fbRoleName = configuration.evaluateToString("adduser.facebook.defaultrole.name");
    }

    public Response addUser(RDBMSUserIdentity newIdentity) {
        String username = newIdentity.getUsername();

        if (username == null || username.length() < 2) {
            username = newIdentity.getEmail();
            if (username == null || username.length() < 2) {
                logger.error("Could not handle the new user data, no username");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        }
        logger.trace("addUser - Adding new user: {}", username);


        try {
            if (rdbmsUserIdentityDao.getWithUsername(username) != null) {
                logger.info("addUser - User already exists, could not create user " + username);
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }

            newIdentity.setUid(UUID.randomUUID().toString());

            if (newIdentity.getPassword() != null) {
                newIdentity.setPasswordBCrypt(bCryptService.hash(newIdentity.getPassword()));
            }

            rdbmsUserIdentityDao.create(newIdentity);

            logger.info("addUser - Added new user: {}", username);
        } catch (Exception e) {
            logger.error("addUser - Could not create user " + username, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        addDefaultWhydahUserRole(newIdentity);

        try {
            luceneIndexer.addToIndex(newIdentity);
            audit(ActionPerformed.ADDED, "user", newIdentity.toString());
        } catch (Exception e) {
            logger.error("addUser - Error with lucene indexing or audit login for " + username, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
    }

    public static RDBMSUserIdentity createWhydahUserIdentity(Document fbUserDoc) {
        XPath xPath = XPathFactory.newInstance().newXPath();

        try {
            String uid = (String) xPath.evaluate("//userId", fbUserDoc, XPathConstants.STRING);
            String firstName = (String) xPath.evaluate("//firstName", fbUserDoc, XPathConstants.STRING);
            String lastName = (String) xPath.evaluate("//lastName", fbUserDoc, XPathConstants.STRING);
            String username = (String) xPath.evaluate("//username", fbUserDoc, XPathConstants.STRING);
            String email = (String) xPath.evaluate("//email", fbUserDoc, XPathConstants.STRING);
            String cellPhone = (String) xPath.evaluate("//cellPhone", fbUserDoc, XPathConstants.STRING);
            String personRef = (String) xPath.evaluate("//personRef", fbUserDoc, XPathConstants.STRING);

            if (personRef == null) {
                personRef = "";
            }

            if (cellPhone == null) {
                cellPhone = "";
            }
            if (!UserName.isValid(username)) {
                username = uid;
            }
            if (email == null || email.equals("")) {
                if (Email.isValid(username)) {
                    email = username;
                } else {
                    email = "";
                }
            }


            logger.debug("From fbuserXml, fbUserId=" + uid + ", firstName=" + firstName + ", lastName=" + lastName);

            RDBMSUserIdentity userIdentity = new RDBMSUserIdentity();
            userIdentity.setUid(uid.trim());
            userIdentity.setUsername(username.trim());
            userIdentity.setFirstName(firstName.trim());
            userIdentity.setLastName(lastName.trim());
            if (email != null && email.trim().length() > 4) {
                userIdentity.setEmail(email.trim());
            }
            userIdentity.setCellPhone(cellPhone);
            userIdentity.setPersonRef(personRef);

            String password = calculateSyntheticPassword(uid);
            userIdentity.setPassword(password);
            return userIdentity;
        } catch (XPathExpressionException e) {
            logger.error("createWhydahUserIdentity - ", e);
            return null;
        }
    }

    public static String calculateSyntheticPassword(String thirdpartyID) {
        //if we make a random password we cannot sign in the user next time
        //return thirdpartyID + UUID.randomUUID().toString();
        //TODO: check again
        return thirdpartyID;
    }


    public void addDefaultWhydahUserRole(RDBMSUserIdentity userIdentity) {
        UserApplicationRoleEntry role = new UserApplicationRoleEntry();

        role.setUserId(userIdentity.getUid());
        role.setApplicationId(defaultApplicationId);
        role.setApplicationName(defaultApplicationName);
        role.setOrgName(defaultOrganizationName);
        role.setRoleName(defaultRoleName);
        role.setRoleValue(defaultRoleValue);
        role.setRoleValue(userIdentity.getEmail());
        addDefaultRole(userIdentity, role);
    }

    public void addDefaultRoles(RDBMSUserIdentity userIdentity, String roleValue) {

        if (roleValue.indexOf("netIQAccessToken") > 0) {
            addDefaultNetIQRole(userIdentity);
        }
        if (roleValue.indexOf("fbAccessToken") > 0) {
            addDefaultFacebookRole(userIdentity, roleValue);
        }
        if (roleValue.indexOf("aadAccessToken") > 0) {
            addDefaultAzureActiveDirectoryRole(userIdentity, roleValue);
        }
    }

    private void addDefaultNetIQRole(RDBMSUserIdentity userIdentity) {
        UserApplicationRoleEntry role;
        role = new UserApplicationRoleEntry();
        role.setUserId(userIdentity.getUid());
        role.setApplicationId(netIQapplicationId);
        role.setApplicationName(netIQapplicationName);
        role.setOrgName(netIQorganizationName);
        role.setRoleName(netIQRoleName);
        role.setRoleValue(userIdentity.getEmail());  // Provide NetIQ identity as rolevalue
        addDefaultRole(userIdentity, role);
    }

    private void addDefaultFacebookRole(RDBMSUserIdentity userIdentity, String roleValue) {
        UserApplicationRoleEntry role;
        role = new UserApplicationRoleEntry();
        role.setUserId(userIdentity.getUid());
        role.setApplicationId(fbapplicationId);
        role.setApplicationName(fbapplicationName);
        role.setOrgName(fborganizationName);
        role.setRoleName(fbRoleName);
        role.setRoleValue(roleValue);
        addDefaultRole(userIdentity, role);
    }

    private void addDefaultAzureActiveDirectoryRole(RDBMSUserIdentity userIdentity, String roleValue) {
        UserApplicationRoleEntry role;
        role = new UserApplicationRoleEntry();
        role.setUserId(userIdentity.getUid());
        role.setApplicationId("5555");
        role.setApplicationName("AzureAD");
        role.setOrgName("AzureAD");
        role.setRoleName("data");
        role.setRoleValue(roleValue);
        addDefaultRole(userIdentity, role);
    }

    private void addDefaultRole(RDBMSUserIdentity userIdentity, UserApplicationRoleEntry role) {
        logger.debug("Adding Role: {}", role);

        if (userApplicationRoleEntryDao.hasRole(userIdentity.getUid(), role)) {
            logger.warn("addDefaultRole - Role already exist. " + role.toString());
            // userApplicationRoleEntryDao.deleteUserRole(userIdentity.getUid(), role.getApplicationId(), role.getOrganizationId(), role.getRoleName());
        }

        String value = "uid=" + userIdentity + ", username=" + userIdentity.getUsername() + ", appid=" + role.getApplicationId() + ", role=" + role.getRoleName();
        try {
            if (userIdentity != null) {
                userApplicationRoleEntryDao.addUserApplicationRoleEntry(role);
                audit(ActionPerformed.ADDED, "role", value);
            }
        } catch (Exception e) {
            logger.warn("addDefaultRole - Failed to add role:" + value, e);
        }
    }


    private void audit(String action, String what, String value) {
        UserToken authenticatedUser = Authentication.getAuthenticatedUser();
        if (authenticatedUser == null) {
            logger.error("authenticatedUser is not set. Auditing failed for action=" + action + ", what=" + what + ", value=" + value);
            return;
        }
        String userId = authenticatedUser.getUserName();
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(userId, now, action, what, value);
        auditLogDao.store(actionPerformed);
    }


}
