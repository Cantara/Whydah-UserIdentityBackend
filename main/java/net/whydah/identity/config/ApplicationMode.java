package net.whydah.identity.config;


/**
 * Mange Constretto configuration tags and get application mode from os environment or system property.
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-05-28
 */
public class ApplicationMode {
    public static final String DEV_MODE = "dev";
    public static final String CI_MODE = "ci";
    public static final String NO_SECURITY_FILTER = "noSecurityFilter";
    private static final String CONSTRETTO_TAGS = "CONSTRETTO_TAGS";

    public static boolean skipSecurityFilter() {
        String tags = System.getenv(CONSTRETTO_TAGS);
        if (tags == null || tags.isEmpty()) {
            tags = System.getProperty(CONSTRETTO_TAGS);
        }
        return tags != null && tags.contains(NO_SECURITY_FILTER);
    }

    public static void setCIMode() {
        setTags(CI_MODE);
    }

    public static void setTags(String... tags) {
        String tagsAsString = String.join(",", tags);
        System.setProperty(CONSTRETTO_TAGS, tagsAsString);
    }

    public static void clearTags() {
        System.clearProperty(CONSTRETTO_TAGS);
    }
}
