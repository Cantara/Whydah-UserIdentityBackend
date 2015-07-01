package net.whydah.identity.user.identity;

/**
 * @author totto
 */
public class UserIdentityRepresentation {
    protected String username;
    protected String firstName;
    protected String lastName;
    protected String personRef;
    protected String email;
    protected String cellPhone;
    protected transient String password;

    public String getPersonName() {
        return firstName + ' ' + lastName;
    }
    public String getPersonRef() {
        return personRef;
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
    public String getEmail() {
        return email;
    }
    public String getCellPhone() {
        return cellPhone;
    }
    public String getPassword() {
        return password;
    }


    public void setPersonRef(String personRef) {
        this.personRef = personRef;
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
    public void setEmail(String email) {
        this.email = email;
    }
    public void setCellPhone(String cellPhone) {
        if (cellPhone != null && cellPhone.isEmpty()) {
            this.cellPhone = null;
            return;
        }
        this.cellPhone = cellPhone;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
