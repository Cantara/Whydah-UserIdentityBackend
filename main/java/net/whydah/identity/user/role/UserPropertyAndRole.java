package net.whydah.identity.user.role;

public class UserPropertyAndRole {
    private String roleId;

    private String uid;

    private String applicationId;
    private transient String applicationName;

    private transient String organizationName;

    private String applicationRoleName;
    private String applicationRoleValue;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserPropertyAndRole that = (UserPropertyAndRole) o;

        if (applicationId != null ? !applicationId.equals(that.applicationId) : that.applicationId != null) {
            return false;
        }
        if (applicationName != null ? !applicationName.equals(that.applicationName) : that.applicationName != null) {
            return false;
        }
        if (organizationName != null ? !organizationName.equals(that.organizationName) : that.organizationName != null) {
            return false;
        }
        if (applicationRoleName != null ? !applicationRoleName.equals(that.applicationRoleName) : that.applicationRoleName != null) {
            return false;
        }
        if (applicationRoleValue != null ? !applicationRoleValue.equals(that.applicationRoleValue) : that.applicationRoleValue != null) {
            return false;
        }
        if (uid != null ? !uid.equals(that.uid) : that.uid != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = uid != null ? uid.hashCode() : 0;
        result = 31 * result + (applicationId != null ? applicationId.hashCode() : 0);
        result = 31 * result + (applicationName != null ? applicationName.hashCode() : 0);
        result = 31 * result + (organizationName != null ? organizationName.hashCode() : 0);
        result = 31 * result + (applicationRoleName != null ? applicationRoleName.hashCode() : 0);
        result = 31 * result + (applicationRoleValue != null ? applicationRoleValue.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserPropertyAndRole{" +
                "uid='" + uid + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", applicationName='" + applicationName + '\'' +
                ", organizationName='" + organizationName + '\'' +
                ", roleName='" + applicationRoleName + '\'' +
                ", roleValue='" + applicationRoleValue + '\'' +
                '}';
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

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

    public String getRoleId() {
        return roleId;
    }

    public String getUid() {
        return uid;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getApplicationName() {
        return (applicationName == null ? "" : applicationName);
    }

    public String getOrganizationName() {
        return (organizationName == null ? "" : organizationName);
    }

    public String getApplicationRoleName() {
        return applicationRoleName;
    }

    public String getApplicationRoleValue() {
        return applicationRoleValue;
    }
}
