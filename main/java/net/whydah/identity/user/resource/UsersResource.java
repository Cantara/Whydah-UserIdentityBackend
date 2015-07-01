package net.whydah.identity.user.resource;


import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.UserIdentityRepresentation;
import net.whydah.identity.user.search.LuceneSearch;
import net.whydah.identity.user.search.UserSearch;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.List;

/**
 * Endpoint for collection of users.
 */
@Component
@Path("/{applicationtokenid}/{usertokenid}/users")
public class UsersResource {
    private static final Logger log = LoggerFactory.getLogger(UsersResource.class);

    private final UserAggregateService userAggregateService;
    private final LuceneSearch luceneSearch;
    private final UserSearch userSearch;
    private final ObjectMapper mapper;


    @Context
    private UriInfo uriInfo;

    @Autowired
    public UsersResource(LuceneSearch luceneSearch, UserAggregateService userAggregateService, UserSearch userSearch) {
        this.luceneSearch = luceneSearch;
        this.userAggregateService = userAggregateService;
        this.userSearch = userSearch;
        this.mapper = new ObjectMapper();
    }

    /*
     * Get user details.
     *
     *
     * @param username Username
     * @return user details and roles.
     */
    /*
    @Deprecated //ED: I think this endpoint should be removed! Use /useraggregate/uid instead...
    @GET
    @Path("/username/{username}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response getUserAggregate(@PathParam("username") String username) {
        log.trace("getUserAggregateByUsername with username=" + username);

        UserAggregate user;
        try {
            user = userAggregateService.getUserAggregateByUsernameOrUid(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }


        HashMap<String, Object> model = new HashMap<>(2);
        model.put("user", user);
        model.put("userbaseurl", uriInfo.getBaseUri());
        return Response.ok(new Viewable("/useradmin/user.json.ftl", model)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=utf-8").build();
    }
    */

    /**
     * Find users.
     *
     * @param query User query.
     * @return json response.
     */
    @GET
    @Path("/find/{q}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response findUsers(@PathParam("q") String query) {
        log.trace("findUsers with query=" + query);
        List<UserIdentityRepresentation> users = userSearch.search(query);
        HashMap<String, Object> model = new HashMap<>(2);
        model.put("users", users);
        model.put("userbaseurl", uriInfo.getBaseUri());
        log.trace("findUsers returned {} users.", users.size());
        Response response = Response.ok(new Viewable("/useradmin/users.json.ftl", model)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=utf-8").build();
        return response;
    }


    /**
     * Lookup users in directory  (Intermediate copy of find)
     *
     * @param query User query.
     * @return json response.
     */
    @GET
    @Path("/search/{q}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response directortSearch(@PathParam("q") String query) {
        log.trace("findUsers with query=" + query);
        List<UserIdentityRepresentation> users = userSearch.search(query);
        HashMap<String, Object> model = new HashMap<>(2);
        model.put("users", users);
        model.put("userbaseurl", uriInfo.getBaseUri());
        log.trace("findUsers returned {} users.", users.size());
        Response response = Response.ok(new Viewable("/useradmin/users.json.ftl", model)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=utf-8").build();
        return response;
    }
}
