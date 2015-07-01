package net.whydah.identity.user;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChangePasswordTokenTest {
    @Test
    public void testToken() {
        String user = "britta@hotmail.org";
        String password = "brille";
        byte[] salt = "jdhUhj\\?".getBytes();
        ChangePasswordToken changePasswordToken = new ChangePasswordToken(user, password);
        String token = changePasswordToken.generateTokenString(salt);
        ChangePasswordToken changePasswordToken2 = ChangePasswordToken.fromTokenString(token, salt);
        assertEquals(user, changePasswordToken2.getUserid());
        assertEquals(password, changePasswordToken2.getPassword());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongSalt() {
        String user = "britta@hotmail.org";
        String password = "brille";
        byte[] salt = "jdhUhj\\?".getBytes();
        ChangePasswordToken changePasswordToken = new ChangePasswordToken(user, password);
        String token = changePasswordToken.generateTokenString(salt);
        ChangePasswordToken.fromTokenString(token, "salt".getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testtimedoutToken() {
        ChangePasswordToken.fromTokenString("bmFsbGU6MTMwMTgzMDE1NjcyNjpVMTkxUjFOMVpnSnJlbVZlVUVoNlRINVlkazU4ZG5ZQWZFaHlUSDltYWdkOFdHb0o", "1234".getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFakeToken() {
        ChangePasswordToken.fromTokenString("dilldall", "salt".getBytes());
    }
}
