package net.whydah.identity.dataimport;

import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.application.ApplicationService;
import net.whydah.identity.application.search.LuceneApplicationIndexer;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.organization.OrganizationDao;
import net.whydah.identity.organization.OrganizationRepository;
import net.whydah.identity.user.identity.BCryptService;
import net.whydah.identity.user.identity.RDBMSUserIdentityDao;
import net.whydah.identity.user.identity.RDBMSUserIdentityRepository;
import net.whydah.identity.user.role.UserApplicationRoleEntryDao;
import net.whydah.identity.user.role.UserApplicationRoleEntryRepository;
import net.whydah.identity.util.FileUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.constretto.ConstrettoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

public class IamDataImporter {
    private static final Logger log = LoggerFactory.getLogger(IamDataImporter.class);
    static final String CHARSET_NAME = "UTF-8";

    private final BasicDataSource dataSource;
    private final String luceneUsersDir;
    private final String luceneApplicationsDir;
    private final UserApplicationRoleEntryDao userApplicationRoleEntryDao;
    private final ConstrettoConfiguration config;

    private final UserApplicationRoleEntryRepository userApplicationRoleEntryRepository;
    private final OrganizationRepository organizationRepository;
    private final RDBMSUserIdentityRepository userIdentityRepository;
    private final BCryptService bCryptService;

    private final String applicationsImportSource;
    private final String organizationsImportSource;
    private final String userImportSource;
    private final String roleMappingImportSource;


    public IamDataImporter(BasicDataSource dataSource, ConstrettoConfiguration configuration) {
        this.dataSource = dataSource;
        this.userApplicationRoleEntryDao = new UserApplicationRoleEntryDao(dataSource);
        this.userApplicationRoleEntryRepository = new UserApplicationRoleEntryRepository(userApplicationRoleEntryDao);

        this.config = configuration;

        this.luceneUsersDir = configuration.evaluateToString("lucene.usersdirectory");
        this.luceneApplicationsDir = configuration.evaluateToString("lucene.applicationsdirectory");
        FileUtils.deleteDirectories(luceneUsersDir, luceneApplicationsDir);

        this.applicationsImportSource = configuration.evaluateToString("import.applicationssource");
        this.organizationsImportSource = configuration.evaluateToString("import.organizationssource");
        this.userImportSource = configuration.evaluateToString("import.usersource");
        this.roleMappingImportSource = configuration.evaluateToString("import.rolemappingsource");

        OrganizationDao organizationDao = new OrganizationDao(dataSource);
        this.organizationRepository = new OrganizationRepository(organizationDao);

        RDBMSUserIdentityDao userIdentityDao = new RDBMSUserIdentityDao(dataSource);
        this.bCryptService = new BCryptService(configuration.evaluateToString("userdb.password.pepper"), configuration.evaluateToInt("userdb.password.bcrypt.preferredcost"));
        this.userIdentityRepository = new RDBMSUserIdentityRepository(userIdentityDao, bCryptService, configuration);
    }


    //Database migrations should already have been performed before import.
    public void importIamData() {
        importApplications(applicationsImportSource);
        importOrganizations(organizationsImportSource);
        importUsers(userImportSource);
        importRoleMappings(roleMappingImportSource);
    }

    private void importRoleMappings(String roleMappingImportSource) {
        InputStream rmis = null;
        try {
            rmis = openInputStream("RoleMappings", roleMappingImportSource);
            RoleMappingImporter roleMappingImporter = new RoleMappingImporter(userApplicationRoleEntryRepository);
            roleMappingImporter.importRoleMapping(rmis);
        } catch (Exception e) {
            log.error("Error in importing roles", e);
        } finally {
            FileUtils.close(rmis);
        }
    }

    private void importUsers(String userImportSource) {
        InputStream uis = null;
        try {
            uis = openInputStream("Users", userImportSource);
            NIOFSDirectory usersIndex = createDirectory(luceneUsersDir);
            WhydahUserIdentityImporter userIdentityImporter = new WhydahUserIdentityImporter(userIdentityRepository, usersIndex, bCryptService);
            userIdentityImporter.importUsers(uis);
        } catch (Exception e) {
            log.error("Error in importing users", e);
        } finally {
            FileUtils.close(uis);
        }
    }

    private void importOrganizations(String organizationsImportSource) {
        InputStream ois = null;
        try {
            ois = openInputStream("Organizations", organizationsImportSource);
            OrganizationImporter organizationImporter = new OrganizationImporter(organizationRepository);
            organizationImporter.importOrganizations(ois);
        } catch (Exception e) {
            log.error("Error in importing organizations", e);
        } finally {
            FileUtils.close(ois);
        }
    }

    private void importApplications(String applicationsImportSource) {
        InputStream ais = null;
        try {
            log.info("ais is: {}", applicationsImportSource);
            ais = openInputStream("Applications", applicationsImportSource);
            Directory applicationsindex = new NIOFSDirectory(Paths.get(new File(luceneApplicationsDir).getPath()));
            LuceneApplicationIndexer luceneApplicationIndexer = new LuceneApplicationIndexer(applicationsindex);
            ApplicationService applicationService = new ApplicationService(new ApplicationDao(dataSource), new AuditLogDao(dataSource), luceneApplicationIndexer, null);

            if (applicationsImportSource.endsWith(".csv")) {
                new ApplicationImporter(applicationService).importApplications(ais);
            } else {
                new ApplicationJsonImporter(applicationService, config).importApplications(ais);
            }
        } catch (Exception e) {
            log.error("Error in importing applications", e);
        } finally {
            FileUtils.close(ais);
        }
    }


    InputStream openInputStream(String tableName, String importSource) {
        InputStream is;
        if (FileUtils.localFileExist(importSource)) {
            log.info("Importing {} from local config override. {}", tableName, importSource);
            is = FileUtils.openLocalFile(importSource);
        } else {
            log.info("Import {} from classpath {}", tableName, importSource);
            is = FileUtils.openFileOnClasspath(importSource);
        }
        return is;
    }


    private NIOFSDirectory createDirectory(String luceneDir) {
        try {
            File luceneDirectory = new File(luceneDir);
            if (!luceneDirectory.exists()) {
                boolean dirsCreated = luceneDirectory.mkdirs();
                if (!dirsCreated) {
                    log.debug("{} was not successfully created.", luceneDirectory.getAbsolutePath());
                }
            }
            return new NIOFSDirectory(Paths.get(luceneDirectory.getPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // expose for test
    RDBMSUserIdentityRepository getUserIdentityRepository() {
        return userIdentityRepository;
    }

    UserApplicationRoleEntryDao getUserApplicationRoleEntryDao() {
        return userApplicationRoleEntryDao;
    }
}
