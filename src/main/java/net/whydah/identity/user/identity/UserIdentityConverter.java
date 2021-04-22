package net.whydah.identity.user.identity;

/**
 * Helperclass during transition from LDAP to DB
 * Class should be removed when transition is completed.
 */
public class UserIdentityConverter extends Converter<LDAPUserIdentity, RDBMSUserIdentity> {

    public UserIdentityConverter(){
        super(UserIdentityConverter::convertToRDBMSUserIdentity, UserIdentityConverter::convertToLDAPUserIdentity);
    }

    private static RDBMSUserIdentity convertToRDBMSUserIdentity(LDAPUserIdentity ldap) {
        if (ldap == null) {
            return null;
        }
        final RDBMSUserIdentity userIdentity = new RDBMSUserIdentity(
                ldap.getUid(),
                ldap.getUsername(),
                ldap.getFirstName(),
                ldap.getLastName(),
                ldap.getEmail(),
                ldap.getPassword(),
                ldap.getCellPhone(),
                ldap.getPersonRef()
        );
        return userIdentity;
    }

    private static LDAPUserIdentity convertToLDAPUserIdentity(RDBMSUserIdentity userIdentity) {
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
