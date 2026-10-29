package net.whydah.identity.application;

import net.whydah.identity.application.search.LuceneApplicationSearch;
import net.whydah.identity.health.HealthResource;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static net.whydah.sso.util.LoggerUtil.first50;


/**
 * CRUD, http endpoint for Application
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a>
 */
@Path("/{applicationtokenid}/{userTokenId}/application")
public class ApplicationResource {
    private static final Logger log = LoggerFactory.getLogger(ApplicationResource.class);
    private final ApplicationService applicationService;
    private final LuceneApplicationSearch luceneApplicationSearch;


    @Autowired
    public ApplicationResource(ApplicationService applicationService, LuceneApplicationSearch luceneApplicationSearch) {
        this.applicationService = applicationService;
        this.luceneApplicationSearch=luceneApplicationSearch;

    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createApplication(String applicationJson)  {
        log.trace("create is called with applicationJson={}", applicationJson);
        Application application;
        try {
            application = ApplicationMapper.fromJson(applicationJson);
        } catch (IllegalArgumentException iae) {
            log.error("create: Invalid json={}", applicationJson, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            Application persisted = applicationService.create(application);
            if (persisted != null) {
                String json = ApplicationMapper.toJson(persisted);
                HealthResource.setNumberOfApplications(luceneApplicationSearch.getApplicationIndexSize());

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
            if (application != null) {
                log.debug("application {}", first50(application.toString()));
            }
            if (application == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String json = ApplicationMapper.toJson(application);
            log.debug("applicationJson {}", first50(json));
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
            application = ApplicationMapper.fromJson(applicationJson);
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
                    HealthResource.setNumberOfApplications(luceneApplicationSearch.getApplicationIndexSize());

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
