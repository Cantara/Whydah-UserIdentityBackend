package net.whydah.identity.user.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.user.InvalidRoleModificationException;
import net.whydah.identity.user.NonExistentRoleException;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.InvalidUserIdentityFieldException;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.identity.UserIdentityRepresentation;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.role.UserPropertyAndRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;

/**
 * Administration of users and their data.
 */
@Component
@Path("/{applicationtokenid}/{userTokenId}/user")
public class UserResource {
    private static final Logger log = LoggerFactory.getLogger(UserResource.class);

    private final UserIdentityService userIdentityService;
    private final UserAggregateService userAggregateService;
    private final ObjectMapper mapper;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public UserResource(UserIdentityService userIdentityService, UserAggregateService userAggregateService, ApplicationDao applicationDao) {
        this.userIdentityService = userIdentityService;
        this.userAggregateService = userAggregateService;
        this.mapper = new ObjectMapper();
        log.info("Started: UserResource");
    }

    /**
     * Expectations to input:
     * no UID
     * no password
     * <p/>
     * Output:
     * uid is included
     * no password
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addUserIdentity(String userIdentityJson) {
        log.trace("addUserIdentity, userIdentityJson={}", userIdentityJson);

        UserIdentityRepresentation representation;
        try {
            representation = mapper.readValue(userIdentityJson, UserIdentityRepresentation.class);
        } catch (IOException e) {
            String msg = "addUserIdentity, invalid json";
            log.info(msg + ". userIdentityJson={}", userIdentityJson, e);
            return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
        }

        UserIdentity userIdentity;
        try {
            userIdentity = userIdentityService.addUserIdentityWithGeneratedPassword(representation);
        } catch (IllegalStateException conflictException) {
            Response response = Response.status(Response.Status.CONFLICT).entity(conflictException.getMessage()).build();
            log.info("addUserIdentity returned {} {} because {}. \njson {}",
                    response.getStatusInfo().getStatusCode(), response.getStatusInfo().getReasonPhrase(), conflictException.getMessage(), userIdentityJson);
            return response;
        } catch (IllegalArgumentException|InvalidUserIdentityFieldException badRequestException) {
            Response response = Response.status(Response.Status.BAD_REQUEST).entity(badRequestException.getMessage()).build();
            log.info("addUserIdentity returned {} {} because {}. \njson {}",
                    response.getStatusInfo().getStatusCode(), response.getStatusInfo().getReasonPhrase(), badRequestException.getMessage(), userIdentityJson);
            return response;
        } catch (RuntimeException e) {
            log.error("addUserIdentity-RuntimeExeption ", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        try {
            String newUserAsJson;
            newUserAsJson = mapper.writeValueAsString(userIdentity);
            //TODO Ensure password is not returned. Expect UserAdminService to trigger resetPassword.
            return Response.status(Response.Status.CREATED).entity(newUserAsJson).build();
        } catch (IOException e) {
            log.error("Error converting to json. {}", userIdentity.toString(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{uid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserIdentity(@PathParam("uid") String uid) {
        log.trace("getUserIdentity for uid={}", uid);

        UserIdentity userIdentity;
        try {
            userIdentity = userIdentityService.getUserIdentityForUid(uid);
            log.trace("getUserIdentity for uid={} found user={}", uid, (userIdentity != null ? userIdentity.toString() : "null"));
        } catch (NamingException e) {
            throw new RuntimeException("getUserIdentityForUid, uid=" + uid, e);
        }
        if (userIdentity == null) {
            log.trace("getUserIdentityForUid could not find user with uid={}", uid);
            return Response.status(Response.Status.NOT_FOUND).build();
        }


        String json;
        try {
            json = mapper.writeValueAsString(userIdentity);
        } catch (IOException e) {
            log.error("Error converting to json. {}", userIdentity.toString(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(json).build();
    }

    @PUT
    @Path("/{uid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUserIdentity(@PathParam("uid") String uid, String userIdentityJson) {
        log.trace("updateUserIdentity: uid={}, userIdentityJson={}", uid, userIdentityJson);

        UserIdentity userIdentity;
        try {
            userIdentity = mapper.readValue(userIdentityJson, UserIdentity.class);
        } catch (IOException e) {
            log.error("updateUserIdentityForUsername, invalid json. userIdentityJson={}", userIdentityJson, e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            UserIdentity updatedUserIdentity = userAggregateService.updateUserIdentity(uid, userIdentity);

            try {
                String json = mapper.writeValueAsString(updatedUserIdentity);
                return Response.ok(json).build();
            } catch (IOException e) {
                log.error("Error converting to json. {}", userIdentity.toString(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (InvalidUserIdentityFieldException iuife) {
            log.warn("updateUserIdentity returned {} because {}.", Response.Status.BAD_REQUEST.toString(), iuife.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (IllegalArgumentException iae) {
            log.info("updateUserIdentity: Invalid json={}", userIdentityJson, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (RuntimeException e) {
            log.error("updateUserIdentity: RuntimeError json={}", userIdentityJson, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{uid}")
    public Response deleteUserIdentityAndRoles(@PathParam("uid") String uid) {
        log.trace("deleteUserIdentityAndRoles: uid={}", uid);

        try {
            userAggregateService.deleteUserAggregateByUid(uid);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (IllegalArgumentException iae) {
            log.error("deleteUserIdentity failed username={}", uid + ". " + iae.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }




    // ROLES


    @POST
    @Path("/{uid}/role/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addRole(@PathParam("uid") String uid, String roleJson) {
        log.trace("addRole, roleJson={}", roleJson);

        RoleRepresentationRequest request;
        try {
            request = mapper.readValue(roleJson, RoleRepresentationRequest.class);
        } catch (IOException e) {
            log.error("addRole, invalid json. roleJson={}", roleJson, e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            UserPropertyAndRole updatedRole = userAggregateService.addRole(uid, request);

            String json;
            try {
                json = mapper.writeValueAsString(updatedRole);
            } catch (IOException e) {
                log.error("Error converting to json. {}", updatedRole.toString(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.status(Response.Status.CREATED).entity(json).build();
        } catch (WebApplicationException ce) {
            log.error("addRole-Conflict. {}", roleJson, ce);
            //return Response.status(Response.Status.CONFLICT).build();
            return ce.getResponse();
        } catch (RuntimeException e) {
            log.error("addRole-RuntimeException. {}", roleJson, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{uid}/roles")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoles(@PathParam("uid") String uid) {
        log.trace("getRoles, uid={}", uid);

        List<UserPropertyAndRole> roles = userAggregateService.getRoles(uid);

        String json;
        try {
            json = mapper.writeValueAsString(roles);
        } catch (IOException e) {
            log.error("Error converting List<UserPropertyAndRole> to json. ", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(json).build();
    }

    @GET
    @Path("/{uid}/role/{roleid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRole(@PathParam("uid") String uid, @PathParam("roleid") String roleid) {
        log.trace("getRole, uid={}, roleid={}", uid, roleid);

        UserPropertyAndRole role = userAggregateService.getRole(uid, roleid);
        if (role == null) {
            log.trace("getRole could not find role with roleid={}", roleid);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String json;
        try {
            json = mapper.writeValueAsString(role);
        } catch (IOException e) {
            log.error("Error converting to json. {}", role.toString(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(json).build();
    }

    @PUT
    @Path("/{uid}/role/{roleid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateRole(@PathParam("uid") String uid, @PathParam("roleid") String roleid, String roleJson) {
        log.trace("updateRole, uid={}, roleid={}", uid, roleid);

        UserPropertyAndRole role;
        try {
            role = mapper.readValue(roleJson, UserPropertyAndRole.class);
        } catch (IOException e) {
            log.error("updateRole, invalid json. roleJson={}", roleJson, e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            String json;
            UserPropertyAndRole updatedRole = userAggregateService.updateRole(uid, roleid, role);
            try {
                json = mapper.writeValueAsString(updatedRole);
            } catch (IOException e) {
                log.error("Error converting to json. {}", updatedRole.toString(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok(json).build();
        } catch (NonExistentRoleException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (InvalidRoleModificationException e) {
            return Response.status(Response.Status.fromStatusCode(422)).entity(e.getMessage()).build();
        } catch (RuntimeException e) {
            log.error("updateRole-RuntimeException. {}", roleJson, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{uid}/role/{roleid}")
    public Response deleteRole(@PathParam("uid") String uid, @PathParam("roleid") String roleid) {
        log.trace("deleteRole, uid={}, roleid={}", uid, roleid);

        try {
            userAggregateService.deleteRole(uid, roleid);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (RuntimeException e) {
            log.error("deleteRole-RuntimeException. roleId {}", roleid, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
