package net.whydah.identity.audit;


public class ActionPerformed {
    public static final String ADDED = "added";
    public static final String MODIFIED = "modified";
    public static final String DELETED = "deleted";

    private final String userId;
    private final String timestamp;

    private final String action;
    private final String field;
    private final String value;
    //oldValue osgå?


    public ActionPerformed(String userId, String timestamp, String action, String field, String value) {
        this.userId = userId;
        this.timestamp = timestamp;
        this.action = action;
        this.field = field;
        this.value = value;
    }

    public String getUserId() {
        return userId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getAction() {
        return action;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
