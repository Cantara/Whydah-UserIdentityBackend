package net.whydah.identity.dataimport;

import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.search.LuceneIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
	
    private LdapUserIdentityDao ldapUserIdentityDao;
    private LuceneIndexer luceneIndexer;
    
	public WhydahUserIdentityImporter(LdapUserIdentityDao ldapUserIdentityDao, LuceneIndexer luceneIndexer) {
		this.ldapUserIdentityDao = ldapUserIdentityDao;
		this.luceneIndexer = luceneIndexer;
	}
    
    public void importUsers(InputStream userImportSource) {
        List<UserIdentity> users = parseUsers(userImportSource);
        int userAddedCount = saveUsers(users);
        log.info("{} users imported.", userAddedCount);
    }

	protected static List<UserIdentity> parseUsers(InputStream userImportStream) {
		BufferedReader reader = null;
		try {
			List<UserIdentity> users = new ArrayList<>();
	        reader = new BufferedReader(new InputStreamReader(userImportStream, IamDataImporter.CHARSET_NAME));
	        String line;
	        while (null != (line = reader.readLine())) {
	        	boolean isComment = line.startsWith("#");
				if (isComment) {
	        		continue;
	        	}
				
	        	String[] lineArray = line.split(",");
	        	validateLine(line, lineArray);
	        	
	        	UserIdentity userIdentity;
	        	userIdentity = new UserIdentity();
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

    private int saveUsers(List<UserIdentity> users) {
        int userAddedCount = 0;
        try {
            for (UserIdentity userIdentity : users) {
                boolean added = ldapUserIdentityDao.addUserIdentity(userIdentity);
                if (added) {
                    log.info("Imported user: uid={}, username={}, name={} {}, email={}",
                            userIdentity.getUid(), userIdentity.getUsername(), userIdentity.getFirstName(), userIdentity.getLastName(), userIdentity.getEmail());
                    userAddedCount++;
                }
            }

            luceneIndexer.addToIndex(users);
        } catch (Exception e) {
            log.error("Error importing users!", e);
            throw new RuntimeException("Error importing users!", e);
        }
        return userAddedCount;
    }
}
