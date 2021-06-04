package net.whydah.identity.dataimport;

import net.whydah.identity.user.role.UserApplicationRoleEntryRepository;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RoleMappingImporter {
    private static final Logger log = LoggerFactory.getLogger(RoleMappingImporter.class);
	
	private static final int REQUIRED_NUMBER_OF_FIELDS = 6;
	private static final int USERNAME = 0;
	private static final int APPLICATIONID = 1;
	private static final int APPLICATIONNAME = 2;
	private static final int ORGANIZATIONNAME = 3;
	private static final int ROLENAME = 4;
	private static final int ROLEVALUE = 5;


	private final UserApplicationRoleEntryRepository userApplicationRoleEntryRepository;

	public RoleMappingImporter(UserApplicationRoleEntryRepository userApplicationRoleEntryRepository) {
		this.userApplicationRoleEntryRepository = userApplicationRoleEntryRepository;
	}

    public void importRoleMapping(InputStream roleMappingSource) {
		List<UserApplicationRoleEntry> roles = parseRoleMapping(roleMappingSource);
		saveRoleMapping(roles);
        log.info("{} roles imported.", roles.size());

        // Ignore and log warning about lucene update if problems with LDAP/AD lookup.
    }

	protected static List<UserApplicationRoleEntry> parseRoleMapping(InputStream roleMappingStream) {
		BufferedReader reader = null;
		try {
			List<UserApplicationRoleEntry> roleMappings = new ArrayList<>();
			reader = new BufferedReader(new InputStreamReader(roleMappingStream, IamDataImporter.CHARSET_NAME));
	        String line;
	        while (null != (line = reader.readLine())) {
	        	boolean isComment = line.startsWith("#");
				if (isComment) {
	        		continue;
	        	}
				
	        	String[] lineArray = line.split(",");
                log.trace("Importing entry=" + line);
                if (lineArray.length < REQUIRED_NUMBER_OF_FIELDS) {
                    log.warn("Role Mapping parsing error. Incorrect format of line. It does not contain all required fields. Line was NOT IMPORTED! Line=" + line);
                    continue;
                }

				UserApplicationRoleEntry userPropertyAndRole = new UserApplicationRoleEntry();

				userPropertyAndRole.setUserId(cleanString(lineArray[USERNAME]));
				
				userPropertyAndRole.setApplicationId(cleanString(lineArray[APPLICATIONID]));
	            userPropertyAndRole.setApplicationName(cleanString(lineArray[APPLICATIONNAME]));

				userPropertyAndRole.setOrgName(cleanString(lineArray[ORGANIZATIONNAME]));
				userPropertyAndRole.setRoleName(cleanString(lineArray[ROLENAME]));
				userPropertyAndRole.setRoleValue(cleanString(lineArray[ROLEVALUE]));

                log.trace("Added role:"+userPropertyAndRole);
	            roleMappings.add(userPropertyAndRole);
	        }
			return roleMappings;
		
		} catch (IOException ioe) {
			log.error("Unable to read file {}", roleMappingStream);
			throw new RuntimeException("Unable to import Role Mappings from file: " + roleMappingStream);
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
		return string == null ? string : string.trim();
	}

	private void saveRoleMapping(List<UserApplicationRoleEntry> roles) {
		roles.stream().forEach(userApplicationRoleEntry -> userApplicationRoleEntryRepository.addUserApplicationRoleEntry(userApplicationRoleEntry));
	}

}
