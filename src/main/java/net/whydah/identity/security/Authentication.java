package net.whydah.identity.security;


import net.whydah.identity.user.authentication.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds current authenticated user in a threadlocal.
 */
public final class Authentication {
    private static final Logger log = LoggerFactory.getLogger(Authentication.class);

    private static final ThreadLocal<UserToken> authenticatedUser = new ThreadLocal<>();

    public static void setAuthenticatedUser(UserToken userToken) {
        log.debug("setAuthenticatedUser with userToken: {}", userToken);
        authenticatedUser.set(userToken);
    }

    public static UserToken getAuthenticatedUser() {
        return authenticatedUser.get();
    }

    public static void clearAuthentication() {
        authenticatedUser.remove();
    }

    private Authentication(){
    }
}
