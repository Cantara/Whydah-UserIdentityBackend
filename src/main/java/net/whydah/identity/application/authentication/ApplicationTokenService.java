package net.whydah.identity.application.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Created by baardl on 29.03.14.
 */
@Service
public class ApplicationTokenService {
    private static final Logger log = LoggerFactory.getLogger(ApplicationTokenService.class);

    public boolean verifyApplication(String applicationTokenId) {
        log.warn("verifyApplication with applicationTokenId={} is always returning true!!! FIXME Bli", applicationTokenId);
        return true;
    }
}
