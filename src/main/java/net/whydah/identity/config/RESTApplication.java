package net.whydah.identity.config;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-05-27
 */
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("dc")
public class RESTApplication extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(RESTApplication.class);

    public RESTApplication() {
        //https://java.net/jira/browse/JERSEY-2175
        ResourceConfig resourceConfig = packages("net.whydah.identity.user.resource", "net.whydah.identity.application", "net.whydah.identity.health");
        resourceConfig.register(org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature.class);
        //resourceConfig.register(CollectionJsonReaderAndWriter.class);
        //resourceConfig.register(MultiPartFeature.class);
        log.debug(this.getClass().getSimpleName() + " started!");
    }
}
