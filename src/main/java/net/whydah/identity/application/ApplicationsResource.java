package net.whydah.identity.application;

import static net.whydah.sso.util.LoggerUtil.first50;

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;


@Component
@Path("/{applicationtokenid}/")
public class ApplicationsResource {
    private static final Logger log = LoggerFactory.getLogger(ApplicationsResource.class);
    private final ApplicationService applicationService;
    
    @Autowired
    public ApplicationsResource(ApplicationService applicationService) throws IOException {
        this.applicationService = applicationService;       
    }

  
    @GET
    @Path("/applications/find/{q}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response findUsersNoUserToken(@PathParam("q") String query) {
        log.info("findApplications with query=" + query);
        List<Application> applications = applicationService.search(query);
        String json = ApplicationMapper.toSafeJson(applications);
        log.info("findApplications - Returning {} applications: {}", applications.size(), first50(json));
        Response response = Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=utf-8").build();
        return response;
    }

  
    @GET
    @Path("/find/applications/{q}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response findUsersNoUserToken2(@PathParam("q") String query) {
        log.info("findApplications with query=" + query);
        List<Application> applications = applicationService.search(query);
        String json = ApplicationMapper.toSafeJson(applications);
        log.info("findApplications - Returning {} applications: {}", applications.size(), first50(json));
        Response response = Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=utf-8").build();
        return response;
    }


    @GET
    @Path("/applications")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplications(){
        log.trace("getApplications is called.");
        try {
            List<Application> applications = applicationService.getApplications();
            if (applications != null && applications.size() > 0) {
                log.trace("application [0] - {}", first50(applications.get(0).toString()));
            }
            String json = ApplicationMapper.toJson(applications);
            log.trace("Returning {} applications: {}", applications.size(), first50(json));
            return Response.ok(json).build();
        } catch (RuntimeException e) {
            log.error("getApplications failed.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * Find users.
     *
     * @param query Application query.
     * @return json response.
     */
    @GET
    @Path("/{userTokenId}/applications/find/{q}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response findUsers(@PathParam("q") String query) {
        log.info("findApplications with query=" + query);
        List<Application> applications = applicationService.search(query);
        String json = ApplicationMapper.toJson(applications);
        log.info("findApplications - Returning {} applications: {}", applications.size(), first50(json));
        Response response = Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=utf-8").build();
        return response;
    }

}

