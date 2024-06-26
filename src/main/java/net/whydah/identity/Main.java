package net.whydah.identity;

import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.dataimport.IamDataImporter;
import net.whydah.identity.util.FileUtils;
import net.whydah.sso.util.SSLTool;
import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

public class Main {
    public static final String CONTEXT_PATH = "/uib";
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private int webappPort;
    private Server server;

    /*
     * 1a. Default:        External database
     * or
     * 1b. Test scenario:  startJetty and database
     *
     * 2. run db migrations (should not share any objects with the web application)
     *
     * 3. possibly import (should not share any objects with the web application)
     *
     * 4. startJetty webserver
     */
    public static void main(String[] args) {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        LogManager.getLogManager().getLogger("").setLevel(Level.INFO);

        copyConfigExamples();
        copyConfig();

        final ConstrettoConfiguration config = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("file:./useridentitybackend_override.properties"))
                .done()
                .getConfiguration();

        printConfiguration(config);

        Integer webappPort = config.evaluateToInt("service.port");
        try {
            final Main main = new Main(webappPort);

            main.start(config);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    log.debug("ShutdownHook triggered. Exiting application");
                    main.stop();

                }
            });

        } catch (RuntimeException e) {
            log.error("Error during startup. Shutting down UserIdentityBackend.", e);
            System.exit(1);
        }
    }

    public Main(Integer webappPort) {
        this.webappPort = webappPort;
    }

    private void start(ConstrettoConfiguration config) {
        // Property-overwrite of SSL verification to support weak ssl certificates
        String sslVerification = config.evaluateToString("sslverification");
        if ("disabled".equalsIgnoreCase(sslVerification)) {
            SSLTool.disableCertificateValidation();
        }


        boolean importEnabled = config.evaluateToBoolean("import.enabled");
        String version = Main.class.getPackage().getImplementationVersion();
        log.info("Starting UserIdentityBackend version={}, import.enabled={}", version, importEnabled);


        initLucene(config);

        BasicDataSource dataSource = initRoleDB(config);

        if (importEnabled) {
            // Populate database and lucene index
            new IamDataImporter(dataSource, config).importIamData();
        }

        startJetty();
        joinJetty();
        log.info("UserIdentityBackend version:{} started on port {}. ", version, webappPort + " context-path:" + CONTEXT_PATH);
        log.info("Health: http://localhost:{}/{}/{}/", webappPort, CONTEXT_PATH, "health");
    }

    private void initLucene(ConstrettoConfiguration config) {
        String luceneUsersDirectory = config.evaluateToString("lucene.usersdirectory");
        String luceneApplicationDirectory = config.evaluateToString("lucene.applicationsdirectory");

        boolean importEnabled = config.evaluateToBoolean("import.enabled");
        if (importEnabled) {
            FileUtils.deleteDirectories(luceneUsersDirectory, luceneApplicationDirectory);
        }

        FileUtils.createDirectory(luceneUsersDirectory);
        FileUtils.createDirectory(luceneApplicationDirectory);

        log.info("Import enabled: {}", importEnabled);
        log.info("luceneUserdirectory: {}", luceneUsersDirectory);
        log.info("luceneApplicationDirectory: {}", luceneApplicationDirectory);
    }


    private BasicDataSource initRoleDB(ConstrettoConfiguration config) {
        boolean importEnabled = config.evaluateToBoolean("import.enabled");
        if (importEnabled) {
            String roleDBDirectory = config.evaluateToString("roledb.directory");
            FileUtils.deleteDirectories(roleDBDirectory);
        }
        BasicDataSource dataSource = initBasicDataSource(config);
        Map<String, String> flywayConfigMap = config.asMap().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("flyway."))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        new DatabaseMigrationHelper(dataSource, flywayConfigMap).upgradeDatabase();
        return dataSource;
    }

    public static BasicDataSource initBasicDataSource(ConstrettoConfiguration configuration) {
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


    public void startJetty() {
        int maxThreads = 100;
        int minThreads = 10;
        int idleTimeout = 120;
        QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);

        this.server = new Server(threadPool);
        ServerConnector connector = new ServerConnector(this.server);
        connector.setPort(webappPort);
        this.server.setConnectors(new Connector[]{connector});

        URL url = ClassLoader.getSystemResource("WEB-INF/web.xml");
        String resourceBase = url.toExternalForm().replace("WEB-INF/web.xml", "");

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
    }

    private void joinJetty() {
        try {
            server.join();
        } catch (InterruptedException e) {
            log.error("Jetty server thread when joinJetty. Pretend everything is OK.", e);
        }
    }


    private void runtimeException(Exception e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        throw new RuntimeException(e);
    }


    public int getPort() {
        return webappPort;
    }

    private static void printConfiguration(ConstrettoConfiguration configuration) {
        if (configuration == null) {
            log.error("Missing configuration - uib will not work properly!");
        } else {
            Map<String, String> properties = new TreeMap<>(configuration.asMap());
            StringBuilder strb = new StringBuilder("Configuration properties (property=value):");
            for (String key : properties.keySet()) {
                strb.append("\n ").append(key).append("=").append(properties.get(key));
            }
            log.debug(strb.toString());
        }
    }

    static void copyConfigExamples() {
        FileUtils.copyFiles(new String[]{"useridentitybackend.properties", "logback.xml"}, "config_examples", true);
    }

    static void copyConfig() {
        FileUtils.copyFiles(new String[]{"useridentitybackend_override.properties", "logback.xml"}, "config", false);
    }
}