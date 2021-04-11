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
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @// TODO: 08/04/2021 kiversen
 *
 * - remove all ldap operations and replce with db persistence
 * - avoid intrusion in existing UserIdentityService by using a "shadow" version
 * - service will replace UserIdentityService when ldap is retired.
 * - service will be added to UserResource when ready
 * - service will live and be used in parallell with UserIdentityService for an overlapping period
 */
@Service
public class UserIdentityServiceV2 {
    private static final Logger log = LoggerFactory.getLogger(UserIdentityServiceV2.class);

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");
    private static final String SALT_ENCODING = "UTF-8";

    private final AuditLogDao auditLogDao;

    private final PasswordGenerator passwordGenerator;

    private final LuceneUserIndexer luceneIndexer;
    private final LuceneUserSearch searcher;
    private static String temporary_pwd=null;

    private final RDBMSLdapUserIdentityRepository userIdentityRepository;

    @Autowired
    public UserIdentityServiceV2(RDBMSLdapUserIdentityRepository userIdentityRepository, AuditLogDao auditLogDao, PasswordGenerator passwordGenerator,
                                 LuceneUserIndexer luceneIndexer, LuceneUserSearch searcher) {
        this.userIdentityRepository = userIdentityRepository;
        this.auditLogDao = auditLogDao;
        this.passwordGenerator = passwordGenerator;
        this.luceneIndexer = luceneIndexer;
        this.searcher = searcher;
    }

    public RDBMSUserIdentity authenticate(final String username, final String password) {
        return userIdentityRepository.authenticate(username, password);
    }


    public String setTempPassword(String username, String uid) {
    	if(temporary_pwd==null){
    		temporary_pwd = passwordGenerator.generate();
    	}
        String newPassword = temporary_pwd;
        String salt = passwordGenerator.generate();
        userIdentityRepository.setTempPassword(username, salt);
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
        String salt = userIdentityRepository.getSalt(username);

        byte[] saltAsBytes;
        try {
            saltAsBytes = salt.getBytes(SALT_ENCODING);
        } catch (UnsupportedEncodingException e1) {
            throw new RuntimeException("Error with salt for username=" + username, e1);
        }
        ChangePasswordToken changePasswordToken = ChangePasswordToken.fromTokenString(changePasswordTokenAsString, saltAsBytes);
        boolean ok = changePasswordToken.getPassword().equals(temporary_pwd);
        log.info("authenticateWithChangePasswordToken was ok={} for username={}", ok, username);
        return ok;
    }


    public void changePassword(String username, String userUid, String newPassword) {
        userIdentityRepository.changePassword(username, newPassword);
        audit(userUid,ActionPerformed.MODIFIED, "password", userUid);
    }


    public RDBMSUserIdentity addUserIdentityWithGeneratedPassword(UserIdentity dto) {
        String username = dto.getUsername();
        if (username == null) {
            String msg = "Can not create a user without username!";
            throw new IllegalStateException(msg);
        }
        try {
            if (!searcher.usernameExists(username) && userIdentityRepository.usernameExist(username)) {
            	userIdentityRepository.deleteUserIdentity(username);
            } else if (userIdentityRepository.usernameExist(username)) {
            	String msg = "User already exists, could not create user with username=" + dto.getUsername();
                throw new IllegalStateException(msg);
            } 
        } catch (RuntimeException e) {
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

        }

        // Must use already created uuid util we part from ldap
        // String uid = UUID.randomUUID().toString();
        String uid = dto.getUid();

        RDBMSUserIdentity userIdentity = new RDBMSUserIdentity(uid, dto.getUsername(), dto.getFirstName(), dto.getLastName(),
                email, passwordGenerator.generate(), dto.getCellPhone(), dto.getPersonRef());
        try {
            userIdentityRepository.addUserIdentity(userIdentity);
            if(luceneIndexer.addToIndex(userIdentity)) {
            	HealthResource.setNumberOfUsers(searcher.getUserIndexSize());
            } else {
            	 throw new IllegalArgumentException("addUserIdentity failed for " + userIdentity.toString());
            }

        } catch (RuntimeException e) {
            throw new RuntimeException("addUserIdentity failed for " + userIdentity.toString(), e);
        }
        log.info("Added new useridentity to DB: username={}, uid={}", username, uid);
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


    public RDBMSUserIdentity getUserIdentityForUid(String uid) {
        RDBMSUserIdentity userIdentity = userIdentityRepository.getUserIdentityWithId(uid);
        if (userIdentity == null) {
            log.warn("Trying to access non-existing UID, removing from index: " + uid);
            luceneIndexer.removeFromIndex(uid);
        }
        return userIdentity;
    }

    public void updateUserIdentityForUid(String uid, LDAPUserIdentity newUserIdentity) {
        UserIdentityConverter userIdentityConverter = new UserIdentityConverter();
        RDBMSUserIdentity userIdentity = userIdentityConverter.convertFromLDAPUserIdentity(newUserIdentity);

        userIdentityRepository.updateUserIdentityForUid(uid, userIdentity);
        luceneIndexer.updateIndex(newUserIdentity);
        audit(uid,ActionPerformed.MODIFIED, "user", newUserIdentity.toString());
    }

    // TODO: 08/04/2021 kiversen - Replace with either username or uid
    public RDBMSUserIdentity getUserIdentity(String usernameOrUid) {
        return userIdentityRepository.getUserIdentityWithUsernameOrUid(usernameOrUid);
    }

    public void updateUserIdentity(String username, RDBMSUserIdentity update) {
        userIdentityRepository.updateUserIdentityForUsername(username, update);
        log.info("Updated useridentity in DB: username={}, values={}", username, update);

        luceneIndexer.updateIndex(update);
    }

    public void deleteUserIdentity(String username) throws RuntimeException {
        luceneIndexer.removeFromIndex(getUserIdentity(username).getUid());
        userIdentityRepository.deleteUserIdentity(username);
    }

    private void audit(String uid,String action, String what, String value) {
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(uid, now, action, what, value);
        auditLogDao.store(actionPerformed);
    }


}
