package net.whydah.identity.application;

import net.whydah.sso.application.Application;
import net.whydah.sso.application.ApplicationSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * CRUD, http endpoint for Application
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a>
 */
@Path("/{applicationtokenid}/{userTokenId}/application")
public class ApplicationResource {
    private static final Logger log = LoggerFactory.getLogger(ApplicationResource.class);
    private final ApplicationService applicationService;

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
        log.trace("create is called with applicationJson={}", applicationJson);
        Application application;
        try {
            application = ApplicationSerializer.fromJson(applicationJson);
        } catch (IllegalArgumentException iae) {
            log.error("create: Invalid json={}", applicationJson, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            Application persisted = applicationService.create(application);
            if (persisted != null) {
                String json = ApplicationSerializer.toJson(persisted);
                return Response.ok(json).build();
            }
        } catch (RuntimeException e) {
            log.error("", e);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    @GET
    @Path("/{applicationId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplication(@PathParam("applicationId") String applicationId){
        log.trace("getApplication is called with applicationId={}", applicationId);
        try {
            Application application = applicationService.getApplication(applicationId);
            if (application == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String json = ApplicationSerializer.toJson(application);
            return Response.ok(json).build();
        } catch (IllegalArgumentException iae) {
            log.error("create: Invalid json={}", applicationId, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (IllegalStateException ise) {
            log.error(ise.getMessage());
            return Response.status(Response.Status.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PUT
    @Path("/{applicationId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateApplication(@PathParam("applicationId") String applicationId, String applicationJson)  {
        log.trace("updateApplication applicationId={}, applicationJson={}", applicationId, applicationJson);
        Application application;
        try {
            application = ApplicationSerializer.fromJson(applicationJson);
        } catch (IllegalArgumentException iae) {
            log.error("create: Invalid json={}", applicationJson, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            int numRowsAffected = applicationService.update(application);
            switch(numRowsAffected) {
                case 0 :
                    return Response.status(Response.Status.NOT_FOUND).build();
                case 1 :
                    return Response.status(Response.Status.NO_CONTENT).build();
                default:
                    log.error("numRowsAffected={}, if more than one row was updated, this means that the database is in an unexpected state. Manual correction is needed!", numRowsAffected);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{applicationId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteApplication(@PathParam("applicationId") String applicationId){
        log.trace("deleteApplication is called with applicationId={}", applicationId);

        try {
            int numRowsAffected = applicationService.delete(applicationId);
            switch(numRowsAffected) {
                case 0 :
                    return Response.status(Response.Status.NOT_FOUND).build();
                case 1 :
                    return Response.status(Response.Status.NO_CONTENT).build();
                default:
                    log.error("numRowsAffected={}, if more than one row was deleted, this means that the database is in an unexpected state. Data may be lost!", numRowsAffected);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
