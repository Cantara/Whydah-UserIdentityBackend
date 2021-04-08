package net.whydah.identity.user.identity;

import net.whydah.identity.application.ApplicationDao;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RDBMSLdapUserIdentityDao {
    private static final Logger log = LoggerFactory.getLogger(ApplicationDao.class);

    private static String USER_IDENTITY_SQL = "SELECT id, username, firstname, lastname, personref, email, cellphone, password from UserIdentity WHERE id=?";
    private static String USERNAME_SQL = "SELECT id, username, firstname, lastname, personref, email, cellphone, password from UserIdentity WHERE username=?";

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public RDBMSLdapUserIdentityDao(BasicDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        String jdbcDriverString = dataSource.getDriverClassName();
        if (jdbcDriverString.contains("mysql")) {
            log.warn("TODO update sql migration script");
            USER_IDENTITY_SQL = "SELECT id, username, firstname, lastname, personref, email, cellphone, password from UserIdentity WHERE Id=? GROUP BY Id";
            USERNAME_SQL = "SELECT id, username, firstname, lastname, personref, email, cellphone, password from UserIdentity WHERE username=? GROUP BY username";
        }
    }

    void create(RDBMSUserIdentity userIdentity) {
        String sql = "INSERT INTO UserIdentity (id, username, firstname, lastname, personref, email, cellphone, password) VALUES (?,?,?,?,?,?,?,?)";
        int numRowsAffected = jdbcTemplate.update(sql,
                userIdentity.getUid(),
                userIdentity.getUsername(),
                userIdentity.getFirstName(),
                userIdentity.getLastName(),
                userIdentity.getPersonRef(),
                userIdentity.getEmail(),
                userIdentity.getCellPhone(),
                userIdentity.getPassword()
        );
        if (numRowsAffected != 1) {
            throw new RuntimeException(String.format("Failed to insert new useridentity {%s}", userIdentity));
        }
    }

    void delete(String uuid) throws RuntimeException {
        String sql = "DELETE FROM UserIdentity WHERE id=?";
        int numRowsAffected = jdbcTemplate.update(sql, uuid);
        if (numRowsAffected != 1) {
            throw new RuntimeException(String.format("Failed to delete useridentity with uid %s", uuid));
        }
    }

    RDBMSUserIdentity get(String uuid) {
        List<RDBMSUserIdentity> userIdentities = jdbcTemplate.query(USER_IDENTITY_SQL, new RDBMSUserIdentityRowMapper(), uuid);
        if (userIdentities != null) {
            RDBMSUserIdentity userIdentity = userIdentities.stream().findAny().get();
            return userIdentity;
        } else {
            return null;
        }
    }

    RDBMSUserIdentity getWithUsername(String username) {
        List<RDBMSUserIdentity> userIdentities = jdbcTemplate.query(USERNAME_SQL, new RDBMSUserIdentityRowMapper(), username);
        if (userIdentities != null) {
            RDBMSUserIdentity userIdentity = userIdentities.stream().findAny().get();
            return userIdentity;
        } else {
            return null;
        }
    }

    public void updatePassword(String username, String password) throws RuntimeException {
        String sql = "UPDATE UserIdentity SET password=? WHERE username=?";
        int numRowsAffected = jdbcTemplate.update(sql, password, username);
        if (numRowsAffected != 1) throw new RuntimeException(String.format("Failed to update password for user %s"));
    }

    public void update(String uid, RDBMSUserIdentity newUserIdentity) throws RuntimeException {
        String sql = "UPDATE UserIdentity SET firstname=?, lastname=?, personref=?, email=?, cellphone=? WHERE id=?";
        int numRowsAffected = jdbcTemplate.update(sql,
                newUserIdentity.getFirstName(),
                newUserIdentity.getLastName(),
                newUserIdentity.getPersonRef(),
                newUserIdentity.getEmail(),
                newUserIdentity.getCellPhone(),
                uid);

        if (numRowsAffected != 1) throw new RuntimeException(String.format("Failed to update useridentity for user %s with input {%s}", uid, newUserIdentity));
    }
}
