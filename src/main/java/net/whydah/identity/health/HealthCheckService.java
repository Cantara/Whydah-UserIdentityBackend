package net.whydah.identity.health;

import net.whydah.identity.user.identity.LDAPUserIdentity;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.identity.UserIdentityServiceV2;
import net.whydah.identity.user.role.UserApplicationRoleEntryDao;
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
    static String USERADMIN_UID = "useradmin";    //uid of user which should always exist
    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);
    private final UserIdentityService identityService;
    private final UserIdentityServiceV2 identityServiceV2;
    private final UserApplicationRoleEntryDao userApplicationRoleEntryDao;
    private long intrusionsDetected = 0;
    private long anonymousIntrsionsDetected = 0;

    @Autowired
    public HealthCheckService(UserIdentityServiceV2 userIdentityServiceV2, UserIdentityService identityService, UserApplicationRoleEntryDao userApplicationRoleEntryDao) {
        this.identityService = identityService;
        this.identityServiceV2 = userIdentityServiceV2;
        this.userApplicationRoleEntryDao = userApplicationRoleEntryDao;
    }


    boolean isOK() {
        log.trace("Checking if uid={} can be found in LDAP and role database.", USERADMIN_UID);
        //How to do count in ldap without fetching all users?
        if (!userExistInLdap(USERADMIN_UID)) {
            USERADMIN_UID = "whydahadmin";  // Initiel support for future support of configurable LDAP admin UID
        }
        return userExistInLdap(USERADMIN_UID) && atLeastOneRoleInDatabase();

    }

    //TODO Make this test more robust
    private boolean userExistInLdap(String uid) {
        try {
            LDAPUserIdentity user = identityService.getUserIdentity(uid);
            if (user != null && uid.equals(user.getUid())) {
                return true;
            }
        } catch (NamingException e) {
            log.error("countUserRolesInDB failed. isOK returned false", e);
        }
        return false;
    }

    public void addIntrusion(){
        if (intrusionsDetected > Long.MAX_VALUE -10) {
            log.warn("IntrusionsDetected is at max value of Long. Resetting. Count {}", intrusionsDetected);
            intrusionsDetected = 0;
        }

        intrusionsDetected += 1;
    }

    public long countIntrusionAttempts(){
        return intrusionsDetected;
    }

    public long countAnonymousIntrusionAttempts() {
        return anonymousIntrsionsDetected;
    }


    private boolean atLeastOneRoleInDatabase() {
        return userApplicationRoleEntryDao.countUserRolesInDB() > 0;
    }

    public void addIntrusionAnonymous() {
        if (anonymousIntrsionsDetected > Long.MAX_VALUE -10) {
            log.warn("AnonymousIntrusionsDetected is at max value of Long. Resetting. Count {}", anonymousIntrsionsDetected);
            anonymousIntrsionsDetected = 0;
        }
        anonymousIntrsionsDetected += 1;
    }
}
