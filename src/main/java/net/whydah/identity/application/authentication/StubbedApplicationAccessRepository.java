package net.whydah.identity.application.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by baardl on 22.03.14.
 */
//@Path("/application")
public class StubbedApplicationAccessRepository implements ApplicationAccessRepository {
    private static final Logger logger = LoggerFactory.getLogger(StubbedApplicationAccessRepository.class);


    @Override
    public boolean hasAccess(String appId) {
        //TODO baardl implement access control for app.
        return true;
    }

    @Override
    public ApplicationToken addApplication(String appId, String name, String publicCryptoKey) {
        return null;
    }
}
