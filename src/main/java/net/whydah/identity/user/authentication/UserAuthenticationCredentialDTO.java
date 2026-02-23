package net.whydah.identity.user.authentication;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import net.whydah.sso.user.helpers.Base64Helper;

import javax.xml.XMLConstants;
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
    private String aadId;

    private UserAuthenticationCredentialDTO(String username, String password, String aadId, String facebookId, String netIQAccessToken) {
        this.username = username;
        this.password = password;
        this.facebookId = facebookId;
        this.netIQAccessToken = netIQAccessToken;
        this.aadId = aadId;
    }

    static UserAuthenticationCredentialDTO fromXml(InputStream input) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setExpandEntityReferences(false);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document dDoc = builder.parse(input);
        XPath xPath = XPathFactory.newInstance().newXPath();
        String username = (String) xPath.evaluate("//username", dDoc, XPathConstants.STRING);
        String password = (String) xPath.evaluate("//password", dDoc, XPathConstants.STRING);
        String aadId = (String) xPath.evaluate("//aadId", dDoc, XPathConstants.STRING);
        String facebookId = (String) xPath.evaluate("//fbId", dDoc, XPathConstants.STRING);
        String netIQAccessToken = (String) xPath.evaluate("//netiqId", dDoc, XPathConstants.STRING);
        
        if(Base64Helper.isBase64(password)) {
        	password = Base64Helper.convertStringFromBase64(password);
        }
        
        UserAuthenticationCredentialDTO dto = new UserAuthenticationCredentialDTO(username, password, aadId, facebookId, netIQAccessToken);
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
        } else if (aadId !=null && aadId.equals("")) {
        	passwordCredentials = UserAdminHelper.calculateSyntheticPassword(aadId);
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
