package net.whydah.identity.user;

import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.security.Authentication;
import net.whydah.identity.user.authentication.UserToken;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.identity.UserIdentityRepresentation;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.resource.RoleRepresentationRequest;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import net.whydah.identity.user.search.LuceneIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 29.03.14
 */
@Service
public class UserAggregateService {
    private static final Logger log = LoggerFactory.getLogger(UserAggregateService.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    private final UserIdentityService userIdentityService;
    private final UserPropertyAndRoleRepository userPropertyAndRoleRepository;
    private final LuceneIndexer luceneIndexer;
    private final AuditLogDao auditLogDao;

    @Autowired
    public UserAggregateService(UserIdentityService userIdentityService, UserPropertyAndRoleRepository userPropertyAndRoleRepository,
                                LuceneIndexer luceneIndexer, AuditLogDao auditLogDao) {
        this.luceneIndexer = luceneIndexer;
        this.auditLogDao = auditLogDao;
        this.userPropertyAndRoleRepository = userPropertyAndRoleRepository;
        this.userIdentityService = userIdentityService;
    }


    public UserAggregate getUserAggregateByUsername(String username) {
        UserIdentity userIdentity;
        try {
            userIdentity = userIdentityService.getUserIdentity(username);
        } catch (NamingException e) {
            throw new RuntimeException("userIdentityService.getUserIdentity with username=" + username, e);
        }
        if (userIdentity == null) {
            log.trace("getUserAggregateByUsername could not find user with username={}", username);
            return null;
        }
        List<UserPropertyAndRole> userPropertyAndRoles = userPropertyAndRoleRepository.getUserPropertyAndRoles(userIdentity.getUid());
        return new UserAggregate(userIdentity, userPropertyAndRoles);
    }

    public UserIdentity updateUserIdentity(String uid, UserIdentity newUserIdentity) {
        userIdentityService.updateUserIdentityForUid(uid, newUserIdentity);
        return newUserIdentity;
    }


    public UserIdentityRepresentation updateUserIdentity(String username, String userJson) {
        UserIdentity newUserIdentity = UserIdentity.fromJson(userJson);

        try {
            UserIdentity userIdentity = userIdentityService.getUserIdentity(username);
            if (userIdentity == null) {
                return null;
            }

            userIdentityService.updateUserIdentity(username, newUserIdentity);
            luceneIndexer.update(newUserIdentity);
            audit(ActionPerformed.MODIFIED, "user", newUserIdentity.toString());
        } catch (NamingException e) {
            throw new RuntimeException("updateUserIdentity failed for username=" + username + ", newUserIdentity=" + newUserIdentity, e);
        }
        return newUserIdentity;
    }

    public void deleteUserAggregateByUid(String uid) {
        UserIdentity userIdentity;
        try {
            userIdentity = userIdentityService.getUserIdentityForUid(uid);
            luceneIndexer.removeFromIndex(uid);
        } catch (NamingException e) {
            throw new RuntimeException("userIdentityService.getUserIdentity with uid=" + uid, e);
        }
        if (userIdentity == null) {
            throw new IllegalArgumentException("UserIdentity not found. uid=" + uid);
        }
        try {
            userIdentityService.deleteUserIdentity(uid);
        } catch (NamingException ne) {
            log.warn("Trouble trying to delete user with uid:" + uid);
        }

        deleteRolesForUser(userIdentity);
    }


    // FIXME This does not seem to make any sense...  DELETE user or DELETE role?
    private void deleteRolesForUser(UserIdentity userIdentity) {
        String uid = userIdentity.getUid();
        userPropertyAndRoleRepository.deleteUser(uid);
        //indexer.removeFromIndex(uid);
        audit(ActionPerformed.DELETED, "user", "uid=" + uid + ", username=" + userIdentity.getUsername());
    }

    private void audit(String action, String what, String value) {
        UserToken authenticatedUser = Authentication.getAuthenticatedUser();
        if (authenticatedUser == null) {
            log.error("authenticatedUser is not set. Auditing failed for action=" + action + ", what=" + what + ", value=" + value);
            return;
        }
        String userId = authenticatedUser.getName();
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(userId, now, action, what, value);
        auditLogDao.store(actionPerformed);
    }

    private void audit(String uid, String action, String what, String value) {
        UserToken authenticatedUser = Authentication.getAuthenticatedUser();
        if (authenticatedUser == null) {
            log.error("authenticatedUser is not set. Auditing failed for action=" + action + ", what=" + what + ", value=" + value);
            return;
        }
        String userId = authenticatedUser.getName();
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(uid, now, action, what, value);
        auditLogDao.store(actionPerformed);
    }

    public UserPropertyAndRole addRole(String uid, RoleRepresentationRequest request) {
        UserPropertyAndRole role = new UserPropertyAndRole();
        role.setUid(uid);
        role.setApplicationId(request.getApplicationId());
        role.setApplicationName(request.getApplicationName());
        role.setOrganizationName(request.getOrganizationName());
        role.setApplicationRoleName(request.getApplicationRoleName());
        role.setApplicationRoleValue(request.getApplicationRoleValue());

        if (userPropertyAndRoleRepository.hasRole(uid, role)) {
            String msg = "User with uid=" + uid + " already has this role. " + role.toString();
            throw new WebApplicationException(msg, Response.Status.CONFLICT);
        }

        userPropertyAndRoleRepository.addUserPropertyAndRole(role);
        String value = "uid=" + uid + ", appid=" + role.getApplicationId() + ", role=" + role.getApplicationRoleName();
        audit(ActionPerformed.ADDED, "role", value);
        return role;
    }

    public UserPropertyAndRole getRole(String uid, String roleId) {
        return userPropertyAndRoleRepository.getUserPropertyAndRole(roleId);
    }

    public List<UserPropertyAndRole> getRoles(String uid) {
        return userPropertyAndRoleRepository.getUserPropertyAndRoles(uid);
    }

    public UserPropertyAndRole updateRole(String uid, String roleId, UserPropertyAndRole role) {
        UserPropertyAndRole existingUserPropertyAndRole = userPropertyAndRoleRepository.getUserPropertyAndRole(roleId);
        if (existingUserPropertyAndRole == null) {
            throw new NonExistentRoleException("RoleID does not exist: " + roleId);
        }
        if (!existingUserPropertyAndRole.getUid().equals(role.getUid())) {
            throw new InvalidRoleModificationException("Illegal attempt to change uid from " + existingUserPropertyAndRole.getUid() + " to " + role.getUid() + " for roleId " + roleId);
        }
        if (!existingUserPropertyAndRole.getApplicationId().equals(role.getApplicationId())) {
            throw new InvalidRoleModificationException("Illegal attempt to change applicationId from " + existingUserPropertyAndRole.getApplicationId() + " to " + role.getApplicationId() + " for roleId " + roleId);
        }
        if (!existingUserPropertyAndRole.getOrganizationName().equals(role.getOrganizationName())) {
            throw new InvalidRoleModificationException("Illegal attempt to change organizationName from " + existingUserPropertyAndRole.getOrganizationName() + " to " + role.getOrganizationName() + " for roleId " + roleId);
        }

        role.setUid(uid);
        role.setRoleId(roleId);
        userPropertyAndRoleRepository.updateUserRoleValue(role);

        //audit(ActionPerformed.MODIFIED, "role", "uid=" + uid + ", appid=" + role.getApplicationId() + ", role=" + jsonrole);
        return role;
    }

    public void deleteRole(String uid, String roleid) {
        userPropertyAndRoleRepository.deleteRole(roleid);
    }
}
