package net.whydah.identity.dataimport;

import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoConfiguration;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class FlywayVersionMigrationHelper {
    private static final Logger log = LoggerFactory.getLogger(FlywayVersionMigrationHelper.class);
    private final ConstrettoConfiguration configuration;
    private final BasicDataSource dataSource;

    private final Flyway flyway;
    private final String dbUrl;
    private final JdbcTemplate jdbcTemplate;

    public FlywayVersionMigrationHelper(ConstrettoConfiguration configuration, BasicDataSource dataSource) {
        this.configuration = configuration;
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        this.dbUrl = dataSource.getUrl();
        this.flyway = new Flyway();
        flyway.setDataSource(dataSource);
        setMigrationScriptLocation(dataSource.getDriverClassName());
    }
    private void setMigrationScriptLocation(String driverClassName) {
        boolean migrate = configuration.evaluateToBoolean("flyway.migration.enable");
        if (migrate) {
            String from = configuration.evaluateToString("flyway.migrate.version.from");
            String to = configuration.evaluateToString("flyway.migrate.version.to");

            log.info("Migrating flyway is enabled");
            log.info("Migrating flyway from version {} -> {}", from, to);

            int fromVersion = Integer.parseInt(from.replace(".", ""));
            int toVersion = Integer.parseInt(to.replace(".", ""));

            log.info("Migrating flyway version (int) from {}", fromVersion);
            log.info("Migrating flyway version (int) to {}", toVersion);

            if (fromVersion > 402) {
                int version = 402;
                setLocation(driverClassName, version);
                migrate();
            }
        }
    }

    private void setLocation(String driverClassName, int version) {
        if (driverClassName.contains("hsqldb")) {
            flyway.setLocations("db/flyway/migration/version/" + version + "/hsqldb");
        } else if (driverClassName.contains("mysql")) {
            flyway.setLocations("db/flyway/migration/version/" + version + "/mysql");
        } else if (driverClassName.contains("mariadb")) {
            flyway.setLocations("db/flyway/migration/version/" + version + "/mariadb");
        } else if (driverClassName.contains("postgresql")) {
            flyway.setLocations("db/flyway/migration/versionversion/" + version + "/postgresql");
        } else if (dbUrl.contains("sqlserver")) {
            log.info("Expecting the MS SQL database to be pre-initialized with the latest schema. Automatic database migration is not supported.");
        } else {
            throw new RuntimeException("Unsupported database driver found in configuration - " + driverClassName);
        }

    }

    public void migrate() {
        log.info("Run migrate");
        try {
            boolean versionRankColumnExists = checkColumnExists(dataSource);
            log.info("versionRankColumnExists: {}", versionRankColumnExists);
            if (versionRankColumnExists) {
                log.info("Upgrading metadata table to the Flyway 4.0 format ...");
            } else {
                log.info("Could not find column name");
            }
        } catch (Exception e) {
            log.error("Failed to migrate flyway version", e);
        }

    }

    /**
     * Check if column version_rank exists in the flyway table schema_version
     * @return True if column exists
     * @throws MetaDataAccessException
     */
    private boolean checkColumnExists(DataSource dataSource) throws MetaDataAccessException {
//        jdbcTemplate.query("", new RowMapper<Object>() {
//            @Override
//            public Object mapRow(ResultSet rs, int i) throws SQLException {
//                ResultSetMetaData metaData = rs.getMetaData();
//                int columnCount = metaData.getColumnCount();
//
//
//
//                if (metaData.get
//                return null;
//            }
//        })
//
//        return JdbcUtils.extractDatabaseMetaData(
//                dataSource, metadata -> {
//                    ResultSet rs = metadata.getColumns(
//                            null, null,
//                            "schema_version",
//                            "version_rank");
//                    return rs.next();
//                });
        return true;
    }



}


