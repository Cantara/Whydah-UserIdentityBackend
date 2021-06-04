package net.whydah.identity.user.identity;

import at.favre.lib.bytes.Bytes;
import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.IllegalBCryptFormatException;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Service
public class BCryptService {

    /*
     * The preferred-cost parameter should be incremented once when the
     * average expected single-threaded cpu processing-power doubles.
     */
    private final int preferredBcryptCost;

    private final SecureRandom SECURE_RANDOM = new SecureRandom(); // used for random salt generation
    private final BCrypt.Hasher BCRYPT_HASHER = BCrypt.with(BCrypt.Version.VERSION_2A, SECURE_RANDOM, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A));
    private final BCrypt.Verifyer VERIFYER = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A));
    private final String pepper;

    @Autowired
    @Configure
    public BCryptService(@Configuration("userdb.password.pepper") String pepper,
                         @Configuration("userdb.password.bcrypt.preferredcost") int preferredBcryptCost) {
        this.pepper = pepper;
        this.preferredBcryptCost = preferredBcryptCost;
    }

    private String pepper(String password) {
        return password + pepper;
    }

    public String hash(String password) {
        String pepperedPassword = pepper(password);
        String bcryptString = BCRYPT_HASHER.hashToString(preferredBcryptCost, pepperedPassword.toCharArray());
        return bcryptString;
    }

    public boolean verifyPassword(String hash, String password) {
        String pepperedPassword = pepper(password);
        BCrypt.Result verifyResult = VERIFYER.verify(pepperedPassword.toCharArray(), hash);
        return verifyResult.verified;
    }

    public String updatePasswordHash(String hash, String password) {
        BCrypt.HashData hashData = parse(hash);
        if (hashData.cost < preferredBcryptCost) {
            return hash(password); // return re-hashed password with the preferred cost
        }
        return hash; // existing hash is already at least as strong as the preferred cost
    }

    private BCrypt.HashData parse(String bcryptString) {
        try {
            byte[] hashBytes = Bytes.from(bcryptString, StandardCharsets.UTF_8).array();
            BCrypt.HashData bcryptHashData = BCrypt.Version.VERSION_2A.parser.parse(hashBytes);
            return bcryptHashData;
        } catch (IllegalBCryptFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
