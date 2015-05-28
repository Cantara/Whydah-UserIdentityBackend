package net.whydah.identity.user;

import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.resource.UserAggregateRepresentation;
import net.whydah.identity.user.role.UserPropertyAndRole;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;


/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 06/04/14
 */
public class UserAggregateServiceTest {
    private static final Logger log = LoggerFactory.getLogger(UserAggregateServiceTest.class);
    /*
{
  "uid": "uid",
  "username": "usernameABC",
  "firstName": "firstName",
  "lastName": "lastName",
  "personRef": "personRef",
  "email": "email",
  "cellPhone": "12345678",
  "password": "password",
  "roles": [
    {
      "applicationId": "applicationId",
      "applicationName": "applicationName",
      "organizationId": "organizationId",
      "organizationName": "organizationName",
      "applicationRoleName": "roleName",
      "applicationRoleValue": "email"
    },
    {
      "applicationId": "applicationId123",
      "applicationName": "applicationName123",
      "organizationId": "organizationId123",
      "organizationName": "organizationName123",
      "applicationRoleName": "roleName123",
      "applicationRoleValue": "roleValue123"
    }
  ]
}
     */


    /*
    {
  "identity": {
    "uid": "uid",
    "username": "username123",
    "firstName": "firstName",
    "lastName": "lastName",
    "personRef": "personRef",
    "email": "email",
    "cellPhone": "12345678",
    "personName": "firstName lastName",
    "password": "password"
  },
  "userPropertiesAndRolesList": [
    {
      "uid": "uid",
      "applicationId": "applicationId",
      "orgId": "organizationId",
      "applicationRoleName": "roleName",
      "applicationRoleValue": "email",
      "organizationName": "organizationName",
      "applicationName": "applicationName"
    }
  ]
}
     */

    @Test   //ED: In-progress
    public void testJsonFromUserAggregate() throws IOException {
        String username = "usernameABC";
        UserIdentity userIdentity = new UserIdentity("uid", username, "firstName", "lastName", "email", "password", "12345678", "personRef"
        );

        UserPropertyAndRole role = new UserPropertyAndRole();
        role.setUid(userIdentity.getUid());
        role.setApplicationId("applicationId");
        role.setApplicationName("applicationName");
        role.setOrganizationName("organizationName");
        role.setApplicationRoleName("roleName");
        //role.setRoleValue(roleValue);
        role.setApplicationRoleValue(userIdentity.getEmail());  // Provide NetIQ identity as rolevalue

        UserPropertyAndRole role2 = new UserPropertyAndRole();
        role2.setUid(userIdentity.getUid());
        role2.setApplicationId("applicationId123");
        role2.setApplicationName("applicationName123");
        role2.setOrganizationName("organizationName123");
        role2.setApplicationRoleName("roleName123");
        //role.setRoleValue(roleValue);
        role2.setApplicationRoleValue("roleValue123");

        List<UserPropertyAndRole> roles = new ArrayList<>(2);
        roles.add(role);
        roles.add(role2);
        UserAggregate userAggregate = new UserAggregate(userIdentity, roles);

        UserAggregateRepresentation userRepresentation = UserAggregateRepresentation.fromUserAggregate(userAggregate);

        ObjectMapper objectMapper = new ObjectMapper();
        Writer strWriter = new StringWriter();
        objectMapper.writeValue(strWriter, userRepresentation);
        String json = strWriter.toString();
        assertNotNull(json);
        log.debug("json: {}", json);
    }
}