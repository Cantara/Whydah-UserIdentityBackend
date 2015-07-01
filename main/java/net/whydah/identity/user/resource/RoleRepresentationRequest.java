package net.whydah.identity.user.resource;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 08/04/14
 */
public class RoleRepresentationRequest {
    private String applicationId;
    private String applicationName;

    private String organizationName;

    private String applicationRoleName;
    private String applicationRoleValue;


    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }
    public void setApplicationRoleName(String applicationRoleName) {
        this.applicationRoleName = applicationRoleName;
    }
    public void setApplicationRoleValue(String applicationRoleValue) {
        this.applicationRoleValue = applicationRoleValue;
    }


    public String getApplicationId() {
        return applicationId;
    }
    public String getApplicationName() {
        return applicationName;
    }
    public String getOrganizationName() {
        return organizationName;
    }
    public String getApplicationRoleName() {
        return applicationRoleName;
    }
    public String getApplicationRoleValue() {
        return applicationRoleValue;
    }
}
