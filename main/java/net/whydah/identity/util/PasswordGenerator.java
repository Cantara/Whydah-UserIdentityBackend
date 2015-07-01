package net.whydah.identity.util;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class PasswordGenerator {
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final int LENGTH = 16;
    private static final String	CHARS = "qwertyuiopzxcvbnmasdfghjklAZERTYUIOPMLKJHGFDSQWXCVBN0123456789";
	private static final int CHARS_LENGTH = CHARS.length();

    public String generate() {
        StringBuilder password = new StringBuilder(LENGTH);
        for(int i=0; i<LENGTH; i++) {
            password.append(CHARS.charAt(RANDOM.nextInt(CHARS_LENGTH)));
        }
        return password.toString();
    }
}
