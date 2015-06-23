package net.whydah.identity.config;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.user.authentication.SecurityTokenServiceHelper;
import net.whydah.identity.user.identity.LdapAuthenticator;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.user.role.UserPropertyAndRoleDao;
import net.whydah.identity.user.search.LuceneIndexer;
import net.whydah.identity.user.search.LuceneSearch;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class UserIdentityBackendModule extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger(UserIdentityBackendModule.class);

    @Override
    protected void configure() {
        log.info("Configure UserIdentityBackendModule (primaryLDAP, secondaryLDAP, roledb (sql), queryRunner and Lucene).");

        BasicDataSource dataSource = getBasicDataSource();
        bind(BasicDataSource.class).toInstance(dataSource);

        UserPropertyAndRoleDao roleDao = new UserPropertyAndRoleDao(dataSource);
        bind(UserPropertyAndRoleDao.class).toInstance(roleDao);

        ApplicationDao applicationDao = new ApplicationDao(dataSource);
        bind(ApplicationDao.class).toInstance(applicationDao);

        QueryRunner queryRunner = new QueryRunner(dataSource);
        bind(QueryRunner.class).toInstance(queryRunner);

        bindLuceneServices();

        String tokenserviceUri = AppConfig.appConfig.getProperty("securitytokenservice");
        bind(SecurityTokenServiceHelper.class).toInstance(new SecurityTokenServiceHelper(tokenserviceUri));

        bindLdapServices();
    }


    private BasicDataSource getBasicDataSource() {
        String jdbcdriver = AppConfig.appConfig.getProperty("roledb.jdbc.driver");
        String jdbcurl = AppConfig.appConfig.getProperty("roledb.jdbc.url");
        String roledbuser = AppConfig.appConfig.getProperty("roledb.jdbc.user");
        String roledbpasswd = AppConfig.appConfig.getProperty("roledb.jdbc.password");

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jdbcdriver);
        dataSource.setUrl(jdbcurl);//"jdbc:hsqldb:file:" + basepath + "hsqldb");
        dataSource.setUsername(roledbuser);
        dataSource.setPassword(roledbpasswd);
        return dataSource;
    }

    private void bindLuceneServices() {
        try {
            String luceneDir = AppConfig.appConfig.getProperty("lucene.directory");
            File luceneDirectory = new File(luceneDir);
            if (!luceneDirectory.exists()) {
                boolean dirsCreated = luceneDirectory.mkdirs();
                if (!dirsCreated) {
                    log.debug("{} was not successfully created.", luceneDirectory.getAbsolutePath());
                }
            }

            Directory index = new NIOFSDirectory(luceneDirectory);
            bind(Directory.class).toInstance(index);
            bind(LuceneIndexer.class).toInstance(new LuceneIndexer(index));
            bind(LuceneSearch.class).toInstance(new LuceneSearch(index));
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    private void bindLdapServices() {
        //primary
        String primaryLdapUrl = AppConfig.appConfig.getProperty("ldap.primary.url");
        String primaryAdmPrincipal = AppConfig.appConfig.getProperty("ldap.primary.admin.principal");
        String primaryAdmCredentials = AppConfig.appConfig.getProperty("ldap.primary.admin.credentials");
        String primaryUidAttribute = AppConfig.appConfig.getProperty("ldap.primary.uid.attribute");
        String primaryUsernameAttribute = AppConfig.appConfig.getProperty("ldap.primary.username.attribute");
        boolean primaryReadOnly = Boolean.parseBoolean(AppConfig.appConfig.getProperty("ldap.primary.readonly"));


        LdapAuthenticator primaryLdapAuthenticator = new LdapAuthenticator(primaryLdapUrl, primaryAdmPrincipal, primaryAdmCredentials, primaryUidAttribute, primaryUsernameAttribute);
        bind(LdapAuthenticator.class).annotatedWith(Names.named("primaryLdap")).toInstance(primaryLdapAuthenticator);

        LdapUserIdentityDao primaryLdapUserIdentityDao =
                new LdapUserIdentityDao(primaryLdapUrl, primaryAdmPrincipal, primaryAdmCredentials, primaryUidAttribute, primaryUsernameAttribute, primaryReadOnly);
        bind(LdapUserIdentityDao.class).toInstance(primaryLdapUserIdentityDao);


        //secondary, not currently in use
        /*
        String internalLdapUrl;
        String admPrincipal;
        String admCredentials;
        String usernameAttribute;
        WhydahConfig fCconfig = new WhydahConfig();
        //For Customer
        if (fCconfig.getProptype().equals("DEV")){
        	internalLdapUrl = "internalLdapUrl FOR CUSTOMER";
            admPrincipal = "admPrincipal FOR CUSTOMER";
     		admCredentials = "admCredentials FOR CUSTOMER";
     		usernameAttribute="usernameAttribute FOR CUSTOMER";
        } else {
        	//For Whydah DEV or TEST
        	internalLdapUrl = fCconfig.getfCinternalLdapUrl();
            admPrincipal = fCconfig.getfCadmPrincipal();
    		admCredentials = fCconfig.getfCadmCredentials();
    		usernameAttribute = fCconfig.getfCusernameAttribute();
        }
        //int LDAP_PORT = new Integer(AppConfig.appConfig.getProperty("ldap.embedded.port"));
        // internalLdapUrl = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";
        LdapAuthenticatorImpl internalLdapAuthenticator = new LdapAuthenticatorImpl(internalLdapUrl, admPrincipal, admCredentials, usernameAttribute);
        bind(LdapAuthenticatorImpl.class).annotatedWith(Names.named("internal")).toInstance(internalLdapAuthenticator);
        */
    }
}
