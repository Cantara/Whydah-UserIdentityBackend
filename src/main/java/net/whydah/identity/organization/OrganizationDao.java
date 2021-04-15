package net.whydah.identity.organization;

import net.whydah.identity.dataimport.Organization;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

import static net.whydah.sso.util.LoggerUtil.first50;

@Repository
public class OrganizationDao {
    private static final Logger log = LoggerFactory.getLogger(OrganizationDao.class);

    private String ORGANIZATIONS_SQL = "SELECT id, name from Organization";
    private String ORGANIZATION_SQL = "SELECT Id, name from Organization WHERE Id=?";

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public OrganizationDao(BasicDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        String jdbcDriverString = dataSource.getDriverClassName();
        if (jdbcDriverString.contains("mysql")) {
            log.warn("TODO update sql migration script");
            ORGANIZATIONS_SQL = "SELECT Id, name from Organization GROUP BY Id ORDER BY Name ASC";
            ORGANIZATION_SQL =  "SELECT Id, name from Organization WHERE Id=? GROUP BY Id";
        }
    }

    /**
     *
     */
    boolean create(Organization newOrganization) {
        boolean executionOk = false;
        String sql = "INSERT INTO Organization (id, name) VALUES (?,?)";
        int numRowsAffected = jdbcTemplate.update(sql, newOrganization.getAppId(), newOrganization.getName());
        if (numRowsAffected > 0) {
            executionOk = true;
        }
        return executionOk;
    }

    List<Organization> getOrganization(String appId) {
        List<Organization> organizations = jdbcTemplate.query(ORGANIZATION_SQL, new String[]{appId.trim()}, new OrganizationRowMapper());
        if (organizations.isEmpty()) {
            log.info("No Organization found for applicationId [{}]", appId);
            return null;
        }

        log.trace("Organization found {}", first50(organizations));
        return organizations;
    }

    public List<Organization> getOrganizations() {
        return this.jdbcTemplate.query(ORGANIZATIONS_SQL, new OrganizationRowMapper());
    }

    boolean update(Organization organization) {
        boolean executionOk = false;
        String sql = "UPDATE Organization set name=? WHERE ID=?";
        int numRowsAffected = jdbcTemplate.update(sql, organization.getName(), organization.getAppId().trim());
        if (numRowsAffected > 0) {
            executionOk = true;
        }
        return executionOk;
    }

    boolean delete(String applicationId, String name) {
        boolean executionOk = false;
        String sql = "DELETE FROM Organization WHERE ID=? AND NAME=?";
        int numRowsAffected = jdbcTemplate.update(sql, applicationId.trim(), name);
        if (numRowsAffected > 0) {
            executionOk = true;
        }
        return executionOk;
    }
}
