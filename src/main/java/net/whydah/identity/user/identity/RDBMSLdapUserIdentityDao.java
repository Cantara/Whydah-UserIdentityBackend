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

    int create(LDAPUserIdentity ldapUserIdentity) {
        String sql = "INSERT INTO UserIdentity (id, username, firstname, lastname, personref, email, cellphone, password) VALUES (?,?,?,?,?,?,?,?)";
        int numRowsAffected = jdbcTemplate.update(sql,
                ldapUserIdentity.getUid(),
                ldapUserIdentity.getUsername(),
                ldapUserIdentity.getFirstName(),
                ldapUserIdentity.getLastName(),
                ldapUserIdentity.getPersonRef(),
                ldapUserIdentity.getEmail(),
                ldapUserIdentity.getCellPhone(),
                ldapUserIdentity.getPassword()
        );
        return numRowsAffected;
    }

    int delete(String uuid) {
        String sql = "DELETE FROM UserIdentity WHERE id=?";
        int numRowsAffected = jdbcTemplate.update(sql, uuid);
        return numRowsAffected;
    }

    LDAPUserIdentity get(String uuid) {
        List<LDAPUserIdentity> query = jdbcTemplate.query(USER_IDENTITY_SQL, new RDBMSLdapUserIdentityMapper(), uuid);
        if (query != null && !query.isEmpty()) {
            return query.stream().findAny().get();
        } else {
            return null;
        }
    }

    LDAPUserIdentity getWithUsername(String username) {
        List<LDAPUserIdentity> query = jdbcTemplate.query(USERNAME_SQL, new RDBMSLdapUserIdentityMapper(), username);
        if (query != null && !query.isEmpty()) {
            return query.stream().findAny().get();
        } else {
            return null;
        }
    }

}