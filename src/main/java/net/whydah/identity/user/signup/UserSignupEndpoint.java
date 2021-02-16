package net.whydah.identity.user.signup;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.types.UserAggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Service for authorization of users and finding UIBUserAggregate with corresponding applications, organizations and roles.
 * This not a RESTful endpoint. This is a http RPC endpoint.
 */
@Component
@Path("/{applicationTokenId}/signup")
public class UserSignupEndpoint {
    private static final Logger log = LoggerFactory.getLogger(UserSignupEndpoint.class);

//    private final UserAggregateService userAggregateService;
//    private final UserAdminHelper userAdminHelper;
    private final UserSignupService userSignupService;
    private final ObjectMapper objectMapper;
    private final AuditLogDao auditLogDao;

    @Autowired
    public UserSignupEndpoint(UserSignupService userSignupService, ObjectMapper objectMapper, AuditLogDao auditLogDao) {
        this.userSignupService = userSignupService;
        this.objectMapper = objectMapper;
        this.auditLogDao = auditLogDao;
    }


    /**
     * Signup using json.  Format
     {"username":"helloMe", "firstName":"hello", "lastName":"me", "personRef":"", "email":"hello.me@example.com", "cellPhone":"+47 90221133"}
     username is required
     */
    @Path("/user")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response signupUser(String userJson) {
        log.trace("signupUser: {}", userJson);
        UserAggregate createFromRepresentation = null;
//        try {
//            createFromRepresentation = objectMapper.readValue(userJson, UserAggregate.class);
//        } catch (IOException ioe) {
//            log.trace("Failed to parse UIBUserAggregateRepresentation from json {}", userJson);
//        }
        
        createFromRepresentation = UserAggregateMapper.fromJson(userJson);

        if (createFromRepresentation == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Could not parse " + userJson + ".").build();
        }

        UserAggregate userAggregate = userSignupService.createUserWithRoles(createFromRepresentation);
        String createdUserAsJson = null;
        if (userAggregate != null) {
            createdUserAsJson = UserAggregateMapper.toJson(userAggregate);
        }

//        if (userIdentity == null) {
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, could not parse input.</error>").build();
//        }


        /*
        String facebookUserAsString = getFacebookDataAsXmlString(fbUserDoc);
        //String facebookUserAsString = getFacebookDataAsXmlString(input);
        return createAndAuthenticateUser(userIdentity, facebookUserAsString, true);
        */
        return Response.status(Response.Status.OK).entity(createdUserAsJson).build();
    }


}
