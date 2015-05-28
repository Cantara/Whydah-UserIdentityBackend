package net.whydah.identity;

import net.whydah.identity.config.SSLTool;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.dataimport.IamDataImporter;
import net.whydah.identity.user.identity.EmbeddedADS;
import net.whydah.identity.util.FileUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class Main {
    public static final String CONTEXT_PATH = "/uib";
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private EmbeddedADS ads;
    //private HttpServer httpServer;
    private int webappPort;
    private Server server;
    private String resourceBase;



    public Main(Integer webappPort) {
        this.webappPort = webappPort;
        //log.info("Starting Jetty on port {}", webappPort);
        this.server = new Server(webappPort);

        URL url = ClassLoader.getSystemResource("WEB-INF/web.xml");
        this.resourceBase = url.toExternalForm().replace("WEB-INF/web.xml", "");
    }



    // 1a. Default:        External ldap and database
    // or
    // 1b. Test scenario:  start embedded Ldap and database

    // 2. run db migrations (should not share any objects with the web application)

    // 3. possibly import (should not share any objects with the web application)

    // 4. start webserver
    public static void main(String[] args) {
        final ConstrettoConfiguration configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("file:./useridentitybackend_override.properties"))
                .done()
                .getConfiguration();

        /*
        boolean importEnabled = Boolean.parseBoolean(AppConfig.appConfig.getProperty("import.enabled"));
        boolean embeddedDSEnabled = Boolean.parseBoolean(AppConfig.appConfig.getProperty("ldap.embedded"));

        String ldapEmbeddedpath = AppConfig.appConfig.getProperty("ldap.embedded.directory");
        String roleDBDirectory = AppConfig.appConfig.getProperty("roledb.directory");
        String luceneDirectory = AppConfig.appConfig.getProperty("lucene.directory");
        Integer ldapPort = Integer.valueOf(AppConfig.appConfig.getProperty("ldap.embedded.port"));

        String sslVerification = AppConfig.appConfig.getProperty("sslverification");
        String requiredRoleName = AppConfig.appConfig.getProperty("useradmin.requiredrolename");
        Integer webappPort = Integer.valueOf(AppConfig.appConfig.getProperty("service.port"));
        */
        String version = Main.class.getPackage().getImplementationVersion();

        boolean importEnabled = configuration.evaluateToBoolean("import.enabled");
        boolean embeddedDSEnabled = configuration.evaluateToBoolean("ldap.embedded");
        log.info("Starting UserIdentityBackend version={}, import.enabled={}, embeddedDSEnabled={}", version, importEnabled, embeddedDSEnabled);
        try {
            Integer webappPort = configuration.evaluateToInt("service.port");
            final Main main = new Main(webappPort);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    log.debug("ShutdownHook triggered. Exiting application");
                    main.stop();
                }
            });

            String ldapEmbeddedpath = configuration.evaluateToString("ldap.embedded.directory");
            String roleDBDirectory = configuration.evaluateToString("roledb.directory");
            String luceneDirectory = configuration.evaluateToString("lucene.directory");

            if (importEnabled) {
                FileUtils.deleteDirectories(ldapEmbeddedpath, roleDBDirectory, luceneDirectory);
            }
            FileUtils.createDirectory(luceneDirectory);

            if (embeddedDSEnabled) {
                Integer ldapPort = configuration.evaluateToInt("ldap.embedded.port");
                main.startEmbeddedDS(ldapEmbeddedpath, ldapPort);
            }

            BasicDataSource dataSource = initBasicDataSource(configuration);
            new DatabaseMigrationHelper(dataSource).upgradeDatabase();

            if (importEnabled) {
                // Populate ldap, database and lucene index
                new IamDataImporter(dataSource, configuration).importIamData();
            }


            // Property-overwrite of SSL verification to support weak ssl certificates
            String sslVerification = configuration.evaluateToString("sslverification");
            if ("disabled".equalsIgnoreCase(sslVerification)) {
                SSLTool.disableCertificateValidation();
            }

            //main.startHttpServer(requiredRoleName);
            main.start();
            main.join();
            log.info("UserIdentityBackend version:{} started on port {}.", version, webappPort + " context-path:" + CONTEXT_PATH);

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

    /*
     String jdbcdriver = AppConfig.appConfig.getProperty("roledb.jdbc.driver");
     String jdbcurl = AppConfig.appConfig.getProperty("roledb.jdbc.url");
     String roledbuser = AppConfig.appConfig.getProperty("roledb.jdbc.user");
     String roledbpasswd = AppConfig.appConfig.getProperty("roledb.jdbc.password");
     */
    //"jdbc:hsqldb:file:" + basepath + "hsqldb");
    private static BasicDataSource initBasicDataSource(ConstrettoConfiguration configuration) {
        String jdbcdriver = configuration.evaluateToString("roledb.jdbc.driver");
        String jdbcurl = configuration.evaluateToString("roledb.jdbc.url");
        String roledbuser = configuration.evaluateToString("roledb.jdbc.user");
        String roledbpasswd = configuration.evaluateToString("roledb.jdbc.password");

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jdbcdriver);
        dataSource.setUrl(jdbcurl);
        dataSource.setUsername(roledbuser);
        dataSource.setPassword(roledbpasswd);
        return dataSource;
    }

    public void start()  {
        WebAppContext webAppContext = new WebAppContext();
        log.debug("Start Jetty using resourcebase={}", resourceBase);
        webAppContext.setDescriptor(resourceBase + "/WEB-INF/web.xml");
        webAppContext.setResourceBase(resourceBase);
        webAppContext.setContextPath(CONTEXT_PATH);
        webAppContext.setParentLoaderPriority(true);

        HandlerList handlers = new HandlerList();
        Handler[] handlerList = {webAppContext, new DefaultHandler()};
        handlers.setHandlers(handlerList);
        server.setHandler(handlers);

        //TODO addSecurityFilterForUserAdmin
        /*
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
         */


        try {
            server.start();
        } catch (Exception e) {
            log.error("Error during Jetty startup. Exiting", e);
            System.exit(2);
        }
        int localPort = getPort();
        log.info("Jetty server started on http://localhost:{}{}", localPort, CONTEXT_PATH);
    }


    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            log.warn("Error when stopping Jetty server", e);
        }

        if (ads != null) {
            log.info("Stopping embedded Apache DS.");
            ads.stopServer();
        }
    }

    public void join() {
        try {
            server.join();
        } catch (InterruptedException e) {
            log.error("Jetty server thread when join. Pretend everything is OK.", e);
        }
    }

    /*
    public void startHttpServer(String requiredRoleName) {
        ServletHandler servletHandler = new ServletHandler();
        servletHandler.setContextPath(CONTEXT_PATH);
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
    }
    */


    public void startEmbeddedDS(String ldapPath, int ldapPort) {
        ads = new EmbeddedADS(ldapPath);
        ads.startServer(ldapPort);
    }

    /*
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
    */

    public int getPort() {
        return webappPort;
        //        return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }
}