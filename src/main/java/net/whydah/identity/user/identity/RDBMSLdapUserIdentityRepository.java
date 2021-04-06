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

    public void addLdapUserIdentity(LDAPUserIdentity ldapUserIdentity) {
        if (enableRDBMSResource) {
            int rowsAffected = rdbmsUserIdentityDao.create(ldapUserIdentity);
            if (rowsAffected != 1) {
                throw new RuntimeException("Failed to store ldap entry in RDBMS");
            }
        }
    }

    public void deleteLdapUserIdentity(LDAPUserIdentity ldapUserIdentity) {
        if (enableRDBMSResource) {
            int rowsAffected = rdbmsUserIdentityDao.delete(ldapUserIdentity.getUid());
            if (rowsAffected != 1) {
                throw new RuntimeException("Failed to delete ldap entry in RDBMS");
            }
        }
    }

    public LDAPUserIdentity getLdapUserIdentityWithId(String uid) {
        if (enableRDBMSResource) {
            return rdbmsUserIdentityDao.get(uid);
        } else {
            return null;
        }
    }

    public LDAPUserIdentity getLdapUserIdentityWithUsername(String username) {
        if (enableRDBMSResource) {
            return rdbmsUserIdentityDao.getWithUsername(username);
        } else {
            return null;
        }
    }

    public LDAPUserIdentity authenticate(final String username, final String password) {
        LDAPUserIdentity ldapUserIdentity = null;
        if (enableRDBMSResource) {
            ldapUserIdentity = rdbmsUserIdentityDao.getWithUsername(username);
            if (password.equals(ldapUserIdentity.getPassword())) {
                return ldapUserIdentity;
            }
        }
        return ldapUserIdentity;
    }

    public boolean isRDBMSEnabled() {
        return enableRDBMSResource;
    }
}
