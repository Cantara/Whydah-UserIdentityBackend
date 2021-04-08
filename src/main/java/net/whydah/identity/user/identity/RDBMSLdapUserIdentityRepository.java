package net.whydah.identity.user.identity;

import org.constretto.ConstrettoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RDBMSLdapUserIdentityRepository {
    private static final Logger log = LoggerFactory.getLogger(RDBMSLdapUserIdentityRepository.class);

    private final RDBMSLdapUserIdentityDao rdbmsUserIdentityDao;
    private final boolean enableRDBMSResource;

    @Autowired
    public RDBMSLdapUserIdentityRepository(RDBMSLdapUserIdentityDao rdbmsUserIdentityDao, ConstrettoConfiguration config) {
        this.rdbmsUserIdentityDao = rdbmsUserIdentityDao;
        this.enableRDBMSResource = config.evaluateToBoolean("ldap.rdbms.enabled");
        log.info("RDBMS for ldap user identities is in use: " + enableRDBMSResource);
    }

    public void addUserIdentity(RDBMSUserIdentity userIdentity) throws RuntimeException {
        if (enableRDBMSResource) {
            rdbmsUserIdentityDao.create(userIdentity);
        }
    }

    public RDBMSUserIdentity getUserIdentityWithId(String uid) {
        if (isRDBMSEnabled()) {
            return rdbmsUserIdentityDao.get(uid);
        } else {
            return null;
        }
    }

    public RDBMSUserIdentity getUserIdentityWithUsername(String username) {
        if (isRDBMSEnabled()) {
            return rdbmsUserIdentityDao.getWithUsername(username);
        } else {
            return null;
        }
    }

    public RDBMSUserIdentity authenticate(final String username, final String password) {
        if (isRDBMSEnabled()) {
            RDBMSUserIdentity userIdentity = rdbmsUserIdentityDao.getWithUsername(username);
            if (password.equals(userIdentity.getPassword())) {
                return userIdentity;
            } else {
                return null;
            }
        }
        return null;
    }

    public boolean isRDBMSEnabled() {
        return enableRDBMSResource;
    }

    public void setTempPassword(String username, String saltedPassword) {
        if (isRDBMSEnabled()) {
            rdbmsUserIdentityDao.updatePassword(username, saltedPassword);
        }
    }

    public String getSalt(String username) {
        if (isRDBMSEnabled()) {
            RDBMSUserIdentity userIdentity = getUserIdentityWithUsername(username);
            if (userIdentity != null) {
                return userIdentity.getPassword();
            } else {
                return null;
            }
        }
        return null;
    }

    public void changePassword(String username, String newPassword) {
        if (isRDBMSEnabled()) {
            setTempPassword(username, newPassword);
        }
    }

    public boolean usernameExist(String username) {
        if (isRDBMSEnabled()) {
            RDBMSUserIdentity userIdentity = getUserIdentityWithUsername(username);
            if (userIdentity != null && userIdentity.getUsername().equalsIgnoreCase(username)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public void deleteUserIdentity(String username) {
        if (isRDBMSEnabled()) {
            RDBMSUserIdentity userIdentity = rdbmsUserIdentityDao.getWithUsername(username);
            if (userIdentity != null && userIdentity.getUsername().equalsIgnoreCase(username)) {
                rdbmsUserIdentityDao.delete(userIdentity.getUid());
            }
        }
    }

    public void updateUserIdentityForUsername(String username, RDBMSUserIdentity newuser) throws RuntimeException {
        if (isRDBMSEnabled()) {
            RDBMSUserIdentity userIdentity = rdbmsUserIdentityDao.getWithUsername(username);
            if (userIdentity != null && userIdentity.getUsername().equalsIgnoreCase(username)) {
                updateUserIdentityForUid(userIdentity.getUid(), newuser);
            }
        }
    }

    public void updateUserIdentityForUid(String uid, RDBMSUserIdentity newUserIdentity) throws RuntimeException {
        if (isRDBMSEnabled()) {
            rdbmsUserIdentityDao.update(uid, newUserIdentity);
        }
    }

    public RDBMSUserIdentity getUserIdentityWithUsernameOrUid(String usernameOrUid) {
        if (isRDBMSEnabled()) {
            RDBMSUserIdentity userIdentity = rdbmsUserIdentityDao.getWithUsername(usernameOrUid);
            if (userIdentity == null) {
                userIdentity = rdbmsUserIdentityDao.get(usernameOrUid);
            }
            return userIdentity;
        }
        return null;
    }
}
