package net.whydah.identity.application.authentication;

/**
 * User: totto
 * Date: Nov 4, 2010
 * Time: 8:53:39 AM
 */
public class MockApplicationCredential {

    private String applicationID="1";
    private String applicationPassword ="thePasswrd";

    public String getApplicationID() {
        return applicationID;
    }

    public void setApplicationID(String applicationID) {
        this.applicationID = applicationID;
    }

    public String getApplicationPassword() {
        return applicationPassword;
    }

    public void setApplicationPassword(String applicationPassword) {
        this.applicationPassword = applicationPassword;
    }

    public String toXML(){
        if (applicationID == null){
            return templateToken;
        } else {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
            "<applicationcredential>\n" +
            "    <params>\n" +
            "        <applicationID>"+ applicationID +"</applicationID>\n" +
            "        <applicationSecret>"+ applicationPassword +"</applicationSecret>\n" +
            "    </params> \n" +
            "</applicationcredential>\n" ;
        }
    }

    private final String templateToken = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
            "<template>\n" +
            "    <applicationcredential>\n" +
            "        <params>\n" +
            "            <applicationID>"+ applicationID +"</applicationID>\n" +
            "            <applicationSecret>"+ applicationPassword +"</applicationSecret>\n" +
            "        </params> \n" +
            "    </applicationcredential>\n" +
            "</template>";

}
