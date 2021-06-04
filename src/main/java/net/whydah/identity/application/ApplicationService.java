package net.whydah.identity.application;

import net.whydah.identity.application.search.LuceneApplicationIndexer;
import net.whydah.identity.application.search.LuceneApplicationSearch;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.health.HealthResource;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.util.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;


@Service
public class ApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    private final ApplicationDao applicationDao;
    private final AuditLogDao auditLogDao;
    private final LuceneApplicationIndexer luceneApplicationIndexer;
    private final LuceneApplicationSearch luceneApplicationSearch;

    @Autowired
    public ApplicationService(ApplicationDao applicationDao, AuditLogDao auditLogDao, @Qualifier("luceneApplicationsDirectory") LuceneApplicationIndexer luceneApplicationIndexer, LuceneApplicationSearch luceneApplicationSearch) {
        this.applicationDao = applicationDao;
        this.auditLogDao = auditLogDao;
        this.luceneApplicationIndexer = luceneApplicationIndexer;
        this.luceneApplicationSearch = luceneApplicationSearch;
    }

    ////// CRUD

    public Application create(Application application) {
    	if(application.getId()==null||application.getId().isEmpty()){
    		return create(UUID.randomUUID().toString(), application);
    	} else {
    		if(getApplication(application.getId())!=null){
    			return null; //The id is already existing
    		} else {
    			return create(application.getId(), application);		
    		}
    	}
    }
    //used by ApplicationImporter, should be remove later
    public Application create(String applicationId, Application application) {
        application.setId(applicationId);
        boolean success = applicationDao.create(application);
        if(success) {
        	luceneApplicationIndexer.addToIndex(application);
        }
        audit(ActionPerformed.ADDED, application.getId() + ", " + application.getName());
        return application;
    }

    public List<Application> search(String applicationQuery) {
    	List<Application> list = luceneApplicationSearch.search(applicationQuery);
    	log.debug("lucene search with query={} returned {} apps.", applicationQuery, list.size());
		importApplicationsIfEmpty();
    	return list;
    }

    public Application getApplication(String applicationId) {
        return applicationDao.getApplication(applicationId);
    }
    
    Lock importLock = new Lock();
    
    //TODO: this should only be called by internal applications
    public List<Application> getApplications() {
        //data is queried directly from DB instead of LUCENE index
        List<Application> applicationDBList = applicationDao.getApplications();
        Map<String, Application> uniqueueApplications = new HashMap<>();
        for (Application a : applicationDBList) {
            uniqueueApplications.put(a.getId(), a);
        }
        List<Application> uniqueApplicationDBList = new ArrayList<Application>(uniqueueApplications.values());
        importApplicationsIfEmpty(uniqueApplicationDBList);
        return uniqueApplicationDBList;
    }

    public int update(Application application) {
        int numRowsAffected = applicationDao.update(application);
        luceneApplicationIndexer.updateIndex(application);
        audit(ActionPerformed.MODIFIED, application.getId() + ", " + application.getName());
        return numRowsAffected;
    }

    public int delete(String applicationId) {
        int numRowsAffected = applicationDao.delete(applicationId);
        luceneApplicationIndexer.removeFromIndex(applicationId);
        audit(ActionPerformed.DELETED, applicationId);
        return numRowsAffected;
    }

    private void audit(String action, String value) {
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(value, now, action, "application", value);
        auditLogDao.store(actionPerformed);
    }

    private void importApplicationsIfEmpty() {
    	List<Application> applicationDBList = applicationDao.getApplications();
    	importApplicationsIfEmpty(applicationDBList);
    }

    private void importApplicationsIfEmpty(List<Application> applicationDBList) {
    	if(!importLock.isLocked()){
    		try {
    			importLock.lock();
    			if(luceneApplicationSearch.getApplicationIndexSize()==0){
    				new Thread(new Runnable() {

    					@Override
    					public void run() {

    						log.debug("lucene index is empty. Trying to import from DB...");
    						try {
    							List<Application> clones = new ArrayList<Application>(applicationDBList);
    							log.debug("Found application list size: {}", applicationDBList.size());
    							luceneApplicationIndexer.addToIndex(clones);
    							
    							HealthResource.setNumberOfApplications(luceneApplicationSearch.getApplicationIndexSize());
    						
    						} catch (Exception e) {
    							log.error("failed to import applications, exception: " + e);
    						}

    					}
    				}).start();
    			}    
    		}
    		catch (InterruptedException e) {

    		} finally{
    			importLock.unlock();
    		}
    	}
    }
	
    ////// Authentication

    /**
     * @param credential    the application credential to verify
     * @return  application if successful authentication, else null
     */
    public Application authenticate(ApplicationCredential credential) {
        if (credential == null) {
            log.warn("authenticate - Missing ApplicationCredential ");
            return null;
        }
        //List<Application> applications = applicationDao.getApplications();
        Application application = applicationDao.getApplication(credential.getApplicationID().trim());
        if (application == null) {
            log.warn("authenticate - ApplicationID:{}- name={} - Not Found", credential.getApplicationID(), application.getName());
            return null;
        }
        String applicationSecret = application.getSecurity().getSecret().trim();
        if (applicationSecret == null || applicationSecret.isEmpty()) {
            log.warn("secret is not set for applicationId=, name={}, all attempts at authentication will fail.", application.getId(), application.getName());
            return null;
        }
        if (applicationSecret.equals(credential.getApplicationSecret().trim())) {
            return application;
        }
        log.warn("authenticate - Incorrect applicationSectret ");
        return null;
    }
}
