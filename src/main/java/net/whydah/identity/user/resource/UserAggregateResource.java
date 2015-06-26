package net.whydah.identity.user.resource;


import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.identity.user.UserAggregate;
import net.whydah.identity.user.UserAggregateService;
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

/**
 * Endpoint for useraggregate.
 */
@Component
@Path("/{applicationtokenid}/{usertokenid}/useraggregate")
public class UserAggregateResource {
    private static final Logger log = LoggerFactory.getLogger(UserAggregateResource.class);

    private final UserAggregateService userAggregateService;
    private final ObjectMapper mapper;


    @Context
    private UriInfo uriInfo;

    @Autowired
    public UserAggregateResource(UserAggregateService userAggregateService) {
        this.userAggregateService = userAggregateService;
        this.mapper = new ObjectMapper();
    }

    @GET
    @Path("/{uid}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response getUserAggregate(@PathParam("uid") String uid) {
        log.trace("getUserAggregateByUsernameOrUid with uid={}", uid);

        UserAggregate user;
        try {
            user = userAggregateService.getUserAggregateByUsernameOrUid(uid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("no user with uid=" + uid).build();
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
}
