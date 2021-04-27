package net.whydah.identity.user.identity;

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RDBMSLdapUserIdentityDao {
    private static final Logger log = LoggerFactory.getLogger(RDBMSLdapUserIdentityDao.class);

    private static String UID_SQL = "SELECT id, username, firstname, lastname, personref, email, cellphone, password from UserIdentity WHERE id=?";
    private static String USERNAME_SQL = "SELECT id, username, firstname, lastname, personref, email, cellphone, password from UserIdentity WHERE username=?";
    private static String LIST_SQL = "SELECT id, username, firstname, lastname, personref, email, cellphone, password FROM UserIdentity";

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public RDBMSLdapUserIdentityDao(BasicDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        String jdbcDriverString = dataSource.getDriverClassName();
        if (jdbcDriverString.contains("mysql")) {
            log.warn("TODO update sql migration script");
            UID_SQL = "SELECT id, username, firstname, lastname, personref, email, cellphone, password from UserIdentity WHERE Id=? GROUP BY Id";
            USERNAME_SQL = "SELECT id, username, firstname, lastname, personref, email, cellphone, password from UserIdentity WHERE username=? GROUP BY username";
            LIST_SQL = "SELECT id, username, firstname, lastname, personref, email, cellphone, password FROM UserIdentity GROUP BY Id";
        }
    }

    public boolean create(RDBMSUserIdentity userIdentity) throws RuntimeException {
        boolean executionOk = false;
        String sql = "INSERT INTO UserIdentity (id, username, firstname, lastname, personref, email, cellphone, password) VALUES (?,?,?,?,?,?,?,?)";
        try {
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
            if (numRowsAffected > 0) {
                executionOk = true;
            }
            return executionOk;
        } catch (DataAccessException e) {
            throw new RuntimeException("userIdentity create in DB failed!", e);
        }
    }

    public boolean delete(String uuid) throws RuntimeException {
        boolean executionOk = false;
        String sql = "DELETE FROM UserIdentity WHERE id=?";
        try {
            int numRowsAffected = jdbcTemplate.update(sql, uuid);

            if (numRowsAffected > 0) {
                executionOk = true;
            }
            return executionOk;
        } catch (DataAccessException e) {
            throw new RuntimeException("userIdentity delete in DB failed", e);
        }
    }

    public RDBMSUserIdentity get(String uuid) throws RuntimeException {
        try {
            List<RDBMSUserIdentity> userIdentities = jdbcTemplate.query(UID_SQL, new RDBMSUserIdentityRowMapper(), uuid);
            if (userIdentities != null && !userIdentities.isEmpty()) {
                RDBMSUserIdentity userIdentity = userIdentities.stream().findAny().get();
                return userIdentity;
            } else {
                return null;
            }
        } catch (DataAccessException e) {
            throw new RuntimeException("userIdentity get with uid failed", e);
        }
    }

    public List<RDBMSUserIdentity> allUsersList() {
         return jdbcTemplate.query(LIST_SQL, new RDBMSUserIdentityRowMapper());
    }

    public RDBMSUserIdentity getWithUsername(String username) throws RuntimeException {
        try {
            List<RDBMSUserIdentity> userIdentities = jdbcTemplate.query(USERNAME_SQL, new RDBMSUserIdentityRowMapper(), username);
            if (userIdentities != null && !userIdentities.isEmpty()) {
                RDBMSUserIdentity userIdentity = userIdentities.stream().findAny().get();
                return userIdentity;
            } else {
                return null;
            }
        } catch (DataAccessException e) {
            throw new RuntimeException("userIdentity get with username failed", e);
        }
    }

    public boolean updatePassword(String username, String password) throws RuntimeException {
        boolean executionOk = false;
        String sql = "UPDATE UserIdentity SET password=? WHERE username=?";
        try {
            int numRowsAffected = jdbcTemplate.update(sql, password, username);

            if (numRowsAffected > 0) {
                executionOk = true;
            }
            return executionOk;
        } catch (DataAccessException e) {
            throw new RuntimeException("updatePassword in DB failed!", e);
        }
    }

    public boolean update(String uid, RDBMSUserIdentity newUserIdentity) throws RuntimeException {
        boolean executionOk = false;
        String sql = "UPDATE UserIdentity SET firstname=?, lastname=?, personref=?, email=?, cellphone=? WHERE id=?";
        try {
            int numRowsAffected = jdbcTemplate.update(sql,
                    newUserIdentity.getFirstName(),
                    newUserIdentity.getLastName(),
                    newUserIdentity.getPersonRef(),
                    newUserIdentity.getEmail(),
                    newUserIdentity.getCellPhone(),
                    uid);

            if (numRowsAffected > 0) {
                executionOk = true;
            }
            return executionOk;
        } catch (DataAccessException e) {
            throw new RuntimeException("userIdentityUpdate in DB failed!", e);
        }
    }
}
