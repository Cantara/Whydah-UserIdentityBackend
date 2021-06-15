package net.whydah.identity.user.identity;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

public class PBKDF2WithHMACSha1PasswordAuthenticatorTest {

    @Test
    public void testKnownPasswordIsCorrect() {
        String hash = createHash("s3cr3t");
        boolean valid = PBKDF2WithHMACSha1PasswordAuthenticator.validatePassword("s3cr3t", hash);
        Assert.assertTrue(valid);
    }

    private String createHash(String password) {
        SecureRandom rnd = new SecureRandom();
        byte[] salt = new byte[16];
        rnd.nextBytes(salt);
        byte[] subkey = PBKDF2WithHMACSha1PasswordAuthenticator.hashPassword(password, salt);
        byte[] buf = new byte[49];
        ByteBuffer bb = ByteBuffer.wrap(buf);
        bb.put((byte) 0);
        bb.put(salt);
        bb.put(subkey);
        return Base64.getEncoder().encodeToString(buf);
    }
}
