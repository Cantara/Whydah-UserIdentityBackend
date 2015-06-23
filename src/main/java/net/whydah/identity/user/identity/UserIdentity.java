package net.whydah.identity.user.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.TelephoneNumberSyntaxChecker;
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
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class UserIdentity extends UserIdentityRepresentation implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(UserIdentity.class);
    private static final long serialVersionUID = 1;
    private static final TelephoneNumberSyntaxChecker telephoneNumberSyntaxChecker = new TelephoneNumberSyntaxChecker();

    private String uid;

    public UserIdentity() {
    }

    public UserIdentity(String uid, String username, String firstName, String lastName, String email, String password,
                        String cellPhone, String personRef) {
        this.uid = uid;
        this.username = (username != null ? username : email);
        this.firstName = firstName;
        this.lastName = lastName;
        this.personRef = personRef;
        this.email = email;
        this.cellPhone = cellPhone;
        this.password = password;
    }

    public void validate() throws InvalidUserIdentityFieldException {
        validateSetAndMinimumLength("uid", uid, 2);
        validateSetAndMinimumLength("username", username, 3);

        validateEmail();

        //Printable string (alphabetic, digits, ', (, ), +, ,, -, ., /, :, ?, and space) and "
        //http://pic.dhe.ibm.com/infocenter/zvm/v6r3/index.jsp?topic=%2Fcom.ibm.zvm.v630.kldl0%2Fkldl023.htm
        if (cellPhone != null && !telephoneNumberSyntaxChecker.isValidSyntax(cellPhone)) {
            throw new InvalidUserIdentityFieldException("cellPhone", cellPhone);
        }

        // valid
    }

    private void validateSetAndMinimumLength(String key, String value, int minLength) {
        if (value == null || value.length() < minLength) {
            throw new InvalidUserIdentityFieldException(key, value);
        }
    }


    private void validateEmail() {
        if (email == null || email.length() < 5) {
            throw new InvalidUserIdentityFieldException("email", email);
        }

        InternetAddress internetAddress = new InternetAddress();
        internetAddress.setAddress(email);
        try {
            internetAddress.validate();
        } catch (AddressException e) {
            throw new InvalidUserIdentityFieldException("email", email);
        }
    }


    @Override
    public String toString() {
        return "UserIdentity{" +
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

        UserIdentity that = (UserIdentity) o;

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
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public static UserIdentity fromJson(String userJson) {
        try {
            UserIdentity userIdentity = new UserIdentity();

            JSONObject jsonobj = new JSONObject(userJson);

            String username = (jsonobj.getString("username").length() > 2) ? jsonobj.getString("username") : jsonobj.getString("email");

            String email = jsonobj.getString("email");
            if (email.contains("+")){
                email = replacePlusWithEmpty(email);
            }

            InternetAddress internetAddress = new InternetAddress();
            internetAddress.setAddress(email);
            try {
                internetAddress.validate();
                userIdentity.setEmail(email);
            } catch (AddressException e) {
                //log.error(String.format("E-mail: %s is of wrong format.", email));
                //return Response.status(Response.Status.BAD_REQUEST).build();
                throw new IllegalArgumentException(String.format("E-mail: %s is of wrong format.", email));
            }
            userIdentity.setUsername(username);
            userIdentity.setFirstName(jsonobj.getString("firstName"));
            userIdentity.setLastName(jsonobj.getString("lastName"));

            userIdentity.setCellPhone(jsonobj.getString("cellPhone"));
            userIdentity.setPersonRef(jsonobj.getString("personRef"));
            //userIdentity.setUid(UUID.randomUUID().toString());
            return userIdentity;
        } catch (JSONException e) {
            throw new IllegalArgumentException("Error parsing json", e);
        }
    }

    private static String replacePlusWithEmpty(String email){
        String[] words = email.split("[+]");
        if (words.length == 1) {
            return email;
        }
        email  = "";
        for (String word : words) {
            email += word;
        }
        return email;
    }
    /*
    private static String getValidLDAPPhoneNumber(String text){
        if (text == null) {
            return null;
        }

        text = text.replaceAll(" +", "");
        if (text != null && Pattern.matches("(d\\+)?([+0-9]*)", text) == true && text.length() > 7) {
            return text;
        }

        return null;
    }
    */
}
