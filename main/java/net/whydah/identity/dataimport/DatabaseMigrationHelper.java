package net.whydah.identity.dataimport;

import org.apache.commons.dbcp.BasicDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        flyway = new Flyway();
        flyway.setDataSource(dataSource);
        setMigrationScriptLocation(dataSource.getDriverClassName());
    }

    private void setMigrationScriptLocation(String driverClassName) {
        if (driverClassName.contains("hsqldb")) {
            flyway.setLocations("db/migration/hsqldb");
        } else if(driverClassName.contains("mysql")) {
            flyway.setLocations("db/migration/mysql");
        } else if (dbUrl.contains("sqlserver")) {
            log.info("Expecting the MS SQL database to be pre-initialized with the latest schema. Automatic database migration is not supported.");
        } else {
            throw new RuntimeException("Unsupported database driver found in configuration - " + driverClassName);
        }
    }

    public void upgradeDatabase() {
        log.info("Upgrading database with url={} using migration files from {}", dbUrl, flyway.getLocations());
        try {
            flyway.migrate();
        } catch (FlywayException e) {
            throw new RuntimeException("Database upgrade failed using " + dbUrl, e);
        }
    }

    //used by tests
    public void cleanDatabase() {
        try {
            flyway.clean();
        } catch (FlywayException e) {
            throw new RuntimeException("Database cleaning failed.", e);
        }
    }
}
