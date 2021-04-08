package net.whydah.identity.user.identity;


public class UserIdentityConverter extends Converter<LDAPUserIdentity, RDBMSUserIdentity> {

    public UserIdentityConverter(){
        super(UserIdentityConverter::convertToRDBMSUserIdentity, UserIdentityConverter::convertToLDAPUserIdentity);
    }

    private static RDBMSUserIdentity convertToRDBMSUserIdentity(LDAPUserIdentity ldap) {
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
