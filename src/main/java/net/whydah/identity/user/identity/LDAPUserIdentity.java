package net.whydah.identity.user.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.whydah.sso.ddd.model.customer.FirstName;
import net.whydah.sso.ddd.model.customer.LastName;
import net.whydah.sso.ddd.model.user.Email;
import net.whydah.sso.ddd.model.user.Password;
import net.whydah.sso.ddd.model.user.PersonRef;
import net.whydah.sso.ddd.model.user.UID;
import net.whydah.sso.ddd.model.user.UserName;
import net.whydah.sso.user.types.UserIdentity;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.Serializable;

/**
 * A class representing the identity of a User - backed by LDAP scheme.
 * See getLdapAttributes in LDAPHelper for mapping to LDAP attributes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LDAPUserIdentity extends UserIdentity implements Serializable {
    public static final String UID = "uid";
    private static final Logger logger = LoggerFactory.getLogger(LDAPUserIdentity.class);
    private static final long serialVersionUID = 1;

    public LDAPUserIdentity() {
    }

    public LDAPUserIdentity(String uid, String username, String firstName, String lastName, String email, String password,
                            String cellPhone, String personRef) {
        this.uid = new UID(uid);
        this.username = new UserName(username != null ? username : email);
        this.firstName = new FirstName(firstName);
        this.lastName = new LastName(lastName);
        this.personRef = new PersonRef(personRef);
        this.email = new Email(email);
        setCellPhone(cellPhone);
        setPassword(password);
    }

    public LDAPUserIdentity(UserIdentity userIdentity, String password) {
        this.uid = new UID(userIdentity.getUid());
        this.username = new UserName((userIdentity.getUsername() != null ? userIdentity.getUsername() : userIdentity.getEmail()));
        this.firstName = new FirstName(userIdentity.getFirstName());
        this.lastName = new LastName(userIdentity.getLastName());
        this.personRef = new PersonRef(userIdentity.getPersonRef());
        this.email = new Email(userIdentity.getEmail());
        setCellPhone(userIdentity.getCellPhone());
        setPassword(password);
    }

    protected transient Password password;
    protected transient String hashedPassword;

    public String getPassword() {
        return password != null ? password.getInput() : hashedPassword;
    }

    public String getCellPhone() {
        return this.cellPhone != null ? this.cellPhone.getInput() : null;
    }

    public void setPassword(String password) {
        if (password == null) {
            this.password = null;
            this.hashedPassword = null;
            return;
        }
        if (password.startsWith("{SSHA}")) {
            this.hashedPassword = password;
        } else {
            this.password = new Password(password);
        }
    }

    @Override
    public String toString() {
        return "LDAPUserIdentity{" +
                "uid='" + uid + '\'' +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", personRef='" + personRef + '\'' +
                ", email='" + email + '\'' +
                ", cellPhone='" + cellPhone + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LDAPUserIdentity that = (LDAPUserIdentity) o;

        if (uid != null ? !uid.equals(that.uid) : that.uid != null) {
            return false;
        }
        if (username != null ? !username.equals(that.username) : that.username != null) {
            return false;
        }
        if (cellPhone != null ? !cellPhone.equals(that.cellPhone) : that.cellPhone != null) {
            return false;
        }
        if (email != null ? !email.equals(that.email) : that.email != null) {
            return false;
        }
        if (firstName != null ? !firstName.equals(that.firstName) : that.firstName != null) {
            return false;
        }
        if (lastName != null ? !lastName.equals(that.lastName) : that.lastName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = uid != null ? uid.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (cellPhone != null ? cellPhone.hashCode() : 0);
        return result;
    }

    public String getUid() {
        return uid != null ? uid.getId() : null;
    }

    public void setUid(String uid) {
        this.uid = new UID(uid);
    }

    public static LDAPUserIdentity fromJson(String userJson) {
        try {
            LDAPUserIdentity userIdentity = new LDAPUserIdentity();

            JSONObject jsonobj = new JSONObject(userJson);

            String username = (jsonobj.getString("username").length() > 2) ? jsonobj.getString("username") : jsonobj.getString("email");

            String email = jsonobj.getString("email");
            if (email.contains("+")) {
                email = replacePlusWithEmpty(email);
            }

            InternetAddress internetAddress = new InternetAddress();
            internetAddress.setAddress(email);
            try {
                internetAddress.validate();
                userIdentity.setEmail(email);
            } catch (AddressException e) {
                throw new IllegalArgumentException(String.format("E-mail: %s is of wrong format.", email));
            }
            userIdentity.setUsername(username);
            userIdentity.setFirstName(jsonobj.getString("firstName"));
            userIdentity.setLastName(jsonobj.getString("lastName"));

            userIdentity.setCellPhone(jsonobj.getString("cellPhone"));
            userIdentity.setPersonRef(jsonobj.getString("personRef"));
            return userIdentity;
        } catch (JSONException e) {
            throw new IllegalArgumentException("Error parsing json", e);
        }
    }

    private static String replacePlusWithEmpty(String email) {
        String[] words = email.split("[+]");
        if (words.length == 1) {
            return email;
        }
        email = "";
        for (String word : words) {
            email += word;
        }
        return email;
    }
}
