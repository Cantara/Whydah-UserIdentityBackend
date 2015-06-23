package net.whydah.identity.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@Component
@Path("/{applicationtokenid}/{userTokenId}/")
public class ApplicationsResource {
    private static final Logger log = LoggerFactory.getLogger(ApplicationsResource.class);
    ApplicationService applicationService;
    ObjectMapper mapper = new ObjectMapper();


    @Autowired
    public ApplicationsResource(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }


    //ED: Does this belong here?
    @POST
    @Path("/verifyApplicationAuth")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response verifyApplicationAuth(String applicationCredential) {
        log.trace("verifyApplicationAuth is called ");

        //FIXME check applicationSecret against applicationID
        return Response.ok().build();

    }


    @GET
    @Path("/applications")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplications(){
        log.trace("getApplications is called ");
        try {
            List<String> availableOrgNames =  new LinkedList<>();
            List<String> availableRoleNames =  new LinkedList<>();
            List<Application> applications = applicationService.getApplications();
            for (Application a : applications) {
                if (!availableOrgNames.contains(a.getDefaultOrgName())) {
                    availableOrgNames.add(a.getDefaultOrgName());
                }
                if (!availableRoleNames.contains(a.getDefaultRoleName())) {
                    availableRoleNames.add(a.getDefaultRoleName());
                }
            }
            for (Application application : applications) {
                application.setAvailableOrgNames(availableOrgNames);
                //application.setAvailableRoleNames(availableRoleNames);    //TODO
            }

            String applicationCreatedJson = buildApplicationsJson(applications);
            log.trace("Returning applications: "+applicationCreatedJson);
            return Response.ok(applicationCreatedJson).build();
        } catch (IllegalArgumentException iae) {
            log.error("getApplications: Invalid json.",  iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (IllegalStateException ise) {
            log.error(ise.getMessage());
            return Response.status(Response.Status.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    protected String buildApplicationsJson(List<Application> applications) {
        String applicationsCreatedJson = null;
        try {
            applicationsCreatedJson = mapper.writeValueAsString(applications);
            log.debug("Applications.json:",applicationsCreatedJson);
        } catch (IOException e) {
            log.warn("Could not convert application to Json {}", applications.toString());
        }
        return applicationsCreatedJson;
    }
}

