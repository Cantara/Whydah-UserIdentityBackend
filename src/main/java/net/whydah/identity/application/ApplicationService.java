package net.whydah.identity.application;

import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    private final ApplicationDao applicationDao;
    private final AuditLogDao auditLogDao;

    @Autowired
    public ApplicationService(ApplicationDao applicationDao, AuditLogDao auditLogDao) {
        this.applicationDao = applicationDao;
        this.auditLogDao = auditLogDao;
    }

    ////// CRUD

    public Application create(Application application) {
        return create(UUID.randomUUID().toString(), application);
    }
    //used by ApplicationImporter, should be remove later
    public Application create(String applicationId, Application application) {
        application.setId(applicationId);
        int numRowsAffected = applicationDao.create(application);
        audit(ActionPerformed.ADDED, application.getId() + ", " + application.getName());
        return application;
    }

    public Application getApplication(String applicationId) {
        return applicationDao.getApplication(applicationId);
    }

    public List<Application> getApplications() {
        return applicationDao.getApplications();
    }

    public int update(Application application) {
        int numRowsAffected = applicationDao.update(application);
        audit(ActionPerformed.MODIFIED, application.getId() + ", " + application.getName());
        return numRowsAffected;
    }

    public int delete(String applicationId) {
        int numRowsAffected = applicationDao.delete(applicationId);
        audit(ActionPerformed.DELETED, applicationId);
        return numRowsAffected;
    }

    private void audit(String action, String value) {
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(value, now, action, "application", value);
        auditLogDao.store(actionPerformed);
    }


    ////// Authentication

    public Application authenticate(ApplicationCredential credential) {
        Application application = applicationDao.getApplication(credential.getApplicationID());
        if (application == null) {
            return null;
        }
        String applicationSecret = application.getSecurity().getSecret();
        if (applicationSecret == null || applicationSecret.isEmpty()) {
            log.warn("secret is not set for applicationId=, name={}, all attempts at authentication will fail.", application.getId(), application.getName());
            return null;
        }
        if (applicationSecret.equals(credential.getApplicationSecret())) {
            return application;
        }
        return null;
    }
}
