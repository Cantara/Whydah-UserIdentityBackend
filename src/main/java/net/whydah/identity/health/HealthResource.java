package net.whydah.identity.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Endpoint for health check.
 */
@Component
@Path("/health")
public class HealthResource {
    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);
    private final HealthCheckService healthCheckService;

    @Autowired
    public HealthResource(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response isHealthy() {
        boolean ok = healthCheckService.isOK();
        log.trace("isHealthy={}", ok);
        if (ok) {
            //return Response.status(Response.Status.NO_CONTENT).build();
            return Response.ok("Status OK!").build();
        } else {
            //Intentionally not returning anything the client can use to determine what's the error for security reasons.
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
