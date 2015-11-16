package net.whydah.identity.application;

import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Component
@Path("/{applicationtokenid}/{userTokenId}/")
public class ApplicationsResource {
    private static final Logger log = LoggerFactory.getLogger(ApplicationsResource.class);
    private final ApplicationService applicationService;

    @Autowired
    public ApplicationsResource(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GET
    @Path("/applications")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplications(){
        log.trace("getApplications is called.");
        try {
            List<Application> applications = applicationService.getApplications();
            String json = ApplicationMapper.toJson(applications);
            log.trace("Returning {} applications: {}", applications.size(), json);
            return Response.ok(json).build();
        } catch (RuntimeException e) {
            log.error("getApplications failed.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}

