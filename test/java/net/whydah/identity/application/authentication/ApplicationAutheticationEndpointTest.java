package net.whydah.identity.application.authentication;

import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

/**
 * @author Stig Lau
 */
public class ApplicationAutheticationEndpointTest {
    @Ignore
    @Test
    public void simplefunctionaltestofAppAuthentication(){
        Response result = new ApplicationAuthenticationEndpoint().authenticateApplication("some random input");
        String responseAsString = result.getEntity().toString();

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n" +
                "  <application>\n" +
                "   <applicationid>9999</applicationid>\n" +
                "   <applicationname>applicationNameMock</applicationname>\n" +
                "   <defaultrolename>null</defaultrolename>\n" +
                "   <defaultorganizationname>null</defaultorganizationname>\n" +
                "  <organizationsnames/>\n" +
                "  <rolenames/>\n" +
                " </application>\n", responseAsString);
    }


}
