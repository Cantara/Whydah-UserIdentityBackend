package net.whydah.identity.user.role;

public class DatastoreException extends RuntimeException {
    public DatastoreException() {
        super();
    }

    public DatastoreException(String message) {
        super(message);
    }

    public DatastoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatastoreException(Throwable cause) {
        super(cause);
    }
}
