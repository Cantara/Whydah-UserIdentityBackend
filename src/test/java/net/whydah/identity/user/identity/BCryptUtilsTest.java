package net.whydah.identity.user.identity;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class BCryptUtilsTest {

    static final BCryptService bCryptService = new BCryptService("75Ez8ghiU$o", 6);

    @Test
    public void testSimpleHashAndVerify() {
        String bcryptString = bCryptService.hash("v3ry s3cr3t ungu3ssab13 passw0rd");
        assertFalse(bCryptService.verifyPassword(bcryptString, "incorrect password"));
        assertTrue(bCryptService.verifyPassword(bcryptString, "v3ry s3cr3t ungu3ssab13 passw0rd"));
    }

    @Test
    public void testReallyLongPasswordHashAndVerify() {
        String bcryptString = bCryptService.hash("this is an extremely long password! la la di da, la la la la di da da da. la la la la la la, la la la la di da, ohh la la la, ooh la la di da.");
        assertFalse(bCryptService.verifyPassword(bcryptString, "incorrect password"));
        assertTrue(bCryptService.verifyPassword(bcryptString, "this is an extremely long password! la la di da, la la la la di da da da. la la la la la la, la la la la di da, ohh la la la, ooh la la di da."));
    }

    @Test
    public void testUpdatePasswordHash() {
        BCryptService weakerBCryptService = new BCryptService("75Ez8ghiU$o", 4);
        String weakBcryptHash = weakerBCryptService.hash("v3ry s3cr3t ungu3ssab13 passw0rd");
        String strongerBcryptHash = bCryptService.updatePasswordHash(weakBcryptHash, "v3ry s3cr3t ungu3ssab13 passw0rd");
        assertNotEquals(weakBcryptHash, strongerBcryptHash);
        bCryptService.verifyPassword(weakBcryptHash, "v3ry s3cr3t ungu3ssab13 passw0rd");
        bCryptService.verifyPassword(strongerBcryptHash, "v3ry s3cr3t ungu3ssab13 passw0rd");
    }

}
