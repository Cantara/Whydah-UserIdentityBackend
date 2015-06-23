package net.whydah.identity.dataimport;

import org.apache.commons.dbutils.QueryRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class OrganizationImporter {
	private static final Logger log = LoggerFactory.getLogger(OrganizationImporter.class);
	
	private static final int REQUIRED_NUMBER_OF_FIELDS = 2;
	private static final int APPID = 0;
	private static final int ORGANIZATIONNAME = 1;
	
	private QueryRunner queryRunner;
	
	public OrganizationImporter(QueryRunner queryRunner) {
		this.queryRunner = queryRunner;
	}

	public void importOrganizations(InputStream organizationsSource) {
		List<Organization> organizations = parseOrganizations(organizationsSource);
		saveOrganizations(organizations);
        log.info("{} organizations imported.", organizations.size());
	}
	
	protected static List<Organization> parseOrganizations(InputStream organizationsStream) {
		BufferedReader reader = null;
		try {
			List<Organization> organizations = new ArrayList<>();
            reader = new BufferedReader(new InputStreamReader(organizationsStream, IamDataImporter.CHARSET_NAME));
	        String line = null; 
	        while (null != (line = reader.readLine())) {
	        	boolean isComment = line.startsWith("#");
				if (isComment) {
	        		continue;
	        	}
				
	        	String[] lineArray = line.split(",");
	        	validateLine(line, lineArray);
	        	
	        	String appId = cleanString(lineArray[APPID]);
	        	String organizationName = cleanString(lineArray[ORGANIZATIONNAME]);
	        	
	        	Organization organization = new Organization(appId, organizationName);
	            organizations.add(organization);
	        }
			return organizations;
		
		} catch (IOException ioe) {
			log.error("Unable to read file {}", organizationsStream);
			throw new RuntimeException("Unable to import Organizations from file: " + organizationsStream);
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
			throw new RuntimeException("Organizations parsing error. Incorrect format of Line. It does not contain all required fields. Line: " + line);
		}
	}

    private void saveOrganizations(List<Organization> organizations) {
        try {
            for (Organization organization: organizations) {
                queryRunner.update("INSERT INTO Organization values (?, ?)", organization.getAppId(), organization.getName());
                log.info("Imported Organization. Id {}, Name {}", organization.getAppId(), organization.getName());
            }
        } catch(Exception e) {
            log.error("Unable to persist organizations.", e);
            throw new RuntimeException("Unable to persist organizations.", e);
        }
    }
}

