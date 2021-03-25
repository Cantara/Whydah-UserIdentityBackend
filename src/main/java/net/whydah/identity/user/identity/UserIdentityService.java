package net.whydah.identity.user.identity;

import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.health.HealthResource;
import net.whydah.identity.user.ChangePasswordToken;
import net.whydah.identity.user.search.LuceneUserIndexer;
import net.whydah.identity.user.search.LuceneUserSearch;
import net.whydah.identity.util.PasswordGenerator;
import net.whydah.sso.user.types.UserIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.naming.NamingException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 29.03.14
 */
@Service
public class UserIdentityService {
    private static final Logger log = LoggerFactory.getLogger(UserIdentityService.class);

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");
    private static final String SALT_ENCODING = "UTF-8";

    //@Autowired @Named("internal") private LdapAuthenticatorImpl internalLdapAuthenticator;
    private final LdapAuthenticator primaryLdapAuthenticator;
    private final LdapUserIdentityDao ldapUserIdentityDao;
    private final AuditLogDao auditLogDao;

    private final PasswordGenerator passwordGenerator;

    private final LuceneUserIndexer luceneIndexer;
    private final LuceneUserSearch searcher;
    private static String temporary_pwd=null;


    //@Named("primaryLdap")
    @Autowired
    public UserIdentityService(LdapAuthenticator primaryLdapAuthenticator, LdapUserIdentityDao ldapUserIdentityDao,
                               AuditLogDao auditLogDao, PasswordGenerator passwordGenerator,
                               LuceneUserIndexer luceneIndexer, LuceneUserSearch searcher) {
        this.primaryLdapAuthenticator = primaryLdapAuthenticator;
        this.ldapUserIdentityDao = ldapUserIdentityDao;
        this.auditLogDao = auditLogDao;
        this.passwordGenerator = passwordGenerator;
        this.luceneIndexer = luceneIndexer;
        this.searcher = searcher;
       
    }

    public LDAPUserIdentity authenticate(final String username, final String password) {
        return primaryLdapAuthenticator.authenticate(username, password);
    }


    public String setTempPassword(String username, String uid) {
    	if(temporary_pwd==null){
    		temporary_pwd = passwordGenerator.generate();
    	}
        String newPassword = temporary_pwd;
        String salt = passwordGenerator.generate();
        //HUY: disable saving a new password
        ldapUserIdentityDao.setTempPassword(username, null, salt);
        //ldapUserIdentityDao.setTempPassword(username, newPassword, salt);
        audit(uid,ActionPerformed.MODIFIED, "resetpassword", uid);

        byte[] saltAsBytes;
        try {
            saltAsBytes = salt.getBytes(SALT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        ChangePasswordToken changePasswordToken = new ChangePasswordToken(username, newPassword);
        return changePasswordToken.generateTokenString(saltAsBytes);
    }

    /**
     * Authenticate using token generated when resetting the password
     * @param username  username to authenticate
     * @param changePasswordTokenAsString with temporary access
     * @return  true if authentication OK
     */
    public boolean authenticateWithChangePasswordToken(String username, String changePasswordTokenAsString) {
        String salt = ldapUserIdentityDao.getSalt(username);

        byte[] saltAsBytes;
        try {
            saltAsBytes = salt.getBytes(SALT_ENCODING);
        } catch (UnsupportedEncodingException e1) {
            throw new RuntimeException("Error with salt for username=" + username, e1);
        }
        ChangePasswordToken changePasswordToken = ChangePasswordToken.fromTokenString(changePasswordTokenAsString, saltAsBytes);
        //HUY: we don't store a temporary password in setTempPassword(), so we just compare the salt 
        //boolean ok = primaryLdapAuthenticator.authenticateWithTemporaryPassword(username, changePasswordToken.getPassword());
        boolean ok = changePasswordToken.getPassword().equals(temporary_pwd);
        log.info("authenticateWithChangePasswordToken was ok={} for username={}", ok, username);
        return ok;
    }


    public void changePassword(String username, String userUid, String newPassword) {
        ldapUserIdentityDao.changePassword(username, newPassword);
        audit(userUid,ActionPerformed.MODIFIED, "password", userUid);
    }


    public UserIdentity addUserIdentityWithGeneratedPassword(UserIdentity dto) {
        String username = dto.getUsername();
        if (username == null) {
            String msg = "Can not create a user without username!";
            throw new IllegalStateException(msg);
        }
        try {
            if (!searcher.usernameExists(username) && ldapUserIdentityDao.usernameExist(username)) {
            	ldapUserIdentityDao.deleteUserIdentity(username);
                //in LDAP
                //String msg = "User already exists, could not create user with username=" + dto.getUsername();
                //throw new IllegalStateException(msg);
            } else if (ldapUserIdentityDao.usernameExist(username)) {
            	String msg = "User already exists, could not create user with username=" + dto.getUsername();
                throw new IllegalStateException(msg);
            } 
        } catch (NamingException e) {
            throw new RuntimeException("usernameExist failed for username=" + dto.getUsername(), e);
        }


        String email;
        if (dto.getEmail() != null && dto.getEmail().contains("+")) {
            //email = replacePlusWithEmpty(dto.getEmail());
            email = dto.getEmail();
        } else {
            email = dto.getEmail();
        }
        if (email != null) {
            InternetAddress internetAddress = new InternetAddress();
            internetAddress.setAddress(email);
            try {
                internetAddress.validate();
            } catch (AddressException e) {
                throw new IllegalArgumentException(String.format("E-mail: %s is of wrong format.", email));
            }

            /*
            List<UIBUserIdentityRepresentation> usersWithSameEmail = searcher.search(email);
            if (!usersWithSameEmail.isEmpty()) {
                //(in lucene index)
                String msg = "E-mail " + email + " is already in use, could not create user with username=" + username;
                throw new IllegalStateException(msg);
            }
            */
        }

        String uid = UUID.randomUUID().toString();
        LDAPUserIdentity userIdentity = new LDAPUserIdentity(uid, dto.getUsername(), dto.getFirstName(), dto.getLastName(),
                email, passwordGenerator.generate(), dto.getCellPhone(), dto.getPersonRef());
        try {
            ldapUserIdentityDao.addUserIdentity(userIdentity);
            if(luceneIndexer.addToIndex(userIdentity)) {
            	HealthResource.setNumberOfUsers(searcher.getUserIndexSize());
            } else {
            	 throw new IllegalArgumentException("addUserIdentity failed for " + userIdentity.toString());
            }

        } catch (NamingException e) {
            throw new RuntimeException("addUserIdentity failed for " + userIdentity.toString(), e);
        }
        log.info("Added new user to LDAP: username={}, uid={}", username, uid);
        return userIdentity;
    }

    public static String replacePlusWithEmpty(String email){
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


    public LDAPUserIdentity getUserIdentityForUid(String uid) throws NamingException {
        LDAPUserIdentity userIdentity = ldapUserIdentityDao.getUserIndentityByUid(uid);
        if (userIdentity == null) {
            log.warn("Trying to access non-existing UID, removing from index: " + uid);
            luceneIndexer.removeFromIndex(uid);
        }
        return userIdentity;
    }

    public void updateUserIdentityForUid(String uid, LDAPUserIdentity newUserIdentity) {
        ldapUserIdentityDao.updateUserIdentityForUid(uid, newUserIdentity);
        luceneIndexer.updateIndex(newUserIdentity);
        audit(uid,ActionPerformed.MODIFIED, "user", newUserIdentity.toString());
    }


    public LDAPUserIdentity getUserIdentity(String usernameOrUid) throws NamingException {
        return ldapUserIdentityDao.getUserIndentity(usernameOrUid);
    }

    public void updateUserIdentity(String username, LDAPUserIdentity newuser) {
        ldapUserIdentityDao.updateUserIdentityForUsername(username, newuser);
        luceneIndexer.updateIndex(newuser);
    }

    public void deleteUserIdentity(String username) throws NamingException {
        luceneIndexer.removeFromIndex(getUserIdentity(username).getUid());
        ldapUserIdentityDao.deleteUserIdentity(username);
    }

    private void audit(String uid,String action, String what, String value) {
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(uid, now, action, what, value);
        auditLogDao.store(actionPerformed);
    }

    //FIXME baardl: implement verification that admin is allowed to update this password.
    //Find the admin user token, based on tokenid
    public boolean allowedToUpdate(String applicationtokenid, String adminUserTokenId) {
        return true;
    }

    //FIXME baardl: implement verification that admin is allowed to update this password.
    //Find the admin user token, based on tokenid
    public String findUserByTokenId(String adminUserTokenId) {
        return "not-found-not-implemented";
    }
}
