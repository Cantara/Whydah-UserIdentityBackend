package net.whydah.identity.user.resource;

import java.util.HashMap;

public class MockMail {
    private final HashMap<String, String> passwords = new HashMap<String, String>();

    public void sendpasswordmail(String to, String user, String token) {
        passwords.put(user, token);
        System.out.println("Sending mocked mail to " + to + " with token " + token);
    }

    public String getToken(String to) {
        return passwords.get(to);
    }
}
