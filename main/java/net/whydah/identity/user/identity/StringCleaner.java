package net.whydah.identity.user.identity;

/**

 * Created on Apr 12, 2007
 * @author Kushal Paudyal
 * www.sanjaal.com/java
 */

/**
 * This utility class has method to remove punctuation
 * marks from a given string
 */

public class StringCleaner {
    private final String legalCharacterSet;

    /**
     * Default Constructor
     * Create a string that contains standard legal characters.
     * This string will be used to reference whether the
     * characters in any 'to be cleaned'
     * string are to be kept or not.
     */

    public StringCleaner() {
        legalCharacterSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅÜÈÄÖÑ" +
                "abcdefghijklmnopqrstuvwxyzæøåüèäöñ" +
                "1234567890-+@. ";
    }

    /**
     * We may provide our own set of valid characters which
     * are to be used on testing the validity.
     * This set is provided as a string containing
     * valid characters.Order is not important.
     *
     * @param validCharacterSet
     */

    public StringCleaner(String validCharacterSet) {
        this.legalCharacterSet = validCharacterSet;
    }

    /**
     * @param str The string to be cleaned
     *            (unnecessary characters and whitespaces removed)
     * @return Cleaned String
     */

    public String cleanString(String str) {
        if(str == null) {
            return null;
        }
        StringBuilder cleanedString = new StringBuilder(str.length());
        for (int index = 0; index<str.length();index++)
        {

            char currentCharacter = str.charAt(index);
            if (legalCharacterSet.indexOf(currentCharacter) >=0) {
                cleanedString.append(currentCharacter);
            }
        }
        return cleanedString.toString().trim();
    }
}
