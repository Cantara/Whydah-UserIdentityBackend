package net.whydah.identity.application.authentication;

/**
 * Created by baardl on 22.03.14.
 */
public interface ApplicationAccessRepository {

    boolean hasAccess(String appId);

    ApplicationToken addApplication(String appId, String name, String publicCryptoKey);
}
