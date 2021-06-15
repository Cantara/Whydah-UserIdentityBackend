package net.whydah.identity.user.identity;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

/**
 * Code to check passwords hashed with the following code (used by IdentityServer3):
 * https://github.com/aspnet/Identity/blob/rel/2.0.0/src/Microsoft.Extensions.Identity.Core/PasswordHasher.cs
 * <p>
 * Version 2:
 * PBKDF2 with HMAC-SHA1, 128-bit salt, 256-bit subkey, 1000 iterations.
 * Format: { 0x00, salt, subkey }
 */
public class PBKDF2WithHMACSha1PasswordAuthenticator {

    public static boolean validatePassword(String password, String goodBase64Hash) {
        byte[] goodSalt = new byte[16];
        byte[] goodSubkey = new byte[32];
        extractSaltAndSubkey(goodBase64Hash, goodSalt, goodSubkey);

        // compute hash based on password and salt derived from good hash
        byte[] computedHMACSha1Hash = hashPassword(password, goodSalt);

        // Compare computed hmac-sha1 hash (based on password) with known good subkey derived from good hash
        boolean passwordValid = Arrays.equals(computedHMACSha1Hash, goodSubkey);

        return passwordValid;
    }

    static void extractSaltAndSubkey(String goodBase64Hash, byte[] goodSalt, byte[] goodSubkey) {
        byte[] goodHash = Base64.getDecoder().decode(goodBase64Hash);
        ByteBuffer bb = ByteBuffer.wrap(goodHash);
        byte mark = bb.get();
        if (mark != (byte) 0) {
            throw new RuntimeException(String.format("MARK not 0x00: %d%n", mark));
        }
        bb.get(goodSalt);
        bb.get(goodSubkey);
    }

    public static byte[] hashPassword(String password, byte[] salt) {
        try {
            char[] pw = password.toCharArray();
            PBEKeySpec spec = new PBEKeySpec(pw, salt, 1000, 256);
            SecretKeyFactory key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return key.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
