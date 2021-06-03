package net.whydah.identity.user.identity;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class BCryptUtilsTest {

    @Test
    public void testSimpleHashAndVerify() {
        String bcryptString = BCryptUtils.hash("v3ry s3cr3t ungu3ssab13 passw0rd");
        assertFalse(BCryptUtils.verifyPassword(bcryptString, "incorrect password"));
        assertTrue(BCryptUtils.verifyPassword(bcryptString, "v3ry s3cr3t ungu3ssab13 passw0rd"));
    }

    @Test
    public void testReallyLongPasswordHashAndVerify() {
        String bcryptString = BCryptUtils.hash("this is an extremely long password! la la di da, la la la la di da da da. la la la la la la, la la la la di da, ohh la la la, ooh la la di da.");
        assertFalse(BCryptUtils.verifyPassword(bcryptString, "incorrect password"));
        assertTrue(BCryptUtils.verifyPassword(bcryptString, "this is an extremely long password! la la di da, la la la la di da da da. la la la la la la, la la la la di da, ohh la la la, ooh la la di da."));
    }

    @Test
    public void testUpdatePasswordHash() {
        String weakBcryptHash = BCryptUtils.BCRYPT_HASHER.hashToString(4, "v3ry s3cr3t ungu3ssab13 passw0rd".toCharArray());
        String strongerBcryptHash = BCryptUtils.updatePasswordHash(weakBcryptHash, "v3ry s3cr3t ungu3ssab13 passw0rd");
        assertNotEquals(weakBcryptHash, strongerBcryptHash);
        BCryptUtils.verifyPassword(weakBcryptHash, "v3ry s3cr3t ungu3ssab13 passw0rd");
        BCryptUtils.verifyPassword(strongerBcryptHash, "v3ry s3cr3t ungu3ssab13 passw0rd");
    }

    @Test
    public void testRawHashingAndParsing() {
        byte[] salt = generateRandomSalt();
        BCrypt.HashData hashData = BCryptUtils.BCRYPT_HASHER.hashRaw(8, salt, "v3ry s3cr3t ungu3ssab13 passw0rd".getBytes(StandardCharsets.UTF_8));
        String bcryptString = new String(hashData.version.formatter.createHashMessage(hashData), StandardCharsets.US_ASCII);
        assertEquals(hashData, BCryptUtils.parse(bcryptString));
        assertFalse(BCryptUtils.verifyPassword(bcryptString, "incorrect password"));
        assertTrue(BCryptUtils.verifyPassword(bcryptString, "v3ry s3cr3t ungu3ssab13 passw0rd"));
    }

    private byte[] generateRandomSalt() {
        byte[] salt = new byte[16];
        BCryptUtils.SECURE_RANDOM.nextBytes(salt);
        return salt;
    }
}
