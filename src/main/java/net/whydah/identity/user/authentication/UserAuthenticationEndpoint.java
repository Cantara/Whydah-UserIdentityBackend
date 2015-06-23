package net.whydah.identity.user.authentication;

import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.user.UserAggregate;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.resource.UserAdminHelper;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.util.List;

/**
 * Service for authorization of users and finding UserAggregate with corresponding applications, organizations and roles.
 * This not a RESTful endpoint. This is a http RPC endpoint.
 */
@Component
@Path("/{applicationTokenId}/authenticate/user")
public class UserAuthenticationEndpoint {
    private static final Logger log = LoggerFactory.getLogger(UserAuthenticationEndpoint.class);

    private final UserPropertyAndRoleRepository roleRepository;
    private final UserAdminHelper userAdminHelper;
    private final UserIdentityService userIdentityService;
    //private final String hostname;

    @Autowired
    private AuditLogDao auditLogDao;

    @Autowired
    public UserAuthenticationEndpoint(UserPropertyAndRoleRepository roleRepository, UserAdminHelper userAdminHelper,
                                      UserIdentityService userIdentityService) {
        this.roleRepository = roleRepository;
        this.userAdminHelper = userAdminHelper;
        this.userIdentityService = userIdentityService;
        //this.hostname = getLocalhostName();
    }

    /**
     * Authentication using XML. XML must contain an element with name username, and an element with name password.
     *
     * @param input XML input stream.
     * @return XML-encoded identity and role information, or a LogonFailed element if authentication failed.
     */
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response authenticateUser(InputStream input) {
        log.trace("authenticateUser from XML InputStream");
        UserAuthenticationCredentialDTO dto;
        try {
            dto = UserAuthenticationCredentialDTO.fromXml(input);
            log.trace("UserAuthenticationCredentialDTO" + dto + " XML:" + input.toString());
        } catch (ParserConfigurationException e) {
            log.error("authenticateUser failed due to internal server error. Returning {}", Response.Status.INTERNAL_SERVER_ERROR, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, check error logs</error>").build();
        } catch (IOException | SAXException | XPathExpressionException e) {
            log.info("authenticateUser failed due to invald client request. Returning {}", Response.Status.BAD_REQUEST, e);
            return Response.status(Response.Status.BAD_REQUEST).entity("<error>Bad request, check client request</error>").build();
        }

        String passwordCredential = dto.getPasswordCredential();
        if (passwordCredential == null) {
            log.trace("Neither password nor facebookId is set. Returning " + Response.Status.FORBIDDEN);
            Viewable entity = new Viewable("/logonFailed.xml.ftl");
            return Response.status(Response.Status.FORBIDDEN).entity(entity).build();
        }
        return authenticateUser(dto.getUsername(), passwordCredential);
    }

    private Response authenticateUser(String username, String password) {
        UserIdentity id = userIdentityService.authenticate(username, password);
        if (id == null) {
            log.trace("Authentication failed for user with username={}. Returning {}", username, Response.Status.FORBIDDEN.toString());
            Viewable entity = new Viewable("/logonFailed.xml.ftl");
            return Response.status(Response.Status.FORBIDDEN).entity(entity).build();
        }

        List<UserPropertyAndRole> roles = roleRepository.getUserPropertyAndRoles(id.getUid());
        UserAggregate userAggregate = new UserAggregate(id, roles);
        log.info("Authentication ok for user with username={}", username);

        String userXml = userAggregate.toXML();
        log.debug("User authentication ok. XML: {}", userXml);

        //Viewable entity = new Viewable("/user.xml.ftl", userAggregate);
        Response response = Response.ok(userXml).build();
        return response;
    }


    //TODO Move to UserAdminService (the separate application)
    @POST
    @Path("createandlogon")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response createAndAuthenticateUser(InputStream input) {
        log.debug("createAndAuthenticateUser");

        Document fbUserDoc = parse(input);
        if (fbUserDoc == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, could not parse input.</error>").build();
        }

        UserIdentity userIdentity = UserAdminHelper.createWhydahUserIdentity(fbUserDoc);

        if (userIdentity == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, could not parse input.</error>").build();
        }


        String facebookUserAsString = getFacebookDataAsXmlString(fbUserDoc);
        //String facebookUserAsString = getFacebookDataAsXmlString(input);
        return createAndAuthenticateUser(userIdentity, facebookUserAsString, true);
    }

    //TODO Move to UserAdminService (the separate application)
    static String getFacebookDataAsXmlString(Document fbUserDoc) {
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            StringWriter buffer = new StringWriter();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.transform(new DOMSource(fbUserDoc), new StreamResult(buffer));
            String original = buffer.toString();

            // Wrap everything in CDATA
            return "<![CDATA[" + original + "]]>";
        } catch (Exception e) {
            log.error("Could not convert Document to string.", e);
            return null;
        }
    }

    static String getFacebookDataAsString(InputStream is) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            log.warn("Error parsing inputStream as string.", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.info("Could not close reader.");
                }
            }
        }
        String facebookUserAsString = sb.toString();

        log.debug("facebookUserAsString=" + facebookUserAsString);
        return facebookUserAsString;
    }

    private Document parse(InputStream input) {
        Document fbUserDoc;
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            fbUserDoc = builder.parse(input);
        } catch (Exception e) {
            log.error("Error when creating UserIdentity from incoming xml stream.", e);
            return null;
        }
        return fbUserDoc;
    }


    //TODO Move to UserAdminService (the separate application)
    // FIXME fail if called and a) user exist from earlier  b) the user has reset password
    Response createAndAuthenticateUser(UserIdentity userIdentity, String roleValue, boolean reuse) {
        try {
            log.trace("createAndAuthenticateUser - userIdentity:{} roleValue:{} reuse:{}", userIdentity, roleValue, reuse);
            Response response = userAdminHelper.addUser(userIdentity);
            if (!reuse && response.getStatus() != Response.Status.OK.getStatusCode()) {
                return response;
            }

            if (userIdentity != null) {
                log.trace("createAndAuthenticateUser - Checking for UID mismatch. received: {} found {}", userIdentity.getUid(), userIdentityService.getUserIdentity(userIdentity.getUsername()).getUid());
                if (!userIdentity.getUid().equalsIgnoreCase(userIdentityService.getUserIdentity(userIdentity.getUsername()).getUid())) {
                    log.error("createAndAuthenticateUser - Got user with dogus UID, resetting to found UID");
                    userIdentity.setUid(userIdentityService.getUserIdentity(userIdentity.getUsername()).getUid());
                }
                userAdminHelper.addDefaultRoles(userIdentity, roleValue);
                if (reuse) {
                    log.info("createAndAuthenticateUser - update useridentity from 3party token ");
                    userIdentityService.updateUserIdentity(userIdentity.getUsername(), userIdentity);
                    log.info("createAndAuthenticateUser - updating password for  useridentity from 3party token, userName: {} uid: {} ", userIdentity.getUsername(), userIdentity.getUid());
                    userIdentityService.changePassword(userIdentity.getUsername(), userIdentity.getUid(), userIdentity.getPassword());

                }
            }

            log.trace("createAndAuthenticateUser - authenticateUser:{}", userIdentity.getUsername());
            return authenticateUser(userIdentity.getUsername(), userIdentity.getPassword());

        } catch (Exception e) {
            if (reuse) {
                log.info("createAndAuthenticateUser - updating password for  useridentity from 3party token, userName: {} uid: {} ", userIdentity.getUsername(), userIdentity.getUid());
                userIdentityService.changePassword(userIdentity.getUsername(), userIdentity.getUid(), userIdentity.getPassword());
                log.info("createAndAuthenticateUser - update useridentity from 3party token ");
                userIdentityService.updateUserIdentity(userIdentity.getUsername(), userIdentity);
                if (userIdentity != null) {
                    userAdminHelper.addDefaultRoles(userIdentity, roleValue);
                }

                log.trace("createAndAuthenticateUser authenticateUser:{}", userIdentity.getUsername());
                return authenticateUser(userIdentity.getUsername(), userIdentity.getPassword());
            }
            log.error("createAndAuthenticateUser failed " + userIdentity.toString(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, check error logs</error>").build();
        }
    }
}
