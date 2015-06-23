package net.whydah.identity.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Created by baardl on 29.03.14.
 */
@Path("/{applicationtokenid}/{userTokenId}/application")
public class ApplicationResource {
    private static final Logger log = LoggerFactory.getLogger(ApplicationResource.class);
    ApplicationService applicationService;
    ObjectMapper mapper = new ObjectMapper();


    @Autowired
    public ApplicationResource(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * Create a new application from json
     * Add default
     *
     * @param applicationJson  json representing an Application
     * @return  Application
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createApplication(String applicationJson)  {
        log.trace("createApplication is called with applicationJson={}", applicationJson);
        Application application;
        try {
            application = fromJson(applicationJson);
        } catch (IllegalArgumentException iae) {
            log.error("createApplication: Invalid json={}", applicationJson, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Application persisted;
        try {
            persisted = applicationService.createApplication(application);
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
            
        /*
        try {
            application = applicationService.createApplication(applicationJson);
            //return Response.status(Response.Status.OK).build();
        } catch (IllegalArgumentException iae) {
            log.error("createApplication: Invalid json={}", applicationJson, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (IllegalStateException ise) {
            log.error(ise.getMessage());
            return Response.status(Response.Status.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        */

        if (persisted != null) {
            String applicationCreatedJson = buildApplicationJson(application);
            return Response.ok(applicationCreatedJson).build();
        } else {
            //TODO If it was not persisted, should return error message
            return Response.status(Response.Status.NO_CONTENT).build();
        }
    }

    @GET
    @Path("/{applicationId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplication(@PathParam("applicationId") String applicationId){
        log.trace("getApplication is called with applicationId={}", applicationId);
        try {
            Application application = applicationService.getApplication(applicationId);
            String applicationCreatedJson = buildApplicationJson(application);
            return Response.ok(applicationCreatedJson).build();
        } catch (IllegalArgumentException iae) {
            log.error("createApplication: Invalid json={}", applicationId, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (IllegalStateException ise) {
            log.error(ise.getMessage());
            return Response.status(Response.Status.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    protected String buildApplicationJson(Application application) {
        String applicationCreatedJson = null;
        try {
            applicationCreatedJson = mapper.writeValueAsString(application);
        } catch (IOException e) {
            log.warn("Could not convert application to Json {}", application.toString());
        }
        return applicationCreatedJson;
    }

    public static Application fromJson(String applicationJson) throws  IllegalArgumentException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Application application = mapper.readValue(applicationJson, Application.class);
            return application;
        } catch (IOException e) {
            throw new IllegalArgumentException("Error mapping json for " + applicationJson, e);
        }
    }

}
