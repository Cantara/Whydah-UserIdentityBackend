package net.whydah.identity.user.signup;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.sso.user.types.UserAggregate;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by baardl on 01.10.15.
 */
public class UserSignupServiceTest  {
    ObjectMapper objectMapper = new ObjectMapper();
    List<UserApplicationRoleEntry> defaultRoleList = new LinkedList<>();
    String userId = "testId1";

    @Before
    public void setUp() throws Exception {
        UserApplicationRoleEntry defaultRole = new UserApplicationRoleEntry();
        defaultRole.setUserId(userId);
        defaultRole.setOrgName("whydah");
        defaultRole.setApplicationName("default");
        defaultRole.setRoleName("whydah-user");
        defaultRole.setRoleValue("enabled");

        defaultRoleList.add(defaultRole);
    }

    @Test
    public void testCreateUserWithRoles() throws Exception {
        UserAggregate userAggregate = objectMapper.readValue(userWithoutRolesJson, UserAggregate.class);
        userAggregate.setUid(userId);
        userAggregate.setRoleList(defaultRoleList);
        String userWithRole = objectMapper.writeValueAsString(userAggregate);
        assertNotNull(userWithRole);
        assertTrue(userWithRole.contains("whydah-user"));
    }

    private String userWithoutRolesJson = "{\"username\":\"helloMe\", \"firstName\":\"hello\", \"lastName\":\"meLast\", \"personRef\":\"\", \"email\":\"hello.me@example.com\", \"cellPhone\":\"+47 90221133\"}";
}