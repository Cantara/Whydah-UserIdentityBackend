package net.whydah.identity.organization;

import net.whydah.identity.dataimport.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrganizationRowMapper implements RowMapper<Organization> {
    private static final Logger log = LoggerFactory.getLogger(OrganizationRowMapper.class);

    public Organization mapRow(ResultSet rs, int rowNum) throws SQLException {
        try {
            if (rs == null) {
                log.info("Organization - No resultset found.");
            } else {
                String uid = rs.getString("id");
                String name = rs.getString("name");

                Organization organization = new Organization(uid, name);
                return organization;
            }
        } catch (Exception e) {
            log.error("Failed to map Organization", e);
        }
        return null;
    }
}