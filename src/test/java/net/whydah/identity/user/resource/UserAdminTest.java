package net.whydah.identity.user.resource;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import net.whydah.identity.Main;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.dataimport.IamDataImporter;
import net.whydah.identity.user.email.MockMail;
import net.whydah.identity.util.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


public class UserAdminTest {
    private static WebResource baseResource;
    private static WebResource logonResource;
    private static Main main;

    @Before
    public void init() throws Exception {
        System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);
        String ldapPath = AppConfig.appConfig.getProperty("ldap.embedded.directory");
        FileUtils.deleteDirectory(new File(ldapPath));
        FileUtils.deleteDirectory(new File("target/bootstrapdata/"));
        main = new Main(Integer.valueOf(AppConfig.appConfig.getProperty("service.port")));

        DatabaseMigrationHelper dbHelper = main.getInjector().getInstance(DatabaseMigrationHelper.class);
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();

        main.startEmbeddedDS(AppConfig.appConfig.getProperty("ldap.embedded.directory"), Integer.valueOf(AppConfig.appConfig.getProperty("ldap.embedded.port")));
        //main.importUsersAndRoles();
        new IamDataImporter().importIamData();

        String sslVerification = AppConfig.appConfig.getProperty("sslverification");
        String requiredRoleName = AppConfig.appConfig.getProperty("useradmin.requiredrolename");
        main.startHttpServer(sslVerification, requiredRoleName);

        URI baseUri = UriBuilder.fromUri("http://localhost/uib/uib/useradmin/").port(main.getPort()).build();
        URI logonUri = UriBuilder.fromUri("http://localhost/uib/").port(main.getPort()).build();
        //String authentication = "usrtk1";
        baseResource = Client.create().resource(baseUri)/*.path(authentication + '/')*/;
        logonResource = Client.create().resource(logonUri);
    }

    @After
    public void teardown() throws InterruptedException {
        main.stop();
    }

    @Test
    public void find() {
        WebResource webResource = baseResource.path("users/find/Thomas");
        String s = webResource.get(String.class);
        assertTrue(s.contains("\"firstName\":\"Thomas\""));
    }

    @Test
    public void getuser() {
        WebResource webResource = baseResource.path("user/username@emailaddress.com");
        String s = webResource.get(String.class);
        //System.out.println(s);
        assertTrue(s.contains("\"firstName\":\"Thomas\""));
    }

    @Test
    public void getnonexistinguser() {
        WebResource webResource = baseResource.path("user/");
        webResource.path("username@emailaddress.com").get(String.class); // verify that path works with existing user
        try {
            String s = webResource.path("bantelonga@gmail.com").get(String.class);
            fail("Expected 404, got " + s);
        } catch (UniformInterfaceException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    public void modifyUser() {
        String uid = doAddUser("1231312", "siqula", "Hoytahl", "Goffse", "siqula@midget.orj", "12121212");

        String s = baseResource.path("user/" + uid).get(String.class);
        assertTrue(s.contains("siqula@midget.orj"));
        assertTrue(s.contains("Hoytahl"));
        assertTrue(s.contains("12121212"));

        String updateduserjson = "{\n" +
                " \"uid\":\"" + uid + "\",\n" +
                " \"personRef\":\"1231312\",\n" +
                " \"username\":\"siqula\",\n" +
                " \"firstName\":\"Harald\",\n" +
                " \"lastName\":\"Goffse\",\n" +
                " \"email\":\"siqula@midget.orj\",\n" +
                " \"cellPhone\":\"35353535\"\n" +
                "}";

        baseResource.path("user/" + uid).type("application/json").put(String.class, updateduserjson);

        s = baseResource.path("user/" + uid).get(String.class);
        assertTrue(s.contains("siqula@midget.orj"));
        assertTrue(s.contains("Harald"));
        assertFalse(s.contains("Hoytahl"));
        assertTrue(s.contains("35353535"));
        assertFalse(s.contains("12121212"));
    }

    @Test
    public void deleteUser() {
        String uid = doAddUser("rubblebeard", "frustaalstrom", "Frustaal", "Strom", "frustaalstrom@gmail.com", "12121212");

        ClientResponse deleteResponse = baseResource.path("user/" + uid).delete(ClientResponse.class);
        deleteResponse.getClientResponseStatus().getFamily().equals(Response.Status.Family.SUCCESSFUL);

        try {
            String s = baseResource.path(uid).get(String.class);
            fail("Expected 404, got " + s);
        } catch (UniformInterfaceException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    public void deleteUserNotFound() {
        WebResource webResource = baseResource.path("users/dededede@hotmail.com/delete");
        try {
            String s = webResource.get(String.class);
            fail("Expected 404, got " + s);
        } catch (UniformInterfaceException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }

    }


    @Test
    public void getuserroles() {
        String uid = doAddUser("riffraff", "snyper", "Edmund", "Goffse", "snyper@midget.orj", "12121212");
        String roleId1 = doAddUserRole(uid, "testappId1", "0005", "KK", "test");
        String roleId2 = doAddUserRole(uid, "testappIdX", "0005", "NN", "another");
        String roleId3 = doAddUserRole(uid, "testappIdX", "0005", "MM", "yetanother");

        List<Map<String, Object>> roles = doGetUserRoles(uid);
        assertEquals(3, roles.size());

        Map<String, Object> testRole1 = doGetUserRole(uid, roleId1);
        assertEquals("testappId1", testRole1.get("applicationId"));
        assertEquals("0005", testRole1.get("organizationName"));
        assertEquals("KK", testRole1.get("applicationRoleName"));
        assertEquals("test", testRole1.get("applicationRoleValue"));

        Map<String, Object> testRole2 = doGetUserRole(uid, roleId2);
        assertEquals("testappIdX", testRole2.get("applicationId"));
        assertEquals("0005", testRole2.get("organizationName"));
        assertEquals("NN", testRole2.get("applicationRoleName"));
        assertEquals("another", testRole2.get("applicationRoleValue"));

        Map<String, Object> testRole3 = doGetUserRole(uid, roleId3);
        assertEquals("testappIdX", testRole3.get("applicationId"));
        assertEquals("0005", testRole3.get("organizationName"));
        assertEquals("MM", testRole3.get("applicationRoleName"));
        assertEquals("yetanother", testRole3.get("applicationRoleValue"));
    }

    @Test
    public void adduserrole() {
        String uid = doAddUser("riffraff", "snyper", "Edmund", "Goffse", "snyper@midget.orj", "12121212");
        List<Map<String, Object>> rolesBefore = doGetUserRoles(uid);
        assertTrue(rolesBefore.isEmpty());

        String roleId = doAddUserRole(uid, "testappId", "0005", "KK", "test");

        List<Map<String, Object>> rolesAfter = doGetUserRoles(uid);
        assertEquals(1, rolesAfter.size());

        assertEquals(roleId, rolesAfter.get(0).get("roleId"));
    }

    @Test
    public void adduserroleNoJson() {
        String uid = doAddUser("riffraff", "snyper", "Edmund", "Goffse", "snyper@midget.orj", "12121212");
        try {
            String s = baseResource.path("user/" + uid + "/role").type("application/json").post(String.class, "");
            fail("Expected 400, got " + s);
        } catch (UniformInterfaceException e) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    public void adduserroleBadJson() {
        String uid = doAddUser("riffraff", "snyper", "Edmund", "Goffse", "snyper@midget.orj", "12121212");
        try {
            String s = baseResource.path("user/" + uid + "/role").type("application/json").post(String.class, "{ dilldall }");
            fail("Expected 400, got " + s);
        } catch (UniformInterfaceException e) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
        }
    }


    @Test
    public void addExistingUserrole() {
        String uid = doAddUser("riffraff", "snyper", "Edmund", "Goffse", "snyper@midget.orj", "12121212");
        doAddUserRole(uid, "testappId", "0005", "KK", "test");
        try {
            String failedRoleId = doAddUserRole(uid, "testappId", "0005", "KK", "test");
            fail("Expected exception with 409, got roleId " + failedRoleId);
        } catch (UniformInterfaceException e) {
            assertEquals(Response.Status.CONFLICT.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    public void deleteuserrole() {
        String uid = doAddUser("riffraff", "snyper", "Edmund", "Goffse", "snyper@midget.orj", "12121212");
        String roleId1 = doAddUserRole(uid, "testappId", "0005", "KK", "test");
        String roleId2 = doAddUserRole(uid, "testappId", "0005", "NN", "tjohei");

        assertEquals(2, doGetUserRoles(uid).size());
        assertNotNull(doGetUserRole(uid, roleId1));
        baseResource.path("user/" + uid + "/role/" + roleId1).delete();
        assertEquals(1, doGetUserRoles(uid).size());

        assertNotNull(doGetUserRole(uid, roleId2));
        baseResource.path("user/" + uid + "/role/" + roleId2).delete();
        assertEquals(0, doGetUserRoles(uid).size());
    }


    @Test
    public void modifyuserrole() {
        String uid = doAddUser("riffraff", "snyper", "Edmund", "Goffse", "snyper@midget.orj", "12121212");
        String roleId = doAddUserRole(uid, "testappId", "0005", "KK", "test");

        Map<String, Object> roleBeforeModification = doGetUserRole(uid, roleId);
        assertEquals("testappId", roleBeforeModification.get("applicationId"));
        assertEquals("0005", roleBeforeModification.get("organizationName"));
        assertEquals("KK", roleBeforeModification.get("applicationRoleName"));
        assertEquals("test", roleBeforeModification.get("applicationRoleValue"));

        String modifiedUserRoleJsonRequest = "{\"organizationName\": \"0005\",\n" +
                "        \"uid\": \"" + uid + "\",\n" +
                "        \"roleId\": \"" + roleId + "\",\n" +
                "        \"applicationId\": \"testappId\",\n" +
                "        \"applicationRoleName\": \"KK\",\n" +
                "        \"applicationRoleValue\": \"test modified\"}";

        String s = baseResource.path("user/" + uid + "/role/" + roleId).type("application/json").put(String.class, modifiedUserRoleJsonRequest);

        Map<String, Object> roleAfterModification = doGetUserRole(uid, roleId);
        assertEquals("testappId", roleAfterModification.get("applicationId"));
        assertEquals("0005", roleAfterModification.get("organizationName"));
        assertEquals("KK", roleAfterModification.get("applicationRoleName"));
        assertEquals("test modified", roleAfterModification.get("applicationRoleValue"));
    }


    @Test
    public void userexists() {
        String uid = doAddUser("1231312", "siqula", "Hoytahl", "Goffse", "siqula@midget.orj", "12121212");
        String s = baseResource.path("user/" + uid).get(String.class);
        assertTrue(s.contains("Hoytahl"));
    }


    @Test
    public void usernotexists() {
        doAddUser("1231312", "siqula", "Hoytahl", "Goffse", "siqula@midget.orj", "12121212");
        String uid = "non-existent-uid";
        try {
            String s = baseResource.path("user/" + uid).get(String.class);
            fail("Expected 404 NOT FOUND");
        } catch (UniformInterfaceException e) {
            assertEquals(ClientResponse.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
    }


    @Test
    public void thatAddUserWillRespondWithConflictWhenUsernameAlreadyExists() {
        doAddUser("riffraff", "snyper", "Edmund", "Goffse", "snyper@midget.orj", "12121212");
        try {
            doAddUser("tifftaff", "snyper", "Another", "Wanderer", "wanderer@midget.orj", "34343434");
            fail("Expected 409 CONFLICT");
        } catch (UniformInterfaceException e) {
            assertEquals(ClientResponse.Status.CONFLICT.getStatusCode(), e.getResponse().getStatus());
        }
    }


    @Test
    public void thatAddUserWillRespondWithConflictWhenEmailIsAlreadyInUseByAnotherUser() {
        doAddUser("riffraff", "another", "Edmund", "Goffse", "snyper@midget.orj", "12121212");
        try {
            doAddUser("tifftaff", "iamatestuser", "Another", "Wanderer", "snyper@midget.orj", "34343434");
            fail("Expected 409 CONFLICT");
        } catch (UniformInterfaceException e) {
            assertEquals(ClientResponse.Status.CONFLICT.getStatusCode(), e.getResponse().getStatus());
        }
    }


    @Test
    public void addUser() {
        String uid = doAddUser("riffraff", "snyper", "Edmund", "Gøæøåffse", "snyper@midget.orj", "12121212");

        assertNotNull(uid);

        String s = baseResource.path("user/" + uid).get(String.class);
        assertTrue(s.contains("snyper@midget.orj"));
        assertTrue(s.contains("Edmund"));
        String findresult = baseResource.path("users/find/snyper").get(String.class);
        assertTrue(findresult.contains("snyper@midget.orj"));
        assertTrue(findresult.contains("Edmund"));
    }

    @Test
    public void addUserAllowMissingPersonRef() {
        String uid = doAddUser(null, "tsnyper", "tEdmund", "tGoffse", "tsnyper@midget.orj", "12121212");
        baseResource.path("user/" + uid).get(String.class);
    }

    @Test
    public void addUserAllowMissingFirstName() {
        doAddUser("triffraff", "tsnyper", null, "tGoffse", "tsnyper@midget.orj", "12121212");
    }

    @Test
    public void addUserAllowMissingLastName() {
        doAddUser("triffraff", "tsnyper", "tEdmund", null, "tsnyper@midget.orj", "12121212");
    }

    @Test
    public void thatAddUserDoesNotAllowMissingEmail() {
        try {
            doAddUser("triffraff", "tsnyper", "tEdmund", "tGoffse", null, "12121212");
            fail("Expected 400 BAD_REQUEST");
        } catch (UniformInterfaceException e) {
            assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    public void addUserWithMissingPhoneNumber() {
        String uid = doAddUser("triffraff", "tsnyper", "tEdmund", "tGoffse", "tsnyper@midget.orj", null);
        baseResource.path("user/" + uid).get(String.class);
    }

    /*  //TODO not sure what this test is supposed to verify
    @Test
    public void addUserWithCodesInCellPhoneNumber() {
        // TODO: Apache DS does not allow phone number with non-number letters
        String uid = doAddUser("triffraff", "tsnyper", "tEdmund", "lastname", "tsnyper@midget.orj", "12121-bb-212");
        baseResource.path("user/" + uid).get(String.class);
    }
    */

    @Test
    public void testAddUserWithLettersInPhoneNumberIsNotAllowed() {
        // Apache DS does not allow phone number with non-number letters
        try {
            doAddUser("triffraff", "tsnyper", "tEdmund", "lastname", "tsnyper@midget.orj", "12121-bb-212");
            fail("Expected 400 BAD_REQUEST");
        } catch (UniformInterfaceException e) {
            assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
        }
    }


    @Test
    @Ignore
    public void resetAndChangePassword() {
        String uid = doAddUser("123123123", "sneile", "Effert", "Huffse", "sneile@midget.orj", "21212121");

        baseResource.path("user/sneile/resetpassword").type("application/json").post(ClientResponse.class);

        // TODO somehow replace PasswordSender with MockMail in guice context.

        String token = main.getInjector().getInstance(MockMail.class).getToken(uid);
        assertNotNull(token);

        ClientResponse response = baseResource.path("user/sneile/newpassword/" + token).type(MediaType.APPLICATION_JSON).post(ClientResponse.class, "{\"newpassword\":\"naLLert\"}");
        assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
        String payload = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><auth><username>sneile</username><password>naLLert</password></auth>";
        response = logonResource.path("logon").type("application/xml").post(ClientResponse.class, payload);
        assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
        String identity = response.getEntity(String.class);
        assertTrue(identity.contains("identity"));
        assertTrue(identity.contains("sneile"));
    }

    @Test
    public void thatEmailCanBeUsedAsUsername() {
        String uid = doAddUser("riffraff", "snyper@midget.orj", "Edmund", "Goffse", "somotheremail@midget.orj", "12121212");
        String s = baseResource.path("user/" + uid).get(String.class);
        assertTrue(s.contains("snyper@midget.orj"));
    }

    private String doAddUser(String userjson) {
        WebResource webResource = baseResource.path("user");
        String postResponseJson = webResource.type(MediaType.APPLICATION_JSON).post(String.class, userjson);
        Map<String, Object> createdUser = null;
        try {
            createdUser = new ObjectMapper().readValue(postResponseJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return (String) createdUser.get("uid");
    }

    private String doAddUser(String personRef, String username, String firstName, String lastName, String email, String cellPhone) {
        String userJson = buildUserJson(personRef, username, firstName, lastName, email, cellPhone);
        return doAddUser(userJson);
    }

    private String buildUserJson(String personRef, String username, String firstName, String lastName, String email, String cellPhone) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(" \"username\":\"").append(username).append("\"");
        if (personRef != null) {
            sb.append(",\n").append(" \"personRef\":\"").append(personRef).append("\"");
        }
        if (firstName != null) {
            sb.append(",\n").append(" \"firstName\":\"").append(firstName).append("\"");
        }
        if (lastName != null) {
            sb.append(",\n").append(" \"lastName\":\"").append(lastName).append("\"");
        }
        if (email != null) {
            sb.append(",\n").append(" \"email\":\"").append(email).append("\"");
        }
        if (cellPhone != null) {
            sb.append(",\n").append(" \"cellPhone\":\"").append(cellPhone).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private List<Map<String, Object>> doGetUserRoles(String uid) {
        String postResponseJson = baseResource.path("user/" + uid + "/roles").get(String.class);
        List<Map<String, Object>> roles = null;
        try {
            roles = new ObjectMapper().readValue(postResponseJson, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return roles;
    }

    private Map<String, Object> doGetUserRole(String uid, String roleId) {
        String postResponseJson = baseResource.path("user/" + uid + "/role/" + roleId).get(String.class);
        Map<String, Object> roles = null;
        try {
            roles = new ObjectMapper().readValue(postResponseJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return roles;
    }

    private String doAddUserRole(String uid, String applicationId, String organizationName, String applicationRoleName, String applicationRoleValue) {
        WebResource webResource = baseResource.path("user/" + uid + "/role");
        String postResponseJson = webResource.type("application/json").post(String.class, "{\"organizationName\": \"" + organizationName + "\",\n" +
                "        \"applicationId\": \"" + applicationId + "\",\n" +
                "        \"applicationRoleName\": \"" + applicationRoleName + "\",\n" +
                "        \"applicationRoleValue\": \"" + applicationRoleValue + "\"}");

        Map<String, Object> createdUser = null;
        try {
            createdUser = new ObjectMapper().readValue(postResponseJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return (String) createdUser.get("roleId");
    }

}