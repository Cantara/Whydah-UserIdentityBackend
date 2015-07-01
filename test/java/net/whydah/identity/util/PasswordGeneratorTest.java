package net.whydah.identity.util;

import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class PasswordGeneratorTest {
    @Test
    public void testGeneratePasswords() {
        PasswordGenerator pwg = new PasswordGenerator();
        for(int i=0; i<100; i++) {
            String password = pwg.generate();
            assertNotNull(password);
            assertTrue(password.length() > 0);
        }
    }

    @Test
    public void testUTF8() throws UnsupportedEncodingException {
        String myString = "HallstrÃ¸m"; //"\u0048\u0065\u006C\u006C\u006F World";
        myString.getBytes("UTF-8");

        //byte[] myBytes = myString.getBytes("UTF-8");

        /*
        try {
            myBytes = myString.getBytes("UTF-8");
            System.out.println("UTF8:"+ new String(myString.getBytes("ISO-8859-1"), "UTF-8"));
            System.out.println("ISO-8859-1:"+ new String(myString.getBytes("ISO-8859-1"), "ISO-8859-1"));
        } catch (UnsupportedEncodingException e) {
            System.out.println("UTF8"+myBytes);
            System.exit(-1);
        }

        for (byte myByte : myBytes) {
            System.out.println(myByte);
        }
        */
    }
}
