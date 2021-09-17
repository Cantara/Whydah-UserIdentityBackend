package net.whydah.identity.user.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

class RDBMSUserIdentityRowMapper implements RowMapper<RDBMSUserIdentity> {
    private static final Logger log = LoggerFactory.getLogger(RDBMSUserIdentityRowMapper.class);

    public RDBMSUserIdentity mapRow(ResultSet rs, int rowNum) throws SQLException {
        RDBMSUserIdentity userIdentity = null;
        try {
            if (rs == null) {
                log.info("RDBMS User Identity - No resultset found.");
            } else {
                String uid = rs.getString("id");
                String username = rs.getString("username");
                String firstname = rs.getString("firstname");
                String lastname = rs.getString("lastname");
                String email = rs.getString("email");
                String cellphone = rs.getString("cellphone");
                String password = rs.getString("password");
                String personRef = rs.getString("personref");

                userIdentity = new RDBMSUserIdentity(uid, username, firstname, lastname, email, password, cellphone, personRef);
            }
        } catch (Exception e) {
            throw new SQLException("Failed to map result from db", e);
        }
        return userIdentity;
    }
}