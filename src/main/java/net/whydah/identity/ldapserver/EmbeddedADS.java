package net.whydah.identity.ldapserver;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.UUID;


public class EmbeddedADS {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedADS.class);
    private static final int DEFAULT_SERVER_PORT = 10389;
    private static final String INSTANCE_NAME = "Whydah";
    private static final String BASE_DN = "o=TEST";

    private DirectoryService service;
    private LdapServer server;
    private String dc = "WHYDAH";

    /**
     * initialize the schema manager and add the schema partition to diectory identity
     *
     * @throws Exception if the schema LDIF files are not found on the classpath
     */
    /**
     * private void initSchemaPartition() throws Exception {
     * SchemaPartition schemaPartition = service.getSchemaPartition();
     * <p/>
     * // Init the LdifPartition
     * LdifPartition ldifPartition = new LdifPartition();
     * String workingDirectory = service.getWorkingDirectory().getPath();
     * ldifPartition.setWorkingDirectory(workingDirectory + "/schema");
     * <p/>
     * // Extract the schema on disk (a brand new one) and load the registries
     * File schemaRepository = new File(workingDirectory, "schema");
     * SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(new File(workingDirectory));
     * extractor.extractOrCopy(true);
     * <p/>
     * logger.trace("Extacted Schema");
     * schemaPartition.setWrappedPartition(ldifPartition);
     * <p/>
     * SchemaLoader loader = new LdifSchemaLoader(schemaRepository);
     * SchemaManager schemaManager = new DefaultSchemaManager(loader);
     * service.setSchemaManager(schemaManager);
     * <p/>
     * // We have to load the schema now, otherwise we won't be able
     * // to initialize the Partitions, as we won't be able to parse
     * // and normalize their suffix DN
     * schemaManager.loadAllEnabled();
     * <p/>
     * schemaPartition.setSchemaManager(schemaManager);
     * <p/>
     * List<Throwable> errors = schemaManager.getErrors();
     * <p/>
     * if (!errors.isEmpty()) {
     * throw new Exception("Schema load failed : " + errors);
     * }
     * }
     */

    private void init(String INSTANCE_PATH) {
        try {
            //Used by http://svn.apache.org/repos/asf/directory/apacheds/tags/2.0.0-M7/core-annotations/src/main/java/org/apache/directory/server/core/factory/DefaultDirectoryServiceFactory.java
            System.setProperty("workingDirectory", INSTANCE_PATH);
            logger.trace("ApacheDS instanceDir: " + INSTANCE_PATH);

            DirectoryServiceFactory factory = new FileDirectoryServiceFactory();
            factory.init(INSTANCE_NAME);

            service = factory.getDirectoryService();
            service.getChangeLog().setEnabled(false);
            service.setShutdownHookEnabled(true);


            InstanceLayout il = new InstanceLayout(INSTANCE_PATH);
            service.setInstanceLayout(il);



            if (true) {
                AvlPartition partition = new AvlPartition(service.getSchemaManager());
                partition.setId("Test");
                partition.setSuffixDn(new Dn(service.getSchemaManager(), BASE_DN));
                logger.trace("Initializing partition {} instance {}", BASE_DN, "Test");
                partition.setCacheService(service.getCacheService());
                partition.initialize();
                service.addPartition(partition);
                SchemaManager schemaManager = service.getSchemaManager();
                partition.setSchemaManager(schemaManager);

                AvlPartition mypartition = new AvlPartition(service.getSchemaManager());
                mypartition.setId(INSTANCE_NAME);
                mypartition.setSuffixDn(new Dn(service.getSchemaManager(), "dc=external,dc=" + dc + ",dc=no"));
                logger.trace("Initializing LDAP partition {} instance {}", "dc=external,dc=" + dc + ",dc=no", INSTANCE_NAME);
                mypartition.setCacheService(service.getCacheService());
                mypartition.initialize();
                mypartition.setSchemaManager(schemaManager);
                service.addPartition(mypartition);

                //         Partition apachePartition = addPartition(dc, "dc=external,dc="+dc+",dc=no");
                Dn dnApache = new Dn("dc=external,dc=" + dc + ",dc=no");
                Entry entryApache2 = service.newEntry(new Dn("ou=users,dc=external,dc=" + dc + ",dc=no"));

                Entry entryApache = service.newEntry(dnApache);
                entryApache.add("objectClass", "top", "domain", "extensibleObject");
                entryApache.add("dc", dc);
                service.getAdminSession().add(entryApache);
                entryApache2.add("objectClass", "top", "organizationalUnit");
                service.getAdminSession().add(entryApache2);


            }

/**
            String entryLdif =
                    "dn: " + entryApache2  + "\n" +
                            "dc: " + dnApache + "\n" +
                            "objectClass: top\n" +
                            "objectClass: domain\n\n";
            importLdifContent(service,entryLdif);
 */
        } catch (Exception e) {
            throw new RuntimeException("init failed", e);
        }

        //server = new LdapServer();
        //server.setTransports(new TcpTransport("localhost", DEFAULT_SERVER_PORT));
        //server.setDirectoryService(service);
    }


    private static void importLdifContent(DirectoryService directoryService, String ldifContent) throws Exception {
        LdifReader ldifReader = new LdifReader(IOUtils.toInputStream(ldifContent));

        try {
            for (LdifEntry ldifEntry : ldifReader) {
                try {
                    directoryService.getAdminSession().add(new DefaultEntry(directoryService.getSchemaManager(), ldifEntry.getEntry()));
                } catch (LdapEntryAlreadyExistsException ignore) {
                    logger.info("Entry " + ldifEntry.getDn() + " already exists. Ignoring");
                }
            }
        } finally {
            ldifReader.close();
        }
    }

    /**
     * Initialize the server. It creates the partition, adds the index, and
     * injects the context entries for the created partitions.
     *
     * @param workDir the directory to be used for storing the data
     * @throws Exception if there were some problems while initializing the system
     */
    private void initDirectoryService(File workDir) {
        if (!workDir.exists()) {
            boolean dirsCreated = workDir.mkdirs();
            if (!dirsCreated) {
                logger.debug("Not all directories could be created. " + workDir.getAbsolutePath());
            }
        }
        init(workDir.getPath());

        // And start the identity
        // service.startup();
    }


    /**
     * Creates a new instance of EmbeddedADS. It initializes the directory identity.
     *
     * @throws Exception If something went wrong
     */
    public EmbeddedADS(File workDir) {
        initDirectoryService(workDir);
    }

    /**
     * Creates a new instance of EmbeddedADS. It initializes the directory identity.
     *
     * @throws Exception If something went wrong
     */
    public EmbeddedADS(String workDir) {
        this(new File(workDir));
    }

    /*
    public void startServer() throws Exception {
        startServer(DEFAULT_SERVER_PORT);
    }
    */

    /**
     * starts the LdapServer
     *
     * @throws Exception
     */
    public void startServer(int serverPort) {
        logger.info("Starting embedded ApacheDS");

        try {
            server = new LdapServer();
            server.setTransports(new TcpTransport(serverPort));
            server.setDirectoryService(service);
            server.start();
            logger.info("Apache DS Started, port: {}", serverPort);
            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    try {
                        server.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            });
        } catch (Exception e) {
            throw new RuntimeException("startServer failed", e);
        }
    }

    public void stopServer() {
        stopLdapServer();
        try {
            shutdownDirectoryService();
        } catch (Exception e) {
            logger.error("Error shutting down DirectoryService.", e);
        }
        //directoryService.getCacheService().destroy();

        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
    }


    protected void stopLdapServer() {
        logger.info("Stopping LDAP server.");
        server.stop();
    }

    protected void shutdownDirectoryService() throws Exception {
        logger.info("Stopping Directory service.");
        DirectoryService directoryService = server.getDirectoryService();
        directoryService.shutdown();

        // Delete workfiles just for 'inmemory' implementation used in tests. Normally we want LDAP data to persist
        File instanceDir = directoryService.getInstanceLayout().getInstanceDirectory();
        if (false) {
            logger.info("Removing Directory service workfiles: %s", instanceDir.getAbsolutePath());
            FileUtils.deleteDirectory(instanceDir);
        } else {
            logger.info("Working LDAP directory not deleted. Delete it manually if you want to start with fresh LDAP data. Directory location: " + instanceDir.getAbsolutePath());
        }
    }


    /**
     * Main class for standalone test and configuration of ApacheDS embedded.
     *
     * @param args first arg is working directory. Default is used if not specified.
     */
    public static void main(String[] args) {
        try {
            String ldappath;
            if (args.length == 0) {
                ldappath = System.getProperty("user.home") + "/bootstrapdata/ldaptest" + UUID.randomUUID().toString();
            } else {
                ldappath = args[0];
            }

            logger.info("LDAP working directory={}", ldappath);
            File workDir = new File(ldappath);

            boolean isDirCreated = workDir.mkdirs();
            if (isDirCreated) {
                // Create the server
                EmbeddedADS ads = new EmbeddedADS(workDir);
                ads.init(workDir.getPath());
                // optionally we can start a server too
                ads.startServer(DEFAULT_SERVER_PORT);


                // Read an entry
                Entry result = ads.service.getAdminSession().lookup(new Dn("dc=external,dc=WHYDAH,dc=no"));

                // And print it if available
                logger.info("Found entry : " + result);
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }
}
