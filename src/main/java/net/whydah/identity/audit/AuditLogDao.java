package net.whydah.identity.audit;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogDao {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public AuditLogDao(BasicDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void store(ActionPerformed actionPerformed) {
        String sql = "INSERT INTO AUDITLOG (userid, timestamp, action, field, value) values (?,?,?,?,?)";

        jdbcTemplate.update(sql,
                actionPerformed.getUserId(),
                actionPerformed.getTimestamp(),
                actionPerformed.getAction(),
                actionPerformed.getField(),
                actionPerformed.getValue()
        );
    }
}
