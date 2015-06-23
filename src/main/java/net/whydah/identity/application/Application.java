package net.whydah.identity.application;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Application.
 *
 * Erik's notes:
 *
 * applications
 *      application, applicationID, name
 *          relations   (organisation)
 *              relation1, id, name
 *                  properties / hashmap (mappet tidligere til roller)
 *              relation2, id, name
 *                  properties / hashmap
 *
 */
public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private String id;
    private String name;
    @JsonIgnore
    private String secret;
    private String description;

    //private List<String> availableRoleNames;    //roleName or roleId here?
    private List<Role> availableRoles;
    private String defaultRoleName;     //roleName or roleId here?

    private List<String> availableOrgNames;
    private String defaultOrgName;


    private Application() {
    }

    public Application(String id, String name) {
        this.id = id;
        this.name = name;
        this.availableRoles = new ArrayList<>();
        this.availableOrgNames = new ArrayList<>();
    }

    public List<String> getAvailableRoleNames() {
        List<String> names = new ArrayList<>(availableRoles.size());
        for (Role role : availableRoles) {
            names.add(role.getName());
        }
        if (names.isEmpty()) {
            return null;
        }
        return names;
    }

    public void addRole(Role role) {
        availableRoles.add(role);
    }


    public void setId(String id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setSecret(String secret) {
        this.secret = secret;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setAvailableRoles(List<Role> availableRoles) {
        this.availableRoles = availableRoles;
    }
    public void setDefaultRoleName(String defaultRoleName) {
        this.defaultRoleName = defaultRoleName;
    }
    public void setAvailableOrgNames(List<String> availableOrgNames) {
        this.availableOrgNames = availableOrgNames;
    }
    public void setDefaultOrgName(String defaultOrgName) {
        this.defaultOrgName = defaultOrgName;
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getSecret() {
        return secret;
    }
    public String getDescription() {
        return description;
    }
    public List<Role> getAvailableRoles() {
        return availableRoles;
    }
    public String getDefaultRoleName() {
        return defaultRoleName;
    }
    public List<String> getAvailableOrgNames() {
        return availableOrgNames;
    }
    public String getDefaultOrgName() {
        return defaultOrgName;
    }

    public String toXML() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
                " <application>\n" +
                "   <applicationid>" + id + "</applicationid>\n" +
                "   <applicationname>" + name + "</applicationname>\n" +
                "   <defaultrolename>" + defaultRoleName + "</defaultrolename>\n" +
                "   <defaultorganizationname>" + defaultOrgName + "</defaultorganizationname>\n" +
                "  " + buildAvailableOrgAsXml() + "\n" +
                "  " + buildAvailableRoleAsXml() + "\n" +
                " </application>\n";
    }

    private String buildAvailableOrgAsXml() {
        if(availableOrgNames == null || availableOrgNames.size() == 0) {
            return "<organizationsnames/>";
        }else {
            StringBuilder availableXml = new StringBuilder("<organizationsnames>\n");
            for (String availableOrgName : availableOrgNames) {
                availableXml.append("<orgName>").append(availableOrgName).append("</orgName>").append("\n");
            }
            availableXml.append("</organizationsnames>");
            return availableXml.toString();
        }
    }

    private String buildAvailableRoleAsXml() {
        if (getAvailableRoleNames() == null || getAvailableRoleNames().size() == 0) {
            return "<rolenames/>";
        } else {
            StringBuilder availableXml = new StringBuilder("<rolenames>\n");
            for (String roleName : getAvailableRoleNames()) {
                availableXml.append("<roleName>").append(roleName).append("</roleName>").append("\n");
            }
            availableXml.append("</rolenames>");
            return availableXml.toString();
        }
    }

    @Override
    public String toString() {
        String availableOrgNamesString = "";
        if (this.availableOrgNames != null) {
            availableOrgNamesString = String.join(",", this.availableOrgNames);
        }

        String roleNamesString = null;
        if (availableRoles != null) {
            StringBuilder strb = new StringBuilder();
            for (Role role : availableRoles) {
                strb.append(role.getName()).append(",");
            }
            roleNamesString = strb.toString();
        }


        return "Application{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", secret='" + secret + '\'' +
                ", description='" + description + '\'' +
                ", availableRoles=" + roleNamesString +
                ", defaultRoleName='" + defaultRoleName + '\'' +
                ", availableOrgNames=" + availableOrgNamesString +
                ", defaultOrgName='" + defaultOrgName + '\'' +
                '}';
    }

    /*
    public void addAvailableOrgName(String availableOrgName) {
        if (availableOrgNames == null) {
            availableOrgNames = new ArrayList();
        }
        if (availableOrgName != null) {
            this.availableOrgNames.add(availableOrgName);
        }
    }
    public void removeAvailableOrgName(String availableOrgName) {
        if (availableOrgNames != null && availableOrgName != null) {
            availableOrgNames.remove(availableOrgName);
        }
    }
    public void addAvailableRoleName(String availableRoleName) {
        if (availableRoleNames == null) {
            availableRoleNames = new ArrayList();
        }
        if (availableRoleName != null) {
            this.availableRoleNames.add(availableRoleName);
        }
    }

    public void removeAvailableRoleName(String availableRoleName) {
        if (availableRoleNames != null && availableRoleName != null) {
            availableRoleNames.remove(availableRoleName);
        }
    }
    */
}
