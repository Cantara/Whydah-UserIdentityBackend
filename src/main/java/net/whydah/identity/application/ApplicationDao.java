package net.whydah.identity.application;

import com.google.common.base.Joiner;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

//TODO Decide strategy to handle different SQL queries for different databases. Inheritance to support variantions from generic?
/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a>
 */
@Repository
public class ApplicationDao {
    private static final Logger log = LoggerFactory.getLogger(ApplicationDao.class);

    private static String APPLICATIONS_SQL = "SELECT Id, Name, Secret, AvailableOrgNames, DefaultRoleName, DefaultOrgName from Application";
    private static String APPLICATION_SQL =  "SELECT Id, Name, Secret, AvailableOrgNames, DefaultRoleName, DefaultOrgName from Application WHERE id=?";

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public ApplicationDao(BasicDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        String jdbcDriverString = dataSource.getDriverClassName();
        if (jdbcDriverString.contains("mysql")) {
            APPLICATIONS_SQL = "SELECT Id, Name, Secret, AvailableOrgNames, DefaultRoleName, DefaultOrgName from Application GROUP BY Id ORDER BY Name ASC";
            APPLICATION_SQL =  "SELECT Id, Name, Secret, AvailableOrgNames, DefaultRoleName, DefaultOrgName from Application WHERE Id=? GROUP BY Id";
        }
    }


    public Application getApplication(String appid) {
        List<Application> applications = jdbcTemplate.query(APPLICATION_SQL, new String[]{appid}, new ApplicationMapper());
        if (applications.isEmpty()) {
            return null;
        }
        Application application = applications.get(0);
        List<Role> roles = findRolesForApplication(application.getId());
        application.setAvailableRoles(roles);
        return application;
    }

    private List<Role> findRolesForApplication(String applicationId) {
        String sql = "SELECT Id, Name from Role where applicationId=?";
        List<Role> roles = jdbcTemplate.query(sql, new String[]{applicationId}, new RoleMapper());
        return roles;
    }

    public List<Application> getApplications() {
        return this.jdbcTemplate.query(APPLICATIONS_SQL, new ApplicationMapper());
    }


    /**
     * @param application
     * @return application, with new Id inserted.
     */
    public Application create(Application application) {
        Application applicationStored = null;
        String sql = "INSERT INTO Application (ID, Name, Secret, AvailableOrgNames, DefaultRoleName, DefaultOrgName) VALUES (?,?,?,?,?,?)";
        String availableOrgNames = Joiner.on(",").join(application.getAvailableOrgNames());
        int numRowsUpdated = jdbcTemplate.update(sql, application.getId(), application.getName(), application.getSecret(),
                availableOrgNames, application.getDefaultRoleName(), application.getDefaultOrgName());

        String roleSql = "INSERT INTO Role(Id, Name, applicationId) VALUES (?,?,?)";
        for (Role role : application.getAvailableRoles()) {
            jdbcTemplate.update(roleSql, role.getId(), role.getName(), application.getId());
        }

        if (numRowsUpdated > 0) {
            applicationStored = getApplication(application.getId());    //Why extra roundtrip when UIB choose ID?
            log.trace("Created application {}, numRowsUpdated {}", applicationStored.toString(), numRowsUpdated);
        }
        return applicationStored;
    }


    public int countApplications() {
        String sql = "SELECT count(*) FROM Application";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        log.debug("applicationCount={}", count);
        return count;
    }

    private static final class ApplicationMapper implements RowMapper<Application> {
        public Application mapRow(ResultSet rs, int rowNum) throws SQLException {
            Application application = new Application(rs.getString("Id"), rs.getString("Name"));
            application.setSecret(rs.getString("Secret"));
            String availableOrgNames = rs.getString("availableOrgNames");
            if (availableOrgNames != null) {
                application.setAvailableOrgNames(Arrays.asList(availableOrgNames.split(",")));
            }
            application.setDefaultRoleName(rs.getString("DefaultRoleName"));
            application.setDefaultOrgName(rs.getString("DefaultOrgName"));
            return application;
        }
    }

    private static final class RoleMapper implements RowMapper<Role> {
        public Role mapRow(ResultSet rs, int rowNum) throws SQLException {
            Role role = new Role(rs.getString("Id"), rs.getString("Name"));
            return role;
        }
    }
}
