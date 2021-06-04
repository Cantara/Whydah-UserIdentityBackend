package net.whydah.identity.user.identity;

/**
 * Helperclass during transition from LDAP to DB
 * Class should be removed when transition is completed.
 */
public class UserIdentityConverter {

    private final BCryptService bCryptService;

    public UserIdentityConverter(BCryptService bCryptService) {
        this.bCryptService = bCryptService;
    }

    public RDBMSUserIdentity convertFromLDAPUserIdentity(LDAPUserIdentity ldap) {
        if (ldap == null) {
            return null;
        }
        String password = ldap.getPassword();
        String hashedPassword;
        if (password == null) {
            hashedPassword = null; // password not set in ldap
        } else if (password.startsWith("{SSHA}")) {
            hashedPassword = null; // Already uses another hashing algorithm, do not set password/hash
        } else {
            hashedPassword = bCryptService.hash(password);
        }
        final RDBMSUserIdentity userIdentity = new RDBMSUserIdentity(
                ldap.getUid(),
                ldap.getUsername(),
                ldap.getFirstName(),
                ldap.getLastName(),
                ldap.getEmail(),
                hashedPassword,
                ldap.getCellPhone(),
                ldap.getPersonRef()
        );
        return userIdentity;
    }

    public LDAPUserIdentity convertFromRDBMSUserIdentity(RDBMSUserIdentity userIdentity) {
        if (userIdentity == null) {
            return null;
        }
        final LDAPUserIdentity ldapUserIdentity = new LDAPUserIdentity(
                userIdentity.getUid(),
                userIdentity.getUsername(),
                userIdentity.getFirstName(),
                userIdentity.getLastName(),
                userIdentity.getEmail(),
                userIdentity.getPassword(),
                userIdentity.getCellPhone(),
                userIdentity.getPersonRef()
        );
        return ldapUserIdentity;
    }
}
