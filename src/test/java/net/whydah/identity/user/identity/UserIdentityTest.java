package net.whydah.identity.user.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2014-11-05
 */

public class UserIdentityTest {
    @Test
    public void testValidateCellPhoneOK() {
        UserIdentity userIdentity = new UserIdentity("uid1", "username1", "firstName1", "lastName1", "valid@email.dk", "password1", "", "personRef1");
        String[] telephoneNumbers = new String[]{"12345678", "+47 12345678", "+4799999999", "90 90 90 90", "", null};

        for (String telephoneNumber : telephoneNumbers) {
            userIdentity.setCellPhone(telephoneNumber);
            userIdentity.validate();
        }
    }
    @Test(expected = InvalidUserIdentityFieldException.class)
    public void testValidateCellPhoneInvalid() {
        UserIdentity userIdentity = new UserIdentity("uid1", "username1", "firstName1", "lastName1", "valid@email.dk", "password1", "", "personRef1");
        userIdentity.setCellPhone("900-FLYING-CIRCUS");
        userIdentity.validate();
    }


    @Test
    public void testValidatePersonRefOK() {
        UserIdentity userIdentity = new UserIdentity("uid", "username1", "firstName1", "lastName1", "valid@email.dk", "password1", null, "personRef1");
        String[] personRefs = new String[]{"0", "123", "abc", "123abc", "valid@email.dk", "123-456", "123/456", "", null};
        for (String personRef : personRefs) {
            userIdentity.setPersonRef(personRef);
            userIdentity.validate();
        }
    }

    @Test
    public void testParseUserIdentityJson() throws Exception {
        String userIdentityJson = "{\"username\":\"totto\", \"firstName\":\"Thor Henning\", \"lastName\":\"Hetland\", \"personRef\":\"\", \"email\":\"totto@totto.org\", \"cellPhone\":\"+4793009556\"}";
        UserIdentityRepresentation representation;
        ObjectMapper mapper = new ObjectMapper();

        representation = mapper.readValue(userIdentityJson, UserIdentityRepresentation.class);
    }

    @Test
    public void testNorthboundParseUserIdentityJson() throws Exception {
        String userIdentityJson = "{\"username\":\"totto\", \"firstName\":\"Thor Henning\", \"lastName\":\"Hetland\",  \"email\":\"totto@totto.org\", \"cellPhone\":\"+4793009556\"}";
        UserIdentityRepresentation representation;
        ObjectMapper mapper = new ObjectMapper();

        representation = mapper.readValue(userIdentityJson, UserIdentityRepresentation.class);
    }

}
