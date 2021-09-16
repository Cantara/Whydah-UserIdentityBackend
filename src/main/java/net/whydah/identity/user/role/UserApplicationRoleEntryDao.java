package net.whydah.identity.user.role;

import net.whydah.sso.user.types.UserApplicationRoleEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Responsible for fetching any user roles or properties stored in RDBMS.
 * Backed by spring-jdbc, http://docs.spring.io/spring/docs/current/spring-framework-reference/html/jdbc.html
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-01-18
 */
@Repository
public class UserApplicationRoleEntryDao {
    private static final Logger log = LoggerFactory.getLogger(UserApplicationRoleEntryDao.class);
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public UserApplicationRoleEntryDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }


    public UserApplicationRoleEntry getUserApplicationRoleEntry(String roleId) {
        log.debug("getUserApplicationRoleEntry for roleId {}", roleId);
        String sql = "SELECT RoleID, UserID, AppID, OrganizationName, RoleName, RoleValues FROM UserRoles WHERE RoleID=?";
        List<UserApplicationRoleEntry> roles = jdbcTemplate.query(sql, new String[]{roleId}, new UserApplicationRoleEntryMapper());
        if (roles.isEmpty()) {
            return null;
        }
        return roles.get(0);
    }

    public UserApplicationRoleEntry getUserApplicationRoleEntryByValues(String UserID, String AppID, String OrganizationName, String RoleName, String RoleValues) {
        log.debug("getUserApplicationRoleEntryByValues for UserID '{}', AppID '{}', OrganizationName '{}', RoleName '{}', RoleValues '{}'", UserID, AppID, OrganizationName, RoleName, RoleValues);
        String sql = "SELECT RoleID, UserID, AppID, OrganizationName, RoleName, RoleValues FROM UserRoles WHERE UserID=? AND AppID=? AND OrganizationName=? AND RoleName=? AND RoleValues=?";
        List<UserApplicationRoleEntry> roles = jdbcTemplate.query(sql, new Object[]{UserID, AppID, OrganizationName, RoleName, RoleValues}, new UserApplicationRoleEntryMapper());
        if (roles.isEmpty()) {
            return null;
        }
        return roles.get(0);
    }


    public List<UserApplicationRoleEntry> getUserApplicationRoleEntries(String uid) {
        String sql = "SELECT RoleID, UserID, AppID, OrganizationName, RoleName, RoleValues FROM UserRoles WHERE UserID=?";
        List<UserApplicationRoleEntry> roles = this.jdbcTemplate.query(sql, new String[]{uid}, new UserApplicationRoleEntryMapper());
        log.debug("Found {} roles for uid={}", (roles != null ? roles.size() : "null"), uid);
        return roles;
    }


    private static final class UserApplicationRoleEntryMapper implements RowMapper<UserApplicationRoleEntry> {
        public UserApplicationRoleEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserApplicationRoleEntry userApplicationRoleEntry = new UserApplicationRoleEntry();
            userApplicationRoleEntry.setId(rs.getString("RoleID").trim());
            userApplicationRoleEntry.setUserId(rs.getString("UserID").trim());
            userApplicationRoleEntry.setApplicationId(rs.getString("AppID"));
            userApplicationRoleEntry.setOrgName(rs.getString("OrganizationName"));
            userApplicationRoleEntry.setRoleName(rs.getString("RoleName"));
            //userPropertyAndRole.setApplicationRoleValue(null2empty(rs.getString("RoleValues")));
            userApplicationRoleEntry.setRoleValue(rs.getString("RoleValues"));

            return userApplicationRoleEntry;
        }
        /*
        private String null2empty(String in) {
            return in != null ? in : "";
        }
        */
    }

    public int countUserRolesInDB() {
        String sql = "SELECT count(*) FROM UserRoles";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        log.trace("countUserRolesInDB={}", count);
        return count;
    }


    public boolean hasRole(String uid, UserApplicationRoleEntry role) {
        List<UserApplicationRoleEntry> existingRoles = getUserApplicationRoleEntries(uid);
        for (UserApplicationRoleEntry existingRole : existingRoles) {
            log.trace("hasRole - checking existing.applicationID {} against applicationID {} " +
                            "\n & existing.getOrganizationName {} against getOrganizationName {}" +
                            "\n & existing.getApplicationRoleName {} against getApplicationRoleName {}",
                    existingRole.getApplicationId(), role.getApplicationId(),
                    existingRole.getOrgName(), role.getOrgName(),
                    existingRole.getApplicationName(), role.getApplicationName());
            boolean roleExist = existingRole.getApplicationId().equals(role.getApplicationId())
                    && existingRole.getOrgName().equals(role.getOrgName())
                    && existingRole.getRoleName().equals(role.getRoleName());
            if (roleExist) {
                return true;
            }
        }
        return false;
    }



    public void addUserApplicationRoleEntry(final UserApplicationRoleEntry userApplicationRoleEntry) {
        log.trace("addUserApplicationRoleEntry:" + userApplicationRoleEntry);
        if (hasRole(userApplicationRoleEntry.getId(), userApplicationRoleEntry)) {
            log.trace("Trying to add an existing role, ignoring");
            return;
        }

        if (userApplicationRoleEntry.getId() == null || userApplicationRoleEntry.getId().length() < 5) {
            userApplicationRoleEntry.setId(UUID.randomUUID().toString());
        }

        String sql = "INSERT INTO UserRoles (RoleID, UserID, AppID, OrganizationName, RoleName, RoleValues) values (?, ?, ?, ?, ?, ?)";
        int rows = jdbcTemplate.update(sql,
                userApplicationRoleEntry.getId(),
                userApplicationRoleEntry.getUserId(),
                userApplicationRoleEntry.getApplicationId(),
                userApplicationRoleEntry.getOrgName(),
                userApplicationRoleEntry.getRoleName(),
                userApplicationRoleEntry.getRoleValue()

        );
        log.trace("{} roles added, sql: {}", rows, userApplicationRoleEntry);
    }


    public void deleteAllRolesForUser(String uid) {
        String sql = "DELETE FROM UserRoles WHERE UserID=?";
        jdbcTemplate.update(sql, uid);
    }

    public void deleteUserRole(String uid, String roleId) {
        String sql = "DELETE FROM UserRoles WHERE UserID=? AND RoleID=?";
        jdbcTemplate.update(sql, uid, roleId);
    }

    public void deleteRoleByRoleID(String roleId) {
        String sql = "DELETE FROM UserRoles WHERE RoleID=? ";
        jdbcTemplate.update(sql, roleId);
    }

    public void deleteUserAppRoles(String uid, String appid) {
        String sql = "DELETE FROM UserRoles WHERE UserID=? AND AppID=?";
        jdbcTemplate.update(sql, uid, appid);
    }


    public void updateUserRoleValue(UserApplicationRoleEntry role) {
        String sql = "UPDATE UserRoles set RoleValues=? WHERE RoleID=? and UserID=?";
        jdbcTemplate.update(sql, role.getRoleValue(), role.getId(), role.getUserId());
    }
}

