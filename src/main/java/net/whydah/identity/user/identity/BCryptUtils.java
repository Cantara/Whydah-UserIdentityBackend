package net.whydah.identity.user.identity;

import at.favre.lib.bytes.Bytes;
import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.IllegalBCryptFormatException;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class BCryptUtils {

    /*
     * The preferred-cost parameter should be incremented once when the
     * average expected single-threaded cpu processing-power doubles.
     */
    static final int PREFERRED_BCRYPT_COST = 12;

    static final SecureRandom SECURE_RANDOM = new SecureRandom();
    static final BCrypt.Hasher BCRYPT_HASHER = BCrypt.with(BCrypt.Version.VERSION_2A, SECURE_RANDOM, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A));
    static final BCrypt.Verifyer VERIFYER = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A));

    public static String hash(String password) {
        String bcryptString = BCRYPT_HASHER.hashToString(PREFERRED_BCRYPT_COST, password.toCharArray());
        return bcryptString;
    }

    public static boolean verifyPassword(String hash, String password) {
        BCrypt.Result verifyResult = VERIFYER.verify(password.toCharArray(), hash);
        return verifyResult.verified;
    }

    public static String updatePasswordHash(String hash, String password) {
        BCrypt.HashData hashData = parse(hash);
        if (hashData.cost < PREFERRED_BCRYPT_COST) {
            return hash(password); // return re-hashed password with the preferred cost
        }
        return hash; // existing hash is already at least as strong as the preferred cost
    }

    static BCrypt.HashData parse(String bcryptString) {
        try {
            byte[] passwordBytes = Bytes.from(bcryptString, StandardCharsets.UTF_8).array();
            BCrypt.HashData bcryptHashData = BCrypt.Version.VERSION_2A.parser.parse(passwordBytes);
            return bcryptHashData;
        } catch (IllegalBCryptFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
