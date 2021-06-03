package net.whydah.identity.user.identity;

import net.whydah.identity.util.PasswordGenerator;
import net.whydah.sso.user.types.UserIdentity;

import java.util.UUID;

/**
 * Helperclass during transition from LDAP to DB
 * Class should be removed when transition is completed.
 */
public class UserIdentityWithAutomaticPasswordGeneration extends UserIdentity {
    private String password;
    private PasswordGenerator passwordGenerator = new PasswordGenerator();

    public UserIdentityWithAutomaticPasswordGeneration(UserIdentity userIdentity) {
        this(userIdentity.getUid() != null ? userIdentity.getUid() : UUID.randomUUID().toString(),
                userIdentity.getUsername(),
                userIdentity.getFirstName(),
                userIdentity.getLastName(),
                userIdentity.getPersonRef(),
                userIdentity.getEmail(),
                userIdentity.getCellPhone());
    }

    public UserIdentityWithAutomaticPasswordGeneration(String uid, String username, String firstName, String lastName, String personRef, String email, String cellPhone) {
        super(uid, username, firstName, lastName, personRef, email, cellPhone);
        this.password = passwordGenerator.generate();
    }

    public UserIdentityWithAutomaticPasswordGeneration(String username, String firstName, String lastName, String personRef, String email, String cellPhone) {
        super(username, firstName, lastName, personRef, email, cellPhone);
        this.password = passwordGenerator.generate();
    }

    public String getPassword() {
        return password;
    }

}
