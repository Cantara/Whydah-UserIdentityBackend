package net.whydah.identity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public final class PasswordBlacklist {
    private static final Logger log = LoggerFactory.getLogger(PasswordBlacklist.class);
    private static final String PASSWORD_FILE = "common-pw.txt";

    public static final Set<String> pwList = new HashSet<>();

    static {
        loadCommonPWFromClasspath();
    }

    /*
    public static final PasswordBlacklist instance = new PasswordBlacklist();
    private PasswordBlacklist() {
        loadCommonPWFromClasspath();
    }
    */

    private static void loadCommonPWFromClasspath() {
        String pwfile = String.format(PASSWORD_FILE);
        log.info("Loading properties from classpath: {}", pwfile);
        InputStream is = PasswordBlacklist.class.getClassLoader().getResourceAsStream(pwfile);
        if (is == null) {
            throw new RuntimeException("Error reading " + pwfile + " from classpath.");
        }
        try {
            String line;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
            while ((line = bufferedReader.readLine()) != null) {
                pwList.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading " + pwfile + " from classpath.", e);
        }
    }

}
