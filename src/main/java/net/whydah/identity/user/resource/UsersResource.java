package net.whydah.identity.user.resource;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.search.PaginatedUserAggregateDataList;
import net.whydah.identity.user.search.PaginatedUserIdentityDataList;
import net.whydah.identity.user.search.UserSearch;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.mappers.UserIdentityMapper;
import net.whydah.sso.user.types.UserAggregate;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserIdentity;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.mvc.Viewable;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Endpoint for collection of users.
 */
@Component
@Path("/{applicationtokenid}/{usertokenid}/users")
public class UsersResource {
    private static final Logger log = LoggerFactory.getLogger(UsersResource.class);
    private final UserSearch userSearch;
    private final UserAggregateService userAggregateService; //to help for retrieving roles for users. This is used for export feature

    @Context
    private UriInfo uriInfo;

    @Autowired
    public UsersResource(UserSearch userSearch, UserAggregateService userAggregateService) {
        this.userSearch = userSearch;
        this.userAggregateService = userAggregateService;
    }

    /**
     * Find users.
     * @param query User query.
     * @return json response.
     */
    @GET
    @Path("/find/{q}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response findUsers(@PathParam("q") String query) {
        log.trace("findUsers with query=" + query);
        List<UserIdentity> users = userSearch.search(query);
        HashMap<String, Object> model = new HashMap<>(2);
        model.put("users", users);
        model.put("userbaseurl", uriInfo.getBaseUri());
        log.trace("findUsers returned {} users.", users.size());
        Response response = Response.ok(new Viewable("/useradmin/users.json.ftl", model)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=utf-8").build();
        return response;
    }

    /**
     * Lookup users in directory  (Intermediate copy of find)
     * @param query User query.
     * @return json response.
     */
    @GET
    @Path("/search/{q}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response userSearch(@PathParam("q") String query) {
        return findUsers(query);
    }
    
    //NEW ONE ADDED FOR PAGINATED SEARCHING (WITHOUT ROLES INCLUDED)
    @GET
    @Path("/query/{p}/{q}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response queryUsers(@PathParam("p") String pageNumber, @PathParam("q") String query) {
        log.trace("findUsers with query=" + query);
        PaginatedUserIdentityDataList dl = userSearch.query(Integer.valueOf(pageNumber), query);
        HashMap<String, Object> model = new HashMap<>();
        model.put("currentPage", String.valueOf(dl.pageNumber));
        model.put("pageSize", String.valueOf(dl.pageSize));
        model.put("totalItems", String.valueOf(dl.totalCount));
        model.put("users", dl.data);
        model.put("userbaseurl", uriInfo.getBaseUri());
        log.trace("findUsers returned {} users.", dl.data.size());
        Response response = Response.ok(new Viewable("/useradmin/paginated_users.json.ftl", model)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=utf-8").build();
        return response;
    }
    
    //NEW ONE ADDED FOR PAGINATED EXPORT (WITH ROLES INCLUDED)
    @GET
    @Path("/export/{p}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response exportUsers(@PathParam("p") String pageNumber) {
        log.trace("exportUsers with for the page =" + pageNumber);
        PaginatedUserIdentityDataList dl = userSearch.query(Integer.valueOf(pageNumber), "*");
        PaginatedUserAggregateDataList adl = new PaginatedUserAggregateDataList(dl.pageNumber, dl.totalCount, new ArrayList<UserAggregate>());
        for (UserIdentity ui : dl.data) {
            UserAggregate userAggregate = UserAggregateMapper.fromJson(UserIdentityMapper.toJson(ui));
            List<UserApplicationRoleEntry> userApplicationRoleEntries = userAggregateService.getUserApplicationRoleEntries(userAggregate.getUid());
            userAggregate.setRoleList(userApplicationRoleEntries);
            adl.data.add(userAggregate);
        }
        
        HashMap<String, Object> model = new HashMap<>();
        model.put("currentPage", String.valueOf(adl.pageNumber));
        model.put("pageSize", String.valueOf(adl.pageSize));
        model.put("totalItems", String.valueOf(adl.totalCount));
        model.put("users", adl.data);
        model.put("userbaseurl", uriInfo.getBaseUri());
        log.trace("findUsers returned {} users.", adl.data.size());
        Response response = Response.ok(new Viewable("/useradmin/paginated_exportedusers.json.ftl", model)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=utf-8").build();
        return response;
    }
    
    //NEW ONE ADDED FOR CHECKING DUPLICATE USERS BEFORE IMPORTiNG
    @POST
    @Path("/checkduplicates")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDuplicateUsers(String json) throws JsonProcessingException, IOException, NamingException {
        log.trace("getDuplicateUsers");
      
        ObjectMapper mapper = new ObjectMapper();
		List<String> list = mapper.readValue(json, new TypeReference<ArrayList<String>>() {});
		List<String> returnedList = new ArrayList<String>(); 
		for(String username : list){
			if(userSearch.isUserIdentityIfExists(username)){
				returnedList.add("\"" + username + "\"");	
	        }
		}
		
        return Response.ok("[" + (returnedList.size()>0 ? StringUtils.join(returnedList, ','):"") + "]").build();
    }
    
    @GET
    @Path("/checkexist/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findUserName(@PathParam("username") String query) throws JsonProcessingException, IOException, NamingException {
        log.trace("checkexist");
         boolean exists = userSearch.isUserIdentityIfExists(query);
         JSONObject json=new JSONObject();
         json.put("result", exists);
         return Response.ok(json.toString()).build();
    }
  
    
}
