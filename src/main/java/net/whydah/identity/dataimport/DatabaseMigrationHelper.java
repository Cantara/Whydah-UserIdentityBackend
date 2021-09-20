package net.whydah.identity.dataimport;

import org.apache.commons.dbcp.BasicDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Ensure database has the necessary DDL and all migrations have been applied.
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a>
 */
public class DatabaseMigrationHelper {
    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationHelper.class);

    private Flyway flyway;
    private String dbUrl;

    public DatabaseMigrationHelper(BasicDataSource dataSource) {
        this.dbUrl = dataSource.getUrl();
        Configuration flyWayConfiguration;
        if (dataSource.getDriverClassName().toLowerCase(Locale.ROOT).contains("hsqldb")) {
            flyWayConfiguration = new FluentConfiguration().dataSource(dataSource).locations("db/migration/hsqldb");
            flyway = new Flyway(flyWayConfiguration);
        } else if (dataSource.getDriverClassName().toLowerCase(Locale.ROOT).contains("mysql")) {
            flyWayConfiguration = new FluentConfiguration().dataSource(dataSource).locations("db/migration/mysql");
            flyway = new Flyway(flyWayConfiguration);
        } else if (dataSource.getDriverClassName().toLowerCase(Locale.ROOT).contains("mariadb")) {
            flyWayConfiguration = new FluentConfiguration().dataSource(dataSource).locations("db/migration/mariadb");
            flyway = new Flyway(flyWayConfiguration);
        } else if (dataSource.getDriverClassName().toLowerCase(Locale.ROOT).contains("postgresql")) {
            flyWayConfiguration = new FluentConfiguration().dataSource(dataSource).locations("db/migration/postgresql");
            org.hsqldb.jdbc.JDBCDriver jdbcDriver;
            flyway = new Flyway(flyWayConfiguration);
        } else if (dbUrl.contains("sqlserver")) {
            log.info("Expecting the MS SQL database to be pre-initialized with the latest schema. Automatic database migration is not supported.");
        } else {
            throw new RuntimeException("Unsupported database driver found in configuration - " + dataSource.getDriverClassName().toLowerCase(Locale.ROOT));
        }
    }



    public void upgradeDatabase() {
        log.info("Upgrading database with url={} using migration files from {}", dbUrl, flyway.getConfiguration().getLocations());
        try {
            flyway.migrate();
        } catch (FlywayException e) {
            log.error("Database upgrade failed using " + dbUrl, e);
//            throw new RuntimeException("Database upgrade failed using " + dbUrl, e);
        }
    }

    //used by tests
    public void cleanDatabase() {
        try {
            if (flyway != null) {
                flyway.clean();
            } else {
                log.warn("Trying to clean an non-initialized database");
            }
        } catch (Exception e) {
            throw new RuntimeException("Database cleaning failed.", e);
        }
    }
}
