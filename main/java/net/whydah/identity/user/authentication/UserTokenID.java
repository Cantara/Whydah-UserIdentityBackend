package net.whydah.identity.user.authentication;


import net.whydah.identity.user.UserRole;

import java.util.List;

public class UserTokenID {

    public String getId() {
        return id;
    }

    private final String id;

    public UserTokenID(String id) {
        this.id = id;
    }

}
