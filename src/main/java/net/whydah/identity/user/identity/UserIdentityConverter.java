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

    public RDBMSUserIdentity convertFromLuceneUserIdentity(LuceneUserIdentity luceneUserIdentity) {
        if (luceneUserIdentity == null) {
            return null;
        }
        String password = luceneUserIdentity.getPassword();
        String hashedPassword;
        if (password == null) {
            hashedPassword = null; // password not set
        } else if (password.startsWith("{SSHA}")) {
            hashedPassword = null; // Already uses another hashing algorithm, do not set password/hash
        } else {
            hashedPassword = bCryptService.hash(password);
        }
        final RDBMSUserIdentity userIdentity = new RDBMSUserIdentity(
                luceneUserIdentity.getUid(),
                luceneUserIdentity.getUsername(),
                luceneUserIdentity.getFirstName(),
                luceneUserIdentity.getLastName(),
                luceneUserIdentity.getEmail(),
                hashedPassword,
                luceneUserIdentity.getCellPhone(),
                luceneUserIdentity.getPersonRef()
        );
        return userIdentity;
    }
}
