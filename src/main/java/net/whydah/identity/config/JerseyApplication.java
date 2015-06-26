package net.whydah.identity.config;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-05-27
 */
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("overriddenByWebXml")
public class JerseyApplication extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(JerseyApplication.class);

    public JerseyApplication() {
        //https://java.net/jira/browse/JERSEY-2175
        //Looks like recursive scanning is not working when specifying multiple packages.
        ResourceConfig resourceConfig = packages("net.whydah.identity");
        resourceConfig.register(org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature.class);
        resourceConfig.property(FreemarkerMvcFeature.TEMPLATE_BASE_PATH, "/templates");
        log.debug(this.getClass().getSimpleName() + " started!");
    }
}
