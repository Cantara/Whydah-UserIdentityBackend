package net.whydah.identity.dataimport;

import net.whydah.identity.user.identity.*;
import net.whydah.identity.user.search.LuceneUserIndexer;
import net.whydah.sso.user.types.UserIdentity;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class WhydahUserIdentityImporter {
	private static final Logger log = LoggerFactory.getLogger(WhydahUserIdentityImporter.class);

	private static final int REQUIRED_NUMBER_OF_FIELDS = 8;
	private static final int USERID = 0;
	private static final int USERNAME = 1;
	private static final int PASSWORD = 2;
	private static final int FIRSTNAME = 3;
	private static final int LASTNAME = 4;
	private static final int EMAIL = 5;
	private static final int CELLPHONE = 6;
	private static final int PERSONREF = 7;
	
    private final LdapUserIdentityDao ldapUserIdentityDao;
    private final RDBMSLdapUserIdentityRepository userIdentityRepository;

    public LuceneUserIndexer luceneIndexer;

    @Autowired
    public WhydahUserIdentityImporter(LdapUserIdentityDao ldapUserIdentityDao, RDBMSLdapUserIdentityRepository userIdentityRepository, Directory index) throws IOException {
        this.ldapUserIdentityDao = ldapUserIdentityDao;
        this.userIdentityRepository = userIdentityRepository;
        this.luceneIndexer = new LuceneUserIndexer(index);
    }
    
    public void importUsers(InputStream userImportSource) {
        List<LDAPUserIdentity> users = parseUsers(userImportSource);
        int userAddedLdapCount = saveUsers(users);
        int userAddedDBCount = saveUsersToDB(users);
        log.info("{} users imported to LDAP.", userAddedLdapCount);
        log.info("{} users imported to DB", userAddedDBCount);
    }

    protected static List<LDAPUserIdentity> parseUsers(InputStream userImportStream) {
        BufferedReader reader = null;
		try {
            List<LDAPUserIdentity> users = new ArrayList<>();
            reader = new BufferedReader(new InputStreamReader(userImportStream, IamDataImporter.CHARSET_NAME));
	        String line;
	        while (null != (line = reader.readLine())) {
                boolean isComment = line.startsWith("#");
                // Skip comments and empty lines
                if (isComment || line.length() < 2) {
                    continue;
                }

                String[] lineArray = line.split(",");
                validateLine(line, lineArray);

                LDAPUserIdentity userIdentity;
                userIdentity = new LDAPUserIdentity();
                userIdentity.setUid(cleanString(lineArray[USERID]));
                userIdentity.setUsername(cleanString(lineArray[USERNAME]));
	        	userIdentity.setPassword(cleanString(lineArray[PASSWORD]));
	            userIdentity.setFirstName(cleanString(lineArray[FIRSTNAME]));
	            userIdentity.setLastName(cleanString(lineArray[LASTNAME]));
	            userIdentity.setEmail(cleanString(lineArray[EMAIL]));
	            userIdentity.setCellPhone(cleanString(lineArray[CELLPHONE]));
	            userIdentity.setPersonRef(cleanString(lineArray[PERSONREF]));
	            
	            users.add(userIdentity);
	        }
			return users;
		
		} catch (IOException ioe) {
			log.error("Unable to read file {}", userImportStream);
			throw new RuntimeException("Unable to import users from file: " + userImportStream);
		} finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warn("Error closing stream", e);
                }
            }
        }
	}

	private static String cleanString(String string) {
		return string==null ? string : string.trim();
	}

	private static void validateLine(String line, String[] lineArray) {
		if (lineArray.length < REQUIRED_NUMBER_OF_FIELDS) {
			throw new RuntimeException("User parsing error. Incorrect format of Line. It does not contain all required fields. Line: " + line);
		}
	}

    private int saveUsers(List<LDAPUserIdentity> users) {
        int userAddedCount = 0;
        try {
            List<UserIdentity> userIdentities = new LinkedList<>();
            for (LDAPUserIdentity userIdentity : users) {
                boolean added = ldapUserIdentityDao.addUserIdentity(userIdentity);
                if (added) {
                    log.info("Imported user: uid={}, username={}, name={} {}, email={}",
                            userIdentity.getUid(), userIdentity.getUsername(), userIdentity.getFirstName(), userIdentity.getLastName(), userIdentity.getEmail());
                    userAddedCount++;
                }
                userIdentities.add(userIdentity);
            }

            luceneIndexer.addToIndex(userIdentities);

        } catch (Exception e) {
            log.error("Error importing users to LDAP!", e);
        }

        return userAddedCount;
    }

    private int saveUsersToDB(List<LDAPUserIdentity> users) {
        int userAddedCount = 0;
        try {
            List<UserIdentity> userIdentities = new LinkedList<>();
            for (LDAPUserIdentity userIdentity : users) {
                UserIdentityConverter converter = new UserIdentityConverter();
                RDBMSUserIdentity rdbmsUserIdentity = converter.convertFromLDAPUserIdentity(userIdentity);

                boolean added = userIdentityRepository.addUserIdentity(rdbmsUserIdentity);

                if (added) {
                    log.info("Imported user to DB: uid={}, username={}, name={} {}, email={}",
                            userIdentity.getUid(), userIdentity.getUsername(), userIdentity.getFirstName(), userIdentity.getLastName(), userIdentity.getEmail());
                    userAddedCount++;
                }
                userIdentities.add(userIdentity);
            }
            luceneIndexer.addToIndex(userIdentities);
        } catch (Exception e) {
            log.error("Error importing users to DB!", e);
        }

        return userAddedCount;
    }
}
