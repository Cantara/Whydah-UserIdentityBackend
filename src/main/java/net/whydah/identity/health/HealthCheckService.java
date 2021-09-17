package net.whydah.identity.health;

import net.whydah.identity.user.identity.RDBMSUserIdentity;
import net.whydah.identity.user.identity.UserIdentityServiceV2;
import net.whydah.identity.user.role.UserApplicationRoleEntryDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-01-13
 */
@Service
public class HealthCheckService {
    static String USERADMIN_UID = "useradmin";    //uid of user which should always exist
    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);
    private final UserIdentityServiceV2 identityServiceV2;
    private final UserApplicationRoleEntryDao userApplicationRoleEntryDao;
    private long intrusionsDetected = 0;
    private long anonymousIntrsionsDetected = 0;

    @Autowired
    public HealthCheckService(UserIdentityServiceV2 userIdentityServiceV2, UserApplicationRoleEntryDao userApplicationRoleEntryDao) {
        this.identityServiceV2 = userIdentityServiceV2;
        this.userApplicationRoleEntryDao = userApplicationRoleEntryDao;
    }


    boolean isOK_DB() {
        log.trace("Checking if uid={} can be found in DB and role database.", USERADMIN_UID);
        if (!userExistInDB(USERADMIN_UID)) {
            USERADMIN_UID = "whydahadmin";  // Initial support for future support of configurable DB admin UID
        }
        return userExistInDB(USERADMIN_UID) && atLeastOneRoleInDatabase();
    }

    private boolean userExistInDB(String uid) {
        try {
            RDBMSUserIdentity userIdentity = identityServiceV2.getUserIdentity(uid);
            if (userIdentity != null && uid.equals(userIdentity.getUid())) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
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
