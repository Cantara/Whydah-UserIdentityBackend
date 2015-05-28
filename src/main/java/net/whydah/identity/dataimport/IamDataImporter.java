package net.whydah.identity.dataimport;

import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.user.role.UserPropertyAndRoleDao;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import net.whydah.identity.user.search.LuceneIndexer;
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

public class IamDataImporter {
    private static final Logger log = LoggerFactory.getLogger(IamDataImporter.class);
    static final String CHARSET_NAME = "ISO-8859-1";

    private final BasicDataSource dataSource;
    private final QueryRunner queryRunner;
    private final LdapUserIdentityDao ldapUserIdentityDao;
    private final Directory index;
    private String applicationsImportSource;
    private String organizationsImportSource;
    private String userImportSource;
    private String roleMappingImportSource;

    public IamDataImporter(BasicDataSource dataSource, ConstrettoConfiguration configuration)  {
        this.dataSource = dataSource;
        this.queryRunner = new QueryRunner(dataSource);
        this.ldapUserIdentityDao = initLdapUserIdentityDao(configuration);
        //String luceneDir = AppConfig.appConfig.getProperty("lucene.directory");
        String luceneDir = configuration.evaluateToString("lucene.directory");
        this.index = initDirectory(luceneDir);

        /*
        this.applicationsImportSource = AppConfig.appConfig.getProperty("import.applicationssource");
        this.organizationsImportSource = AppConfig.appConfig.getProperty("import.organizationssource");
        this.userImportSource = AppConfig.appConfig.getProperty("import.usersource");
        this.roleMappingImportSource = AppConfig.appConfig.getProperty("import.rolemappingsource");
        */
        this.applicationsImportSource = configuration.evaluateToString("import.applicationssource");
        this.organizationsImportSource = configuration.evaluateToString("import.organizationssource");
        this.userImportSource = configuration.evaluateToString("import.usersource");
        this.roleMappingImportSource = configuration.evaluateToString("import.rolemappingsource");
    }

    /*
    @Deprecated
	public IamDataImporter(BasicDataSource dataSource)  {
        this.dataSource = dataSource;
        this.queryRunner = new QueryRunner(dataSource);
        this.ldapUserIdentityDao = initLdapUserIdentityDao(null);
        this.index = initDirectory(AppConfig.appConfig.getProperty("lucene.directory"));
	}
	*/

    //used by tests
    public IamDataImporter(BasicDataSource dataSource, LdapUserIdentityDao ldapUserIdentityDao, Directory index)  {
        this.dataSource = dataSource;
        this.queryRunner = new QueryRunner(dataSource);
        this.ldapUserIdentityDao = ldapUserIdentityDao;
        this.index = index;
    }

    //Database migrations should already have been performed before import.
	public void importIamData() {
        InputStream ais = null;
        InputStream ois = null;
        InputStream uis = null;
        InputStream rmis = null;
        try {
            ais = openInputStream("Applications", applicationsImportSource);
            new ApplicationImporter(queryRunner).importApplications(ais);

            ois = openInputStream("Organizations", organizationsImportSource);
            new OrganizationImporter(queryRunner).importOrganizations(ois);

            uis = openInputStream("Users", userImportSource);
            new WhydahUserIdentityImporter(ldapUserIdentityDao, new LuceneIndexer(index)).importUsers(uis);

            rmis = openInputStream("RoleMappings", roleMappingImportSource);
            UserPropertyAndRoleRepository userPropertyAndRoleRepository =
                    new UserPropertyAndRoleRepository(new UserPropertyAndRoleDao(dataSource), new ApplicationDao(dataSource));
            new RoleMappingImporter(userPropertyAndRoleRepository).importRoleMapping(rmis);
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


    private LdapUserIdentityDao initLdapUserIdentityDao(ConstrettoConfiguration configuration) {
        //Primary LDAP
        /*
        String primaryLdapUrl = AppConfig.appConfig.getProperty("ldap.primary.url");
        String primaryAdmPrincipal = AppConfig.appConfig.getProperty("ldap.primary.admin.principal");
        String primaryAdmCredentials = AppConfig.appConfig.getProperty("ldap.primary.admin.credentials");
        String primaryUidAttribute = AppConfig.appConfig.getProperty("ldap.primary.uid.attribute");
        String primaryUsernameAttribute = AppConfig.appConfig.getProperty("ldap.primary.username.attribute");
        boolean readonly = Boolean.parseBoolean(AppConfig.appConfig.getProperty("ldap.primary.readonly"));
        */
        String primaryLdapUrl = configuration.evaluateToString("ldap.primary.url");
        String primaryAdmPrincipal = configuration.evaluateToString("ldap.primary.admin.principal");
        String primaryAdmCredentials = configuration.evaluateToString("ldap.primary.admin.credentials");
        String primaryUidAttribute = configuration.evaluateToString("ldap.primary.uid.attribute");
        String primaryUsernameAttribute = configuration.evaluateToString("ldap.primary.username.attribute");
        boolean readonly = configuration.evaluateToBoolean("ldap.primary.readonly");
        return new LdapUserIdentityDao(primaryLdapUrl, primaryAdmPrincipal, primaryAdmCredentials, primaryUidAttribute, primaryUsernameAttribute, readonly);
    }

    private Directory initDirectory(String luceneDir) {
        File luceneDirectory = new File(luceneDir);
        if (!luceneDirectory.exists()) {
            boolean dirsCreated = luceneDirectory.mkdirs();
            if (!dirsCreated) {
                log.debug("{} was not successfully created.", luceneDirectory.getAbsolutePath());
            }
        }
        try {
            return new NIOFSDirectory(luceneDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
