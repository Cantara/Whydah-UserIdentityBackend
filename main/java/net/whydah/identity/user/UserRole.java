package net.whydah.identity.user;

public class UserRole {
    private final String appId;
    private final String orgName;
    private final String roleName;

    public UserRole(String appId, String orgName, String roleName) {
        this.appId = appId;
        this.orgName = orgName;
        this.roleName = roleName;
    }

    public String getAppId() {
        return appId;
    }

    public String getOrgName() {
        return orgName;
    }

    public String getRoleName() {
        return roleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserRole userRole = (UserRole) o;

        if (appId != null ? !appId.equals(userRole.appId) : userRole.appId != null) {
            return false;
        }
        if (orgName != null ? !orgName.equals(userRole.orgName) : userRole.orgName != null) {
            return false;
        }
        if (roleName != null ? !roleName.equals(userRole.roleName) : userRole.roleName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = appId != null ? appId.hashCode() : 0;
        result = 31 * result + (orgName != null ? orgName.hashCode() : 0);
        result = 31 * result + (roleName != null ? roleName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserRole{" +
                "appId='" + appId + '\'' +
                ", orgName='" + orgName + '\'' +
                ", roleName='" + roleName + '\'' +
                '}';
    }
}
