package net.whydah.identity.security;

import net.whydah.identity.application.ApplicationCredentialRepository;
import net.whydah.sso.application.types.ApplicationCredential;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 22.11.15.
 */
@Service
public class AuthenticationService {
    private static final Logger log = getLogger(AuthenticationService.class);

    private final ApplicationCredentialRepository appCredRepo;

    @Autowired
    public AuthenticationService(ApplicationCredentialRepository appCredRepo) {
        this.appCredRepo = appCredRepo;
    }

    /**
     * Validate UAS application credential.
     * @param applicationCredential
     * @return true is applicationCredential match that of UAS.
     */
    public boolean isAuthenticatedAsUAS(ApplicationCredential applicationCredential) {
        ApplicationCredential uasApplicationCredential = appCredRepo.getUasAppCred();
        boolean isAuthenticated = false;

       
        if (applicationCredential != null && uasApplicationCredential != null) {
        	 log.info("Comparing applicationCredential:{} - with uasCredential:{}", applicationCredential.toString(), uasApplicationCredential.toString());
        	if (applicationCredential.getApplicationID().equals(uasApplicationCredential.getApplicationID()) &&
                    applicationCredential.getApplicationSecret().equals(uasApplicationCredential.getApplicationSecret())) {
                isAuthenticated = true;
            }
        }
        log.trace("AppCredential verification result:"+isAuthenticated);
        return isAuthenticated;
    }
}
