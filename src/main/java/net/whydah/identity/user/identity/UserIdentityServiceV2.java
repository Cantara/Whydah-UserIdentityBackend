package net.whydah.identity.user.identity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.health.HealthResource;
import net.whydah.identity.user.ChangePasswordToken;
import net.whydah.identity.user.search.LuceneUserIndexer;
import net.whydah.identity.user.search.LuceneUserSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserIdentityServiceV2 {
    private static final Logger log = LoggerFactory.getLogger(UserIdentityServiceV2.class);

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");
    private static final String SALT_ENCODING = "UTF-8";

    private final AuditLogDao auditLogDao;

    private final LuceneUserIndexer luceneIndexer;
    private final LuceneUserSearch searcher;
    private final BCryptService bCryptService;
    private final Map<String, String> temporaryPwdByUsername = new ConcurrentHashMap<>();

    private final RDBMSUserIdentityRepository userIdentityRepository;

    @Autowired
    public UserIdentityServiceV2(RDBMSUserIdentityRepository userIdentityRepository, AuditLogDao auditLogDao,
                                 LuceneUserIndexer luceneIndexer, LuceneUserSearch searcher, BCryptService bCryptService) {
        this.userIdentityRepository = userIdentityRepository;
        this.auditLogDao = auditLogDao;
        this.luceneIndexer = luceneIndexer;
        this.searcher = searcher;
        this.bCryptService = bCryptService;
        HealthResource.setNumberOfUsersDB(userIdentityRepository.countUsers());
    }

    public RDBMSUserIdentity authenticate(final String username, final String password) {
        return userIdentityRepository.authenticate(username, password);
    }

    // TODO: 22/04/2021 kiversen: remove this when transition to db is complete
    public String setTempPassword(String username, String uid, String newPassword, String salt) {
        temporaryPwdByUsername.put(username, newPassword);
        //HUY: disable saving a new password
        userIdentityRepository.setTempPassword(username, salt);
        audit(uid, ActionPerformed.MODIFIED, "resetpassword", uid);

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
     *
     * @param username                    username to authenticate
     * @param changePasswordTokenAsString with temporary access
     * @return true if authentication OK
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
        String temporaryPassword = temporaryPwdByUsername.get(username);
        boolean ok;
        if (temporaryPassword == null) {
            log.warn("authenticateWithChangePasswordToken could not find a temporary password. Please reset password again. username={}", username);
            ok = false;
        } else {
            ok = changePasswordToken.getPassword().equals(temporaryPassword);
        }
        log.info("authenticateWithChangePasswordToken was ok={} for username={}", ok, username);
        return ok;
    }


    public void changePassword(String username, String userUid, String newPassword) {
        String bcryptString = bCryptService.hash(newPassword);
        userIdentityRepository.changePassword(username, bcryptString);
        audit(userUid, ActionPerformed.MODIFIED, "password", userUid);
    }


    public RDBMSUserIdentity addUserIdentityWithGeneratedPassword(UserIdentityWithAutomaticPasswordGeneration dto) {
        String username = dto.getUsername();
        if (username == null) {
            String msg = "Can not create a user without username!";
            throw new IllegalStateException(msg);
        }

        
        if (!searcher.usernameExists(username) && userIdentityRepository.usernameExist(username)) {
            userIdentityRepository.deleteUserIdentity(username);
        } else if (userIdentityRepository.usernameExist(username)) {
            String msg = "User already exists, could not create user with username=" + dto.getUsername();
            throw new IllegalStateException(msg);
        }
        
//        if(userIdentityRepository.usernameExist(username)) {
//            String msg = "User already exists, could not create user with username=" + dto.getUsername();
//            throw new IllegalStateException(msg);
//        }

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

        // Use already created uuid if one exists
        String tentativeUid = UUID.randomUUID().toString();
        String uid = dto.getUid() != null ? dto.getUid() : tentativeUid;
        String passwordPlaintext = dto.getPassword();
        RDBMSUserIdentity userIdentity = new RDBMSUserIdentity(uid, dto.getUsername(), dto.getFirstName(), dto.getLastName(),
                email, passwordPlaintext, dto.getCellPhone(), dto.getPersonRef());
        try {
            userIdentityRepository.addUserIdentity(userIdentity);
            if (luceneIndexer.addToIndex(userIdentity)) {
                int usersInDb = userIdentityRepository.countUsers();
                HealthResource.setNumberOfUsersDB(usersInDb);
            } else {
                throw new IllegalArgumentException("addUserIdentity to DB failed for " + userIdentity.toString());
            }

        } catch (RuntimeException e) {
            throw new RuntimeException("addUserIdentity to DB failed for " + userIdentity.toString(), e);
        }
        log.info("Added new useridentity to DB: username={}, uid={}", username, uid);
        return userIdentity;
    }

    public static String replacePlusWithEmpty(String email) {
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


    public RDBMSUserIdentity getUserIdentityForUid(String uid) throws RuntimeException {
        RDBMSUserIdentity userIdentity = userIdentityRepository.getUserIdentityWithId(uid);
        if (userIdentity == null) {
            log.warn("Trying to access non-existing UID, removing from index: " + uid);
            luceneIndexer.removeFromIndex(uid);
        }
        return userIdentity;
    }

    public void updateUserIdentityForUid(String uid, LuceneUserIdentity newUserIdentity) {
        UserIdentityConverter userIdentityConverter = new UserIdentityConverter(bCryptService);
        RDBMSUserIdentity userIdentity = userIdentityConverter.convertFromLuceneUserIdentity(newUserIdentity);

        userIdentityRepository.updateUserIdentityForUid(uid, userIdentity);
        luceneIndexer.updateIndex(newUserIdentity);
        audit(uid, ActionPerformed.MODIFIED, "user", newUserIdentity.toString());
    }

    // TODO: 08/04/2021 kiversen - Replace with either username or uid
    public RDBMSUserIdentity getUserIdentity(String usernameOrUid) throws RuntimeException {
        return userIdentityRepository.getUserIdentityWithUsernameOrUid(usernameOrUid);
    }

    public void updateUserIdentity(String uid, RDBMSUserIdentity update) throws RuntimeException {
        String json = null;
        try {
            json = new ObjectMapper().writeValueAsString(update);
        } catch (JsonProcessingException jpe) {
            //
        }
        log.info("update with {} and {}", uid, json);
        userIdentityRepository.updateUserIdentityForUid(uid, update);
        log.info("Updated useridentity in DB: uid={}, values={}", uid, update);

        luceneIndexer.updateIndex(update);
    }

    public void deleteUserIdentity(String username) throws RuntimeException {
        luceneIndexer.removeFromIndex(getUserIdentity(username).getUid());
        userIdentityRepository.deleteUserIdentity(username);
        HealthResource.setNumberOfUsersDB(userIdentityRepository.countUsers());
    }

    private void audit(String uid, String action, String what, String value) {
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(uid, now, action, what, value);
        auditLogDao.store(actionPerformed);
    }


}
