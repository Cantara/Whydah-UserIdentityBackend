package net.whydah.identity.user.identity;

public class InvalidUserIdentityFieldException extends RuntimeException {
    InvalidUserIdentityFieldException(String field, String value) {
        super("Invalid " + field + " : " + value);
    }
}
