package net.whydah.identity.health;

import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.role.UserPropertyAndRoleDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-01-13
 */
@Service
public class HealthCheckService {
    static final String USERADMIN_UID = "useradmin";    //uid of user which should always exist
    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);
    private final UserIdentityService identityService;
    private final UserPropertyAndRoleDao userPropertyAndRoleDao;

    @Autowired
    public HealthCheckService(UserIdentityService identityService, UserPropertyAndRoleDao userPropertyAndRoleDao) {
        this.identityService = identityService;
        this.userPropertyAndRoleDao = userPropertyAndRoleDao;
    }


    boolean isOK() {
        log.trace("Checking if uid={} can be found in LDAP and role database.", USERADMIN_UID);
        //How to do count in ldap without fetching all users?
        return userExistInLdap(USERADMIN_UID) && atLeastOneRoleInDatabase();

    }

    //TODO Make this test more robust
    private boolean userExistInLdap(String uid) {
        try {
            UserIdentity user = identityService.getUserIdentityForUid(uid);
            if (user != null && uid.equals(user.getUid())) {
                return true;
            }
        } catch (NamingException e) {
            log.error("countUserRolesInDB failed. isOK returned false", e);
        }
        return false;
    }


    private boolean atLeastOneRoleInDatabase() {
        return userPropertyAndRoleDao.countUserRolesInDB() > 0;
    }
}
