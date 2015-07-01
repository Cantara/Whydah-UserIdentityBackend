package net.whydah.identity.user.resource;

import net.whydah.identity.user.role.UserPropertyAndRole;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 12/04/14
 */
public class RoleRepresentation extends RoleRepresentationRequest {
    private String id;

    public static RoleRepresentation fromUserPropertyAndRole(UserPropertyAndRole role) {
        RoleRepresentation representation = new RoleRepresentation();
        representation.setId(role.getRoleId());
        representation.setApplicationId(role.getApplicationId());
        representation.setApplicationName(role.getApplicationName());
        representation.setOrganizationName(role.getOrganizationName());
        representation.setApplicationRoleName(role.getApplicationRoleName());
        representation.setApplicationRoleValue(role.getApplicationRoleValue());
        return representation;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
