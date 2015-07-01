package net.whydah.identity.user.resource;

import net.whydah.identity.user.identity.UserIdentityRepresentation;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:erik.drolshammer@altran.com">Erik Drolshammer</a>
 * @since 10/11/12
 */
public class UserAdminHelperTest {

    @Test
    public void testCreateWhydahUserIdentity() throws Exception {
        String fbId = "facebookId1";
        String firstName = "Erik";
        String lastName = "Drolshammer";
        String fbuserXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
                "<user>\n" +
                "    <params>\n" +
                "        <userId>" + fbId + "</userId>\n" +
                "        <firstName>" + firstName + "</firstName>\n" +
                "        <lastName>" + lastName + "</lastName>\n" +
                "    </params> \n" +
                "</user>\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(fbuserXml.getBytes());

        Document fbUserDoc;
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        fbUserDoc = builder.parse(inputStream);

        UserIdentityRepresentation userIdentity = UserAdminHelper.createWhydahUserIdentity(fbUserDoc);

        assertEquals(firstName, userIdentity.getFirstName());
        assertEquals(lastName, userIdentity.getLastName());
    }

}
