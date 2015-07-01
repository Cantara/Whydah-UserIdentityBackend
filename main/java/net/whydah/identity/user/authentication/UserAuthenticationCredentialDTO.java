package net.whydah.identity.user.authentication;

import net.whydah.identity.user.resource.UserAdminHelper;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 30.03.14
 */
public class UserAuthenticationCredentialDTO {
    private String username;
    private String password;
    private String facebookId;
    private String netIQAccessToken;

    private UserAuthenticationCredentialDTO(String username, String password, String facebookId, String netIQAccessToken) {
        this.username = username;
        this.password = password;
        this.facebookId = facebookId;
        this.netIQAccessToken = netIQAccessToken;
    }

    static UserAuthenticationCredentialDTO fromXml(InputStream input) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document dDoc = builder.parse(input);
        XPath xPath = XPathFactory.newInstance().newXPath();
        String username = (String) xPath.evaluate("//username", dDoc, XPathConstants.STRING);
        String password = (String) xPath.evaluate("//password", dDoc, XPathConstants.STRING);
        String facebookId = (String) xPath.evaluate("//fbId", dDoc, XPathConstants.STRING);
        String netIQAccessToken = (String) xPath.evaluate("//netiqId", dDoc, XPathConstants.STRING);
        UserAuthenticationCredentialDTO dto = new UserAuthenticationCredentialDTO(username, password, facebookId, netIQAccessToken);
        return dto;
    }

    String getPasswordCredential() {
        String passwordCredentials = null;
        if (password != null && !password.equals("")) {
            passwordCredentials = password;
        } else if (facebookId != null && !facebookId.equals("")) {
            passwordCredentials = UserAdminHelper.calculateSyntheticPassword(facebookId);
        } else if (netIQAccessToken != null && !netIQAccessToken.equals("")) {
            passwordCredentials = UserAdminHelper.calculateSyntheticPassword(netIQAccessToken);
        }
        return passwordCredentials;
    }

    String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "UserAuthenticationCredentialDTO{" +
                "username='" + username + '\'' +
                ", facebookId='" + facebookId + '\'' +
                ", netIQAccessToken='" + netIQAccessToken + '\'' +
                '}';
    }
}
