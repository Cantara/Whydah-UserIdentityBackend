package net.whydah.identity.config;

import java.util.Arrays;

/**
 * Get application mode from os environment or system property.
 */
@Deprecated
public class ApplicationMode {
    public final static String IAM_MODE_KEY = "IAM_MODE";
    public final static String PROD = "PROD";
    public final static String TEST = "TEST";
    public final static String TEST_L = "TEST_LOCALHOST";
    public final static String DEV = "DEV";

    public static String getApplicationMode() {
        String appMode = System.getenv(IAM_MODE_KEY);
        if(appMode == null) {
            appMode = System.getProperty(IAM_MODE_KEY);
        }
        if(appMode == null) {
            System.err.println(IAM_MODE_KEY + " not defined. Must be one of PROD, TEST, DEV.");
            System.exit(4);
        }
        if(!Arrays.asList(PROD, TEST, TEST_L, DEV).contains(appMode)) {
            System.err.println("Unknown " + IAM_MODE_KEY + ": " + appMode);
            System.exit(5);
        }
        //System.out.println(String.format("Running in %s mode", appMode));
        return appMode;
    }
}
