package net.whydah.identity.dataimport;

import net.whydah.identity.Main;
import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.application.ApplicationService;
import net.whydah.identity.application.search.LuceneApplicationIndexer;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.util.FileUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.lucene.store.NIOFSDirectory;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.UUID;


public class EmbeddedJsonApplicationFileImportTest {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedJsonApplicationFileImportTest.class);

    private static BasicDataSource dataSource;
    private static Main main;
    private static String applicationsImportSource;


    @BeforeClass
    public static void startServer() {
        ApplicationMode.setCIMode();
        final ConstrettoConfiguration config = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("classpath:useridentitybackend-test.properties"))
                .done()
                .getConfiguration();


        String roleDBDirectory = config.evaluateToString("roledb.directory");
        FileUtils.deleteDirectory(roleDBDirectory);

        applicationsImportSource = config.evaluateToString("import.applicationssource");
        dataSource = Main.initBasicDataSource(config);

        DatabaseMigrationHelper dbHelper = new DatabaseMigrationHelper(dataSource);
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();

        main = new Main(6648);
    }


    @AfterClass
    public static void stop() {
        if (main != null) {
        }

        try {
            if (!dataSource.isClosed()) {
                dataSource.close();
            }
        } catch (SQLException e) {
            log.error("", e);
        }
    }

    @Test
    public void testEmbeddedJsonApplicationsFile() throws IOException {
        final ConstrettoConfiguration config = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("classpath:useridentitybackend-test.properties"))
                .done()
                .getConfiguration();

        InputStream ais = null;
        try {
            ais = openInputStream("Applications", applicationsImportSource);
            log.debug("Testimporting:" + applicationsImportSource);

            LuceneApplicationIndexer luceneApplicationIndexer = new LuceneApplicationIndexer(new NIOFSDirectory(Paths.get("target/lucene/EmbeddedJsonApplicationFileImportTest/" + UUID.randomUUID())));
            ApplicationService applicationService = new ApplicationService(new ApplicationDao(dataSource), new AuditLogDao(dataSource), luceneApplicationIndexer, null);

            if (applicationsImportSource.endsWith(".csv")) {
                new ApplicationImporter(applicationService).importApplications(ais);
            } else {
                new ApplicationJsonImporter(applicationService, config).importApplications(ais);
            }
        } finally {
            FileUtils.close(ais);

        }
    }

    private InputStream openInputStream(String tableName, String importSource) {
        InputStream is;
        if (FileUtils.localFileExist(importSource)) {
            is = FileUtils.openLocalFile(importSource);
        } else {
            is = FileUtils.openFileOnClasspath(importSource);
        }
        return is;
    }
}
