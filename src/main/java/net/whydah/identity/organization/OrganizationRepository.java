package net.whydah.identity.organization;

import net.whydah.identity.dataimport.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrganizationRepository {
    private final static Logger log = LoggerFactory.getLogger(OrganizationRepository.class);

    private final OrganizationDao organizationDao;

    @Autowired
    public OrganizationRepository(OrganizationDao organizationDao) {
        this.organizationDao = organizationDao;
    }

    public void addOrganization(Organization organization) {
        if (organization != null) {
            Organization org = filterOrganizationFromList(organization.getAppId(), organization.getName());
            if (org != null) {
                log.warn("Organization {}-{} already exists", organization.getAppId(), organization.getName());
            } else {
                boolean inserted = organizationDao.create(organization);
                if (inserted) {
                    log.info("Organization {}-{} inserted", organization.getAppId(), organization.getName());
                }
            }
        }
   }

    public Organization getOrganization(String appId, String name) {
        Organization organization = filterOrganizationFromList(appId, name);
        return organization;
    }

    public List<Organization> getOrganizations(String appId) {
        return organizationDao.getOrganization(appId);
    }

    public void deleteOrganization(String appId, String name) {
        boolean deleted = organizationDao.delete(appId, name);
        if (deleted) {
            log.info("Organization {}-{} deleted", appId, name);
        } else {
            log.info("Organization {}-{} not deleted", appId, name);
        }
    }

    public void updateOrganization(String appId, Organization organization) throws RuntimeException {
        Organization exists = filterOrganizationFromList(appId, organization.getName());
        if (exists != null) {
            boolean updated = organizationDao.update(organization);
            if (updated) {
                log.info("Organization {}-{} updated", appId, organization.getName());
            }
        } else {
            log.info("Organization {}-{} not found. Could not update", appId, organization.getName());
        }
    }

    private final Organization filterOrganizationFromList(String appId, String name) {
        List<Organization> organizations = organizationDao.getOrganization(appId);
        if (organizations != null) {
            return organizations.stream().filter(o -> o.getAppId().equals(appId) && o.getName().equalsIgnoreCase(name))
                    .findAny()
                    .orElse(null);
        }
        return null;
    }
}
