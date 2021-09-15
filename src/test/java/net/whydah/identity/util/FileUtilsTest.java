package net.whydah.identity.util;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2019-03-01
 */
public class FileUtilsTest {

    @Test
    public void testCopyFilesOK() throws IOException {
        String dir = "target/FileUtilsTest/config_examples/";

        String file1 = "useridentitybackend.properties";
        String file2 = "logback.xml";
        FileUtils.copyFiles(new String[]{file1, file2}, dir, true);

        assertTrue(FileUtils.localFileExist(dir + file1));
        Path pathFile1 = Paths.get(dir).resolve(Paths.get(file1));
        long sizeFile1 = Files.size(pathFile1);
        assertTrue(sizeFile1 > 0);

        assertTrue(FileUtils.localFileExist(dir + file2));
        long sizeFile2 = Files.size(Paths.get(dir).resolve(Paths.get(file2)));
        assertTrue(sizeFile2 > 0);


        //change content and verify overwrite
        Files.write(pathFile1, "replaced something here".getBytes());
        assertNotEquals(sizeFile1, Files.size(pathFile1));
        FileUtils.copyFiles(new String[]{file1, file2}, dir, true);
        assertEquals(sizeFile1, Files.size(pathFile1));

        //change content and verify file is not overwritten
        Files.write(pathFile1, "replaced something here".getBytes());
        long sizeFile1Modified = Files.size(pathFile1);
        assertNotEquals(sizeFile1, sizeFile1Modified);
        FileUtils.copyFiles(new String[]{file1, file2}, dir, false);
        assertEquals(sizeFile1Modified, Files.size(pathFile1));
    }
}
