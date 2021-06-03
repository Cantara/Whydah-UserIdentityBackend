package net.whydah.identity.user.identity;

import at.favre.lib.bytes.Bytes;
import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.IllegalBCryptFormatException;
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
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

/**
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RDBMSUserIdentity extends UserIdentity implements Serializable {
    public static final String UID = "uid";
    private static final Logger logger = LoggerFactory.getLogger(RDBMSUserIdentity.class);
    private static final long serialVersionUID = 1;

    public RDBMSUserIdentity() {
    }

    public RDBMSUserIdentity(String uid, String username, String firstName, String lastName, String email, String password,
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

    public RDBMSUserIdentity(UserIdentity userIdentity, String password) {
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
    protected transient String passwordBCrypt;

    public String getPassword() {
        return password != null ? password.getInput() : null;
    }

    public String getPasswordBCrypt() {
        return passwordBCrypt;
    }

    public String getCellPhone() {
        return this.cellPhone != null ? this.cellPhone.getInput() : null;
    }

    public void setPassword(String password) {
        if (password == null) {
            this.password = null;
            this.passwordBCrypt = null;
            return;
        }
        byte[] passwordBytes = Bytes.from(password, StandardCharsets.UTF_8).array();
        try {
            BCrypt.HashData bcryptHashData = BCrypt.Version.VERSION_2A.parser.parse(passwordBytes);
            this.passwordBCrypt = password;
        } catch (IllegalBCryptFormatException e) {
            this.password = new Password(password);
        }
    }

    public void setPasswordBCrypt(String passwordBCrypt) {
        if (passwordBCrypt == null) {
            this.passwordBCrypt = null;
            return;
        }
        byte[] passwordBytes = Bytes.from(passwordBCrypt, StandardCharsets.UTF_8).array();
        try {
            BCrypt.HashData bcryptHashData = BCrypt.Version.VERSION_2A.parser.parse(passwordBytes); // verify that bcrypt-string is well-formed
            this.passwordBCrypt = passwordBCrypt;
        } catch (IllegalBCryptFormatException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public String toString() {
        return new StringJoiner(", ", RDBMSUserIdentity.class.getSimpleName() + "[", "]")
                .add("uid=" + uid)
                .add("username=" + username)
                .add("firstName=" + firstName)
                .add("lastName=" + lastName)
                .add("personRef=" + personRef)
                .add("email=" + email)
                .add("cellPhone=" + cellPhone)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RDBMSUserIdentity that = (RDBMSUserIdentity) o;

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

    public static RDBMSUserIdentity fromJson(String userJson) {
        try {
            RDBMSUserIdentity userIdentity = new RDBMSUserIdentity();

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
