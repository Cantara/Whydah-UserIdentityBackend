package net.whydah.identity.dataimport;

import net.whydah.identity.Main;
import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.application.ApplicationService;
import net.whydah.identity.application.search.LuceneApplicationIndexer;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.user.role.UserApplicationRoleEntryDao;
import net.whydah.identity.util.FileUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.constretto.ConstrettoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;

public class IamDataImporter {
    private static final Logger log = LoggerFactory.getLogger(IamDataImporter.class);
    static final String CHARSET_NAME = "UTF-8";

    private final BasicDataSource dataSource;
    private final QueryRunner queryRunner;
    private final LdapUserIdentityDao ldapUserIdentityDao;
    private final String luceneUsersDir;
    private final String luceneApplicationsDir;
    private final UserApplicationRoleEntryDao userApplicationRoleEntryDao;
    private final ConstrettoConfiguration config;


    private String applicationsImportSource;
    private String organizationsImportSource;
    private String userImportSource;
    private String roleMappingImportSource;


    public IamDataImporter(BasicDataSource dataSource, ConstrettoConfiguration config)  {
        this(dataSource, config, Main.ldapProperties(config));
    }

    //used by tests
    IamDataImporter(BasicDataSource dataSource, ConstrettoConfiguration configuration, Map<String, String> ldapProperties)  {
        this.dataSource = dataSource;
        this.queryRunner = new QueryRunner(dataSource);
        this.userApplicationRoleEntryDao = new UserApplicationRoleEntryDao(dataSource);

        this.config = configuration;

        this.luceneUsersDir = configuration.evaluateToString("lucene.usersdirectory");
        this.luceneApplicationsDir = configuration.evaluateToString("lucene.applicationsdirectory");
        FileUtils.deleteDirectories(luceneUsersDir, luceneApplicationsDir);

        this.applicationsImportSource = configuration.evaluateToString("import.applicationssource");
        this.organizationsImportSource = configuration.evaluateToString("import.organizationssource");
        this.userImportSource = configuration.evaluateToString("import.usersource");
        this.roleMappingImportSource = configuration.evaluateToString("import.rolemappingsource");

        this.ldapUserIdentityDao = initLdapUserIdentityDao(ldapProperties);
    }



    //Database migrations should already have been performed before import.
	public void importIamData() {
        InputStream ais = null;
        InputStream ois = null;
        InputStream uis = null;
        InputStream rmis = null;
        try {
            ais = openInputStream("Applications", applicationsImportSource);
            Directory applicationsindex = new NIOFSDirectory(Paths.get(new File(luceneApplicationsDir).getPath()));
            LuceneApplicationIndexer luceneApplicationIndexer = new LuceneApplicationIndexer(applicationsindex);

            ApplicationService applicationService = new ApplicationService(new ApplicationDao(dataSource), new AuditLogDao(dataSource), luceneApplicationIndexer, null);

            if (applicationsImportSource.endsWith(".csv")) {
                new ApplicationImporter(applicationService).importApplications(ais);
            } else {
                new ApplicationJsonImporter(applicationService, config).importApplications(ais);
            }


            ois = openInputStream("Organizations", organizationsImportSource);
            new OrganizationImporter(queryRunner).importOrganizations(ois);

            uis = openInputStream("Users", userImportSource);
            NIOFSDirectory usersIndex = createDirectory(luceneUsersDir);
            //Directory index = new RAMDirectory();
            //LuceneUserIndexer luceneUserIndexer = new LuceneUserIndexer(index);
            new WhydahUserIdentityImporter(ldapUserIdentityDao, usersIndex).importUsers(uis);
            //luceneUserIndexer.closeIndexer();

            rmis = openInputStream("RoleMappings", roleMappingImportSource);
            new RoleMappingImporter(userApplicationRoleEntryDao).importRoleMapping(rmis);
        } catch (Exception e) {
            log.error("Error in Importing applications", e);
        } finally {
            FileUtils.close(ais);
            FileUtils.close(ois);
            FileUtils.close(uis);
            FileUtils.close(rmis);

            //TODO are ldap, lucene and database resources closed?
        }
    }

    InputStream openInputStream(String tableName, String importSource) {
        InputStream is;
        if (FileUtils.localFileExist(importSource)) {
            log.info("Importing {} from local config override. {}", tableName,importSource);
            is = FileUtils.openLocalFile(importSource);
        } else {
            log.info("Import {} from classpath {}", tableName, importSource);
            is = FileUtils.openFileOnClasspath(importSource);
        }
        return is;
    }


    private LdapUserIdentityDao initLdapUserIdentityDao(Map<String, String> ldapProperties) {
        String primaryLdapUrl = ldapProperties.get("ldap.primary.url");
        String primaryAdmPrincipal = ldapProperties.get("ldap.primary.admin.principal");
        String primaryAdmCredentials = ldapProperties.get("ldap.primary.admin.credentials");
        String primaryUidAttribute = ldapProperties.get("ldap.primary.uid.attribute");
        String primaryUsernameAttribute = ldapProperties.get("ldap.primary.username.attribute");
        String readonly = ldapProperties.get("ldap.primary.readonly");
        return new LdapUserIdentityDao(primaryLdapUrl, primaryAdmPrincipal, primaryAdmCredentials, primaryUidAttribute, primaryUsernameAttribute, readonly);
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

    //expose for test
    LdapUserIdentityDao getLdapUserIdentityDao() {
        return ldapUserIdentityDao;
    }

    UserApplicationRoleEntryDao getUserApplicationRoleEntryDao() {
        return userApplicationRoleEntryDao;
    }
}
