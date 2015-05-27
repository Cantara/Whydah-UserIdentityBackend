package net.whydah.identity.dataimport;

import org.apache.commons.dbutils.QueryRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ApplicationImporter {
	private static final Logger log = LoggerFactory.getLogger(ApplicationImporter.class);
	
	private static final int REQUIRED_NUMBER_OF_FIELDS = 5;
	private static final int APPLICATIONID = 0;
	private static final int APPLICATIONNAME = 1;
	private static final int DEFAULTROLE = 2;
	private static final int DEFAULTORGANIZATIONNAME = 3;
    private static final int APPLICATIONSECRET = 4;

	private QueryRunner queryRunner;

	public ApplicationImporter(QueryRunner queryRunner) {
		this.queryRunner = queryRunner;
	}
	
	public void importApplications(InputStream applicationsSource) {
		List<Application> applications = parseApplications(applicationsSource);
		saveApplications(applications);
        log.info("{} applications imported.", applications.size());
	}

	private void saveApplications(List<Application> applications) {
        for (Application application: applications) {
            try {
                queryRunner.update("INSERT INTO Application (Id, Name, DefaultRoleName, DefaultOrgName, Secret) values (?, ?, ?, ?, ?)",
                        application.getId(), application.getName(), application.getDefaultRoleName(), application.getDefaultOrganizationId(),application.getApplicationSecret());
                log.info("Imported Application. Id {}, Name {}", application.getId(), application.getName());
            } catch(SQLException e) {
                log.error("Unable to persist application:", application.toString());
            } catch(Exception e) {
                log.error("Unable to persist applications.", e);
                throw new RuntimeException("Unable to persist applications.", e);
            }
        }
	}

	protected static List<Application> parseApplications(InputStream applicationsStream) {
		BufferedReader reader = null;
		try {
			List<Application> applications = new ArrayList<>();
	        reader = new BufferedReader(new InputStreamReader(applicationsStream, IamDataImporter.CHARSET_NAME));
            String line = null;
	        while (null != (line = reader.readLine())) {
	        	boolean isComment = line.startsWith("#");
				if (isComment) {
	        		continue;
	        	}
				
	        	String[] lineArray = line.split(",");
	        	validateLine(line, lineArray);
	        	
	        	String applicatinId = cleanString(lineArray[APPLICATIONID]);
	        	String applicationName = cleanString(lineArray[APPLICATIONNAME]);
	        	String defaultRoleName = cleanString(lineArray[DEFAULTROLE]);
	        	String defaultOrganizationId = cleanString(lineArray[DEFAULTORGANIZATIONNAME]);
                String applicationSecret = cleanString(lineArray[APPLICATIONSECRET]);

	        	Application application = new Application(applicatinId, applicationName, defaultRoleName, defaultOrganizationId,applicationSecret);
	            applications.add(application);
	        }
			return applications;
		
		} catch (IOException ioe) {
			log.error("Unable to read file {}", applicationsStream);
			throw new RuntimeException("Unable to import Application from file: " + applicationsStream);
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
			throw new RuntimeException("Applications parsing error. Incorrect format of Line. It does not contain all required fields. Line: " + line);
		}
	}
}
