package net.whydah.identity.application;

import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static net.whydah.sso.util.LoggerUtil.first50;


//TODO Decide strategy to handle different SQL queries for different databases. Inheritance to support variations from generic?
/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a>
 */
@Repository
public class ApplicationDao {
    private static final Logger log = LoggerFactory.getLogger(ApplicationDao.class);

    private static String APPLICATIONS_SQL = "SELECT id, json from Application";
    private static String APPLICATION_SQL =  "SELECT id, json from Application WHERE id=?";

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public ApplicationDao(BasicDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        String jdbcDriverString = dataSource.getDriverClassName();
        if (jdbcDriverString.contains("mysql")) {
            log.warn("TODO update sql migration script");
            APPLICATIONS_SQL = "SELECT Id, json from Application GROUP BY Id ORDER BY Name ASC";
            APPLICATION_SQL =  "SELECT Id, json from Application WHERE Id=? GROUP BY Id";
        }
    }


    int create(Application application) {
        String json = ApplicationMapper.toJson(application);
        String sql = "INSERT INTO Application (id, json) VALUES (?,?)";
        int numRowsAffected = jdbcTemplate.update(sql, application.getId().trim(), json);
        return numRowsAffected;

        /*
        Application applicationStored = null;
        String sql = "INSERT INTO Application (ID, Name, Secret, AvailableOrgNames, DefaultRoleName, DefaultOrgName) VALUES (?,?,?,?,?,?)";
        String availableOrgNames = String.join(",", application.getOrganizationNames());
        int numRowsUpdated = jdbcTemplate.update(sql, application.getId(), application.getName(), application.getSecret(),
                availableOrgNames, application.getDefaultRoleName(), application.getDefaultOrganizationName());

        String roleSql = "INSERT INTO Role(Id, Name, applicationId) VALUES (?,?,?)";
        for (Role role : application.getRoles()) {
            jdbcTemplate.update(roleSql, role.getId(), role.getName(), application.getId());
        }

        if (numRowsUpdated > 0) {
            applicationStored = getApplication(application.getId());    //Why extra roundtrip when UIB choose ID?
            log.trace("Created application {}, numRowsUpdated {}", applicationStored.toString(), numRowsUpdated);
        }
        return applicationStored;
        */
    }

    Application getApplication(String applicationId) {
        log.debug("Get Application for applicationId [{}]", applicationId);
        List<Application> applications = jdbcTemplate.query(APPLICATION_SQL, new String[]{applicationId.trim()}, new ApplicationMapper2());
        if (applications.isEmpty()) {
            log.info("No Applciation found for applicationId [{}]", applicationId);
            return null;
        }
        Application application = applications.get(0);
        log.trace("Application found {}", first50(application));
        return application;
    }

    public List<Application> getApplications() {
        return this.jdbcTemplate.query(APPLICATIONS_SQL, new ApplicationMapper2());
    }

    int update(Application application) {
        String json = ApplicationMapper.toJson(application);
        String sql = "UPDATE Application set Json=? WHERE ID=?";
        int numRowsAffected = jdbcTemplate.update(sql, json, application.getId().trim());
        return numRowsAffected;
    }

    int delete(String applicationId) {
        String sql = "DELETE FROM Application WHERE ID=?";
        int numRowsAffected = jdbcTemplate.update(sql, applicationId.trim());
        return numRowsAffected;
    }

    //used by ApplicationDaoTest
    int countApplications() {
        String sql = "SELECT count(id) FROM Application";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        log.debug("applicationCount={}", count);
        return count;
    }

    private static final class ApplicationMapper2 implements RowMapper<Application> {
        public Application mapRow(ResultSet rs, int rowNum) throws SQLException {

            String json = rs.getString("json");
            Application application;
            try {
                if (rs == null) {
                    log.info("Application - No resultset found.");
                } else if (json == null || json.isEmpty()) {
                    log.warn("Application - No data found for application id {}", rs.getString(0));
                }
                log.trace("Application Json before mapper {}", first50(json));
                application = ApplicationMapper.fromJson(json);
                log.trace("Application after mapper {}", first50(application));
            } catch (Exception e) {
                log.warn("Application2 - Unable to parse json for application, will attempt fallback. Json:", json);
                log.info("Application2 Json before mapper {}", first50(json));
                json = json.replace("0000", "0");
                application = ApplicationMapper.fromJson(json);
                log.warn("Application2 after mapper {}", first50(application));
            }
            return application;
        }
    }
}
