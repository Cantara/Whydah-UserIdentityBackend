package net.whydah.identity;

import net.whydah.identity.util.FileUtils;
import org.junit.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertTrue;

@Ignore     //Review: Still useful?
public class MainTest {

    private ProcessBuilder processBuilder;
    private Process process;

    @BeforeClass
    public static void init() {
        FileUtils.deleteDirectory(new File("target/ssotest/"));
    }

    @AfterClass
    public static void cleanup() {
        FileUtils.deleteDirectory(new File("target/ssotest/"));
    }

    @Before
    public void setup() {
        processBuilder = new ProcessBuilder("java", "-cp", System.getProperty("java.class.path"), "net.whydah.identity.Main");
    }

    @After
    public void tearDown() {
        process.destroy();
    }

    @Test
    public void whenEnvironmentNotSpecifiedThenTerminateUnexpectedly() throws Exception {
        processBuilder.environment().remove("IAM_MODE");
        process = processBuilder.start();
        assertHasTerminatedUnexpectedly(process);
    }

    @Test
    public void whenUnknownEnvironmentSpecifiedThenTerminateUnexpectedly() throws Exception {
        setIAM_MODEenvironment("NUDLER");
        process = processBuilder.start();
        assertHasTerminatedUnexpectedly(process);
    }

    @Test
    public void whenValidEnvironmentSpecifiedThenDontExit() throws Exception {
        setIAM_MODEenvironment("JUNIT");
        process = processBuilder.start();
        assertWebappStarted();
    }

    private void assertWebappStarted() throws IOException {
        boolean webappStarted = false;
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.contains("Jersey webapp started")) {
                webappStarted = true;
                break;
            }
        }
        assertTrue(webappStarted);
    }

    private void setIAM_MODEenvironment(String value) {
        processBuilder.environment().put("IAM_MODE", value);
    }

    private void assertHasTerminatedUnexpectedly(Process p) throws InterruptedException {
        assertTrue(p.waitFor() != 0);
    }
}
