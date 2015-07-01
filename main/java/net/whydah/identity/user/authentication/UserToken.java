package net.whydah.identity.user.authentication;

import net.whydah.identity.user.UserRole;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class UserToken {
    private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    private final String name;
    private final List<UserRole> userRoles;

    public UserToken(String name, List<UserRole> userRoles) {
        this.name = name;
        this.userRoles = userRoles;
    }

    public UserToken(String userToken) {
        Document doc = parseXML(userToken);
        name = extractUsername(doc);
        userRoles = extractUserRoles(doc);
    }

    private String extractUsername(Document doc) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            String fornavn = (String) xPath.evaluate("/usertoken/firstname", doc, XPathConstants.STRING);
            String etternavn = (String) xPath.evaluate("/usertoken/lastname", doc, XPathConstants.STRING);
            return fornavn + ' ' + etternavn;
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "UserToken{" +
                "name='" + name + '\'' +
                ", userRoles=" + userRoles +
                '}';
    }

    public boolean hasRole(String rolename) {
        for (UserRole userRole : userRoles) {
            if (rolename.equals(userRole.getRoleName())) {
                return true;
            }
        }
        return false;
    }

    public List<UserRole> getUserRoles() {
        return userRoles;
    }

    public String getName() {
        return name;
    }

    private Document parseXML(String userToken) {
        try {
            DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            return documentBuilder.parse(new InputSource(new StringReader(userToken)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<UserRole> extractUserRoles(Document doc) {
        List<UserRole> userRoleList = new ArrayList<>();
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            NodeList apps = (NodeList) xPath.evaluate("//application", doc, XPathConstants.NODESET);
            for (int appNo = 0; appNo < apps.getLength(); appNo++) {
                Node appnode = apps.item(appNo);
                String appid = appnode.getAttributes().getNamedItem("ID").getNodeValue();
                String orgName = (String) xPath.evaluate(".//organizationName", appnode, XPathConstants.STRING);
                NodeList roles = (NodeList) xPath.evaluate("./role", appnode, XPathConstants.NODESET);
                for (int roleNo = 0; roleNo < roles.getLength(); roleNo++) {
                    Node rolenode = roles.item(roleNo);
                    String rolename = rolenode.getAttributes().getNamedItem("name").getNodeValue();
                    userRoleList.add(new UserRole(appid, orgName, rolename));
                }
            }
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        return userRoleList;
    }

}
