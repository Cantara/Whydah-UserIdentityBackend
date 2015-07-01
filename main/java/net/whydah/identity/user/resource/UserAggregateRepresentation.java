package net.whydah.identity.user.resource;

import net.whydah.identity.user.UserAggregate;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.role.UserPropertyAndRole;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 12/04/14
 */
public class UserAggregateRepresentation {
    private String uid;
    private String username;
    private String firstName;
    private String lastName;
    private String personRef;
    private String email;
    private String cellPhone;
    private String password;    //TODO include this in response?

    private List<RoleRepresentation> roles;

    private UserAggregateRepresentation() {
    }

    public static UserAggregateRepresentation fromUserAggregate(UserAggregate userAggregate) {
        UserAggregateRepresentation dto = new UserAggregateRepresentation();

        UserIdentity id = userAggregate.getIdentity();
        dto.setUid(id.getUid());
        dto.setUsername(id.getUsername());
        dto.setFirstName(id.getFirstName());
        dto.setLastName(id.getLastName());
        dto.setPersonRef(id.getPersonRef());
        dto.setEmail(id.getEmail());
        dto.setCellPhone(id.getCellPhone());
        dto.setPassword(id.getPassword());

        List<UserPropertyAndRole> userPropertyAndRoles = userAggregate.getRoles();
        List<RoleRepresentation> roleRepresentations = new ArrayList<>(userPropertyAndRoles.size());
        for (UserPropertyAndRole role : userPropertyAndRoles) {
            roleRepresentations.add(RoleRepresentation.fromUserPropertyAndRole(role));
        }
        dto.setRoles(roleRepresentations);
        return dto;
    }


    public void setUid(String uid) {
        this.uid = uid;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public void setPersonRef(String personRef) {
        this.personRef = personRef;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setCellPhone(String cellPhone) {
        this.cellPhone = cellPhone;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setRoles(List<RoleRepresentation> roles) {
        this.roles = roles;
    }

    public String getUid() {
        return uid;
    }
    public String getUsername() {
        return username;
    }
    public String getFirstName() {
        return firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public String getPersonRef() {
        return personRef;
    }
    public String getEmail() {
        return email;
    }
    public String getCellPhone() {
        return cellPhone;
    }
    public String getPassword() {
        return password;
    }
    public List<RoleRepresentation> getRoles() {
        return roles;
    }
}
