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
    private final BCryptService bCryptService;
    private final boolean enableRDBMSResource;

    @Autowired
    public RDBMSLdapUserIdentityRepository(RDBMSLdapUserIdentityDao rdbmsUserIdentityDao, BCryptService bCryptService, ConstrettoConfiguration config) {
        this.rdbmsUserIdentityDao = rdbmsUserIdentityDao;
        this.bCryptService = bCryptService;
        this.enableRDBMSResource = config.evaluateToBoolean("ldap.rdbms.enabled");
        log.info("RDBMS for user identities enabled: " + enableRDBMSResource);
    }

    public boolean isRDBMSEnabled() {
        return enableRDBMSResource;
    }

    public boolean addUserIdentity(final RDBMSUserIdentity userIdentity) throws RuntimeException {
        if (isRDBMSEnabled()) {
            if (userIdentity != null) {
                if (usernameExist(userIdentity.getUsername())) {
                    log.warn("UserIdentity {} already exists", userIdentity.getUsername());
                    return false;
                } else {
                    if (userIdentity.getPassword() != null) {
                        userIdentity.setPasswordBCrypt(bCryptService.hash(userIdentity.getPassword()));
                    }
                    boolean success = rdbmsUserIdentityDao.create(userIdentity);
                    if (success) {
                        log.debug("UserIdentity {} created", userIdentity.getUsername());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public RDBMSUserIdentity getUserIdentityWithId(String uid) throws RuntimeException {
        if (isRDBMSEnabled()) {
            return rdbmsUserIdentityDao.get(uid);
        } else {
            return null;
        }
    }

    public RDBMSUserIdentity getUserIdentityWithUsername(String username) throws RuntimeException {
        if (isRDBMSEnabled()) {
            return rdbmsUserIdentityDao.getWithUsername(username);
        } else {
            return null;
        }
    }

    public RDBMSUserIdentity authenticate(final String username, final String password) {
        if (isRDBMSEnabled()) {
            RDBMSUserIdentity userIdentity = rdbmsUserIdentityDao.getWithUsername(username);
            if (userIdentity == null) {
                return null;
            }
            String bcryptHash = userIdentity.getPasswordBCrypt();
            if (bcryptHash == null) {
                log.warn("User with username {} has stored the password as plaintext, please hash password using bcrypt");
                String storedPassword = userIdentity.getPassword();
                if (storedPassword.equals(password)) {
                    return userIdentity;
                }
                return null;
            }
            if (bCryptService.verifyPassword(bcryptHash, password)) {
                String updatedHash = bCryptService.updatePasswordHash(bcryptHash, password);
                if (!updatedHash.equals(bcryptHash)) {
                    // update with a stronger hash in database
                    rdbmsUserIdentityDao.updatePassword(username, updatedHash);
                }
                return userIdentity;
            } else {
                log.warn("Authentication failed for UserIdentity {} - password mismatch", username);
                return null;
            }
        }
        return null;
    }

    public void setTempPassword(final String username, final String saltedPassword) throws RuntimeException {
        if (isRDBMSEnabled()) {
            if (username != null && saltedPassword != null) {
                boolean success = rdbmsUserIdentityDao.updatePassword(username, saltedPassword);
                if (success) {
                    log.info("UserIdentity {} password updated", username);
                } else {
                    log.info("UserIdentity {} password not updated", username);
                }
            }
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

    public void changePassword(final String username, final String newPassword) {
        if (isRDBMSEnabled()) {
            if (username != null && newPassword != null) {
                boolean success = rdbmsUserIdentityDao.updatePassword(username, newPassword);
                if (success) {
                    log.info("UserIdentity {} password updated", username);
                } else {
                    log.info("UserIdentity {} password not updated", username);
                }
            }
        }
    }

    public boolean usernameExist(final String username) throws RuntimeException {
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

    public void deleteUserIdentity(final String username) throws RuntimeException {
        if (isRDBMSEnabled()) {
            RDBMSUserIdentity userIdentity = rdbmsUserIdentityDao.getWithUsername(username);
            if (userIdentity != null && userIdentity.getUsername().equalsIgnoreCase(username)) {
                boolean success = rdbmsUserIdentityDao.delete(userIdentity.getUid());
                if (success) {
                    log.info("UserIdentity {} deleted", username);
                } else {
                    log.warn("UserIdentity {} not deleted", username);
                }
            }
        }
    }

    public void updateUserIdentityForUsername(final String username, final RDBMSUserIdentity newuser) throws RuntimeException {
        if (isRDBMSEnabled()) {
            RDBMSUserIdentity userIdentity = rdbmsUserIdentityDao.getWithUsername(username);
            if (userIdentity != null && userIdentity.getUsername().equalsIgnoreCase(username)) {
                updateUserIdentityForUid(userIdentity.getUid(), newuser);
            }
        }
    }

    public void updateUserIdentityForUid(final String uid, final RDBMSUserIdentity newUserIdentity) throws RuntimeException {
        if (isRDBMSEnabled()) {
            rdbmsUserIdentityDao.update(uid, newUserIdentity);
        }
    }

    public RDBMSUserIdentity getUserIdentityWithUsernameOrUid(String usernameOrUid) throws RuntimeException {
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
