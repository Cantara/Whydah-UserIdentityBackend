package net.whydah.identity.user;

public class NonExistentRoleException extends RuntimeException {
    public NonExistentRoleException(String message) {
        super(message);
    }
}
