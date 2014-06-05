package net.whydah.identity.application;

import com.google.common.base.Joiner;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    private String defaultRole;
    private String defaultOrgid;
    private List<String> availableOrgIds;

    private Application() {
    }

    public Application(String id, String name) {
        this(id, name, null, null);
    }
    public Application(String id, String name, String defaultRole, String defaultOrgid) {
        this.id = id;
        this.name = name;
        this.defaultRole = defaultRole;
        this.defaultOrgid = defaultOrgid;
    }

    public Application(String id, String name, String defaultRole, String defaultOrgid, List<String> availableOrgIds) {
        this.id = id;
        this.name = name;
        this.defaultRole = defaultRole;
        this.defaultOrgid = defaultOrgid;
        this.availableOrgIds = availableOrgIds;
    }

    /**
     * {
     "id": "id1",
     "name": "test",
     "defaultRole": "default1role",
     "defaultOrgid": "defaultorgid",
     "availableOrgIds": [
     "developer@customer",
     "consultant@customer"
     ]
     }
     * @param applicationJson
     * @return
     */
    public static Application fromJson(String applicationJson) {
            try {
                Application application;

                ObjectMapper mapper = new ObjectMapper();
                application = mapper.readValue(applicationJson, Application.class);
                return application;
            } catch (JsonMappingException e) {
                throw new IllegalArgumentException("Error mapping json for " + applicationJson, e);
            } catch (JsonParseException e) {
                throw new IllegalArgumentException("Error parsing json for " + applicationJson, e);
            } catch (IOException e) {
                throw new IllegalArgumentException("Error reading json for " + applicationJson, e);
            }
    }

    public String toJson() {
        String applicationJson = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            applicationJson =  mapper.writeValueAsString(this);
        } catch (IOException e) {
            log.info("Could not create json from this object {}", toString(), e);
        }
        return applicationJson;
    }

    public String toXML() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
                " <application>\n" +
                "   <applicationid>" + id + "</applicationid>\n" +
                "   <applicationname>" + name + "</applicationname>\n" +
                "   <defaultrole>" + defaultRole + "</defaultrole>\n" +
                "   <defaultorgid>" + defaultOrgid + "</defaultorgid>\n" +
                "  " + buildAvailableOrgIsXml() + "\n" +
                " </application>\n";
    }

    private String buildAvailableOrgIsXml() {
        if(availableOrgIds == null || availableOrgIds.size() == 0) {
            return "<availableOrgIds/>";
        }else {
            StringBuilder availableXml = new StringBuilder("<availableOrgIds>\n");
            for (String availableOrgId : availableOrgIds) {
                availableXml.append("<orgId>").append(availableOrgId).append("</orgId>").append("\n");
            }
            availableXml.append("</availableOrgIds>");
            return availableXml.toString();
        }
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getDefaultRole() {
        return defaultRole;
    }
    public String getDefaultOrgid() {
        return defaultOrgid;
    }

    public List<String> getAvailableOrgIds() {
        return availableOrgIds;
    }

    public void setAvailableOrgIds(List<String> availableOrgIds) {
        if (availableOrgIds != null) {
            this.availableOrgIds = availableOrgIds;
        }
    }

    public void addAvailableOrgId(String availableOrgId) {
        if (availableOrgIds == null) {
            availableOrgIds = new ArrayList();
        }
        if (availableOrgId != null) {
            this.availableOrgIds.add(availableOrgId);
        }
    }

    public void removeAvailableOrgId(String availableOrgId) {
        if (availableOrgIds != null && availableOrgId != null) {
            availableOrgIds.remove(availableOrgId);
        }
    }

    public void setDefaultOrgid(String defaultOrgid) {
        this.defaultOrgid = defaultOrgid;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }

    @Override
    public String toString() {
        String availableIdsString = "";
         if (this.availableOrgIds != null) {
            availableIdsString = Joiner.on(", ").join(this.availableOrgIds);
         }
        return "Application{" +
                "appId='" + id + '\'' +
                ", name='" + name + '\'' +
                ", defaultrole='" + defaultRole + '\'' +
                ", defaultOrgid='" + defaultOrgid + '\'' +
                ", availableOrgIds = '" + availableIdsString  + '\'' +
                '}';
    }

    public void setId(String id) {
        this.id = id;
    }
}
