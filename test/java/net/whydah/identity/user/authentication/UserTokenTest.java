package net.whydah.identity.user.authentication;

import net.whydah.identity.user.UserRole;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class UserTokenTest {
    @Test
    public void hasRole() {
        UserToken token = new UserToken(usertoken);
        assertTrue(token.hasRole("WhydahUserAdmin"));
        assertFalse(token.hasRole("nesevis"));
    }
    @Test
    public void getRoles() {
        UserToken token = new UserToken(usertoken);

        UserRole expectedRole1 = new UserRole("1", "WHYDAH", "WhydahUserAdmin");
        UserRole expectedRole2 = new UserRole("1", "WHYDAH", "Tester");
        UserRole expectedRole3 = new UserRole("005", "NBBL", "WhydahUserAdmin");
        List<UserRole> actualRoles = token.getUserRoles();
        assertNotNull(actualRoles);
        assertEquals(3, actualRoles.size());
        assertTrue(actualRoles.contains(expectedRole1));
        assertTrue(actualRoles.contains(expectedRole2));
        assertTrue(actualRoles.contains(expectedRole3));
    }

    private final static String usertoken = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<usertoken xmlns:ns2=\"http://www.w3.org/1999/xhtml\" id=\"b035df2e-e766-4077-a514-2c370cc78714\">\n" +
            "    <securitylevel>1</securitylevel>\n" +
            "    <personid></personid>\n" +
            "    <fornavn>Bruker</fornavn>\n" +
            "    <etternavn>Admin</etternavn>\n" +
            "    <timestamp>1299848579653</timestamp>\n" +
            "    <lifespan>200000</lifespan>\n" +
            "    <issuer>http://10.10.3.88:9998/user/9056ac3f744957ae6a86daffb5aa98d3/usertoken</issuer>\n" +
            "    <application ID=\"1\">\n" +
            "        <applicationName>WhydahUserAdmin</applicationName>\n" +
            "            <organizationName>WHYDAH</organizationName>\n" +
            "            <role name=\"WhydahUserAdmin\" value=\"\"/>\n" +
            "            <role name=\"Tester\" value=\"\"/>\n" +

            "    </application>\n" +
            "    <application ID=\"005\">\n" +
            "        <applicationName>HMS</applicationName>\n" +
            "            <organizationName>NBBL</organizationName>\n" +
            "            <role name=\"WhydahUserAdmin\" value=\"\"/>\n" +
            "    </application>\n" +
            "\n" +
            "    <ns2:link type=\"application/xml\" href=\"/b035df2e-e766-4077-a514-2c370cc78714\" rel=\"self\"/>\n" +
            "    <hash type=\"MD5\">6660ae2fcaa0b8311661fa9e3234eb7a</hash>\n" +
            "</usertoken>";

    @Test
    public void getRoles2() {
        UserToken token = new UserToken(usertoken2);
        UserRole expectedRole1 = new UserRole("1", "", "WhydahUserAdmin");
        List<UserRole> actualRoles = token.getUserRoles();
        assertNotNull(actualRoles);
        assertEquals(1, actualRoles.size());
        assertTrue(actualRoles.contains(expectedRole1));
    }

    private final static String usertoken2 = "<usertoken xmlns:ns2=\"http://www.w3.org/1999/xhtml\" id=\"12b84a5a-595b-49df-bb20-26a8a974d7b9\">\n" +
            "    <uid>useradmin</uid>\n" +
            "    <timestamp>1403606943867</timestamp>\n" +
            "    <lifespan>3600000</lifespan>\n" +
            "    <issuer>http://localhost:9998/tokenservice/user/e0287c65a5c9300c476b34edd0446778/get_usertoken_by_usertokenid</issuer>\n" +
            "    <securitylevel>1</securitylevel>\n" +
            "    <username>admin</username>\n" +
            "    <firstname>User</firstname>\n" +
            "    <lastname>Admin</lastname>\n" +
            "    <email>useradmin@altran.com</email>\n" +
            "    <personRef>0</personRef>\n" +
            "    <application ID=\"1\">\n" +
            "        <applicationName>UserAdmin</applicationName>\n" +
            "            <organizationName></organizationName>\n" +
            "            <role name=\"WhydahUserAdmin\" value=\"99\"/>\n" +
            "    </application>\n" +
            "\n" +
            "    <ns2:link type=\"application/xml\" href=\"/\" rel=\"self\"/>\n" +
            "    <hash type=\"MD5\">9fc1509fbfc2d62e0b40949e4245524a</hash>\n" +
            "</usertoken>";
}
