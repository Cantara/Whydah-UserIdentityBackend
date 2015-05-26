package net.whydah.identity;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import net.whydah.identity.application.authentication.ApplicationTokenService;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.config.SSLTool;
import net.whydah.identity.config.UserIdentityBackendModule;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.dataimport.IamDataImporter;
import net.whydah.identity.security.SecurityFilter;
import net.whydah.identity.user.authentication.SecurityTokenServiceHelper;
import net.whydah.identity.user.identity.EmbeddedADS;
import net.whydah.identity.util.FileUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static String version;

    private EmbeddedADS ads;
    private HttpServer httpServer;
    private int webappPort;
    public static final String contextpath = "/uib";


    public Main(Integer webappPort) {
        version = this.getClass().getPackage().getImplementationVersion();
        this.webappPort = webappPort;
    }



    // 1a. Default:        External ldap and database
    // or
    // 1b. Test scenario:  start embedded Ldap and database

    // 2. run db migrations (should not share any objects with the web application)

    // 3. possibly import (should not share any objects with the web application)

    // 4. start webserver
    public static void main(String[] args) {
        boolean importEnabled = Boolean.parseBoolean(AppConfig.appConfig.getProperty("import.enabled"));
        boolean embeddedDSEnabled = Boolean.parseBoolean(AppConfig.appConfig.getProperty("ldap.embedded"));

        String ldapEmbeddedpath = AppConfig.appConfig.getProperty("ldap.embedded.directory");
        String roleDBDirectory = AppConfig.appConfig.getProperty("roledb.directory");
        String luceneDirectory = AppConfig.appConfig.getProperty("lucene.directory");
        Integer ldapPort = Integer.valueOf(AppConfig.appConfig.getProperty("ldap.embedded.port"));

        String sslVerification = AppConfig.appConfig.getProperty("sslverification");
        String requiredRoleName = AppConfig.appConfig.getProperty("useradmin.requiredrolename");
        Integer webappPort = Integer.valueOf(AppConfig.appConfig.getProperty("service.port"));


        log.info("Starting UserIdentityBackend version={}, import.enabled={}, embeddedDSEnabled={}", version, importEnabled, embeddedDSEnabled);
        try {
            final Main main = new Main(webappPort);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    log.debug("ShutdownHook triggered. Exiting application");
                    main.stop();
                }
            });

            if (importEnabled) {
                FileUtils.deleteDirectories(ldapEmbeddedpath, roleDBDirectory, luceneDirectory);
            }


            if (embeddedDSEnabled) {
                main.startEmbeddedDS(ldapEmbeddedpath, ldapPort);
            }

            BasicDataSource dataSource = initBasicDataSource();
            new DatabaseMigrationHelper(dataSource).upgradeDatabase();

            if (importEnabled) {
                // Populate ldap, database and lucene index
                new IamDataImporter(dataSource).importIamData();
            }

            main.startHttpServer(sslVerification, requiredRoleName);

            if (!embeddedDSEnabled) {
                try {
                    // wait forever...
                    Thread.currentThread().join();
                } catch (InterruptedException ie) {
                    log.warn("Thread was interrupted.", ie);
                }
                log.debug("Finished waiting for Thread.currentThread().join()");
                main.stop();
            }
        } catch (RuntimeException e) {
            log.error("Error during startup. Shutting down UserIdentityBackend.", e);
            System.exit(1);
        }
    }

    private static BasicDataSource initBasicDataSource() {
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

    public void startHttpServer(String sslVerification, String requiredRoleName) {
        // Property-overwrite of SSL verification to support weak ssl certificates
        if ("disabled".equalsIgnoreCase(sslVerification)) {
            SSLTool.disableCertificateValidation();
        }

        ServletHandler servletHandler = new ServletHandler();
        servletHandler.setContextPath(contextpath);
        servletHandler.addInitParameter("com.sun.jersey.config.property.packages",
                "net.whydah.identity.user.resource, net.whydah.identity.user.authentication, net.whydah.identity.application, net.whydah.identity.application.authentication, net.whydah.identity.health");
        servletHandler.addInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        servletHandler.setProperty(ServletHandler.LOAD_ON_STARTUP, "1");

        GuiceFilter filter = new GuiceFilter();
        servletHandler.addFilter(filter, "guiceFilter", null);

        Injector injector = Guice.createInjector(new UserIdentityBackendModule());
        SecurityTokenServiceHelper securityTokenHelper = injector.getInstance(SecurityTokenServiceHelper.class);
        ApplicationTokenService applicationTokenService = injector.getInstance(ApplicationTokenService.class);
        //addSecurityFilterForUserAdmin
        if (StringUtils.isEmpty(requiredRoleName)) {
            log.warn("Required Role Name is empty! Verify the useradmin.requiredrolename-attribute in the configuration.");
        }
        SecurityFilter securityFilter = new SecurityFilter(securityTokenHelper, applicationTokenService);
        HashMap<String, String> initParams = new HashMap<>(1);
        servletHandler.addFilter(securityFilter, "SecurityFilter", initParams);
        log.info("SecurityFilter initialized with params:", initParams);

        GuiceContainer container = new GuiceContainer(injector);
        servletHandler.setServletInstance(container);

        httpServer = new HttpServer();
        ServerConfiguration serverconfig = httpServer.getServerConfiguration();
        serverconfig.addHttpHandler(servletHandler, "/");
        NetworkListener listener = new NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, webappPort);
        httpServer.addListener(listener);
        try {
            httpServer.start();
        } catch (IOException e) {
            throw new RuntimeException("grizzly server start failed", e);
        }
        log.info("UserIdentityBackend version:{} started on port {}.", version, webappPort + " context-path:" + contextpath);
    }


    public void startEmbeddedDS(String ldapPath, int ldapPort) {
        ads = new EmbeddedADS(ldapPath);
        ads.startServer(ldapPort);
    }

    public void stop() {
        log.info("Stopping http server");    //TODO ED: What about hsqldb?  It dies with the process..
        if (httpServer != null) {
            httpServer.stop();
        }
        if (ads != null) {
            log.info("Stopping embedded Apache DS.");
            ads.stopServer();
        }
    }

    public int getPort() {
        return webappPort;
    }
}