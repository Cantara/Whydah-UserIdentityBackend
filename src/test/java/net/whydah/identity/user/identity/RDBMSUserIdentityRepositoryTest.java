package net.whydah.identity.user.identity;

import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static net.whydah.identity.Main.initBasicDataSource;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RDBMSUserIdentityRepositoryTest {

    @Test
    public void testAuthenticate() {

        /*
         * Given
         */

        ConstrettoConfiguration configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("classpath:useridentitybackend-test-override.properties"))
                .done()
                .getConfiguration();
        BasicDataSource dataSource = initBasicDataSource(configuration);

        DatabaseMigrationHelper dbHelper = new DatabaseMigrationHelper(dataSource);
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();

        RDBMSUserIdentityDao rdbmsUserIdentityDao = new RDBMSUserIdentityDao(dataSource);
        BCryptService bCryptService = new BCryptService(configuration.evaluateToString("userdb.password.pepper"), configuration.evaluateToInt("userdb.password.bcrypt.preferredcost"));
        RDBMSUserIdentityRepository rdbmsUserIdentityRepository = new RDBMSUserIdentityRepository(rdbmsUserIdentityDao, bCryptService, configuration);

        String myPassword = bCryptService.hash("my awesome secret");
        rdbmsUserIdentityDao.create(new RDBMSUserIdentity(UUID.randomUUID().toString(), "myself", "Me", "Mine", "me@mine.me", myPassword, null, null));

        String yourPassword = createIdentityServer3Hash("your awesome secret");
        rdbmsUserIdentityDao.create(new RDBMSUserIdentity(UUID.randomUUID().toString(), "yourself", "You", "Your", "you@yours.you", yourPassword, null, null));

        String myCleartextPassword = "my cleartext secret";
        rdbmsUserIdentityDao.create(new RDBMSUserIdentity(UUID.randomUUID().toString(), "clearmyself", "ClearMe", "Mine", "clearme@mine.me", myCleartextPassword, null, null));

        /*
         * When and Then
         */

        assertNull(rdbmsUserIdentityRepository.authenticate("myself", "my bad secret"));
        assertNotNull(rdbmsUserIdentityRepository.authenticate("myself", "my awesome secret"));
        assertNull(rdbmsUserIdentityRepository.authenticate("myself", "my bad secret"));

        assertNull(rdbmsUserIdentityRepository.authenticate("yourself", "your bad secret"));
        assertNotNull(rdbmsUserIdentityRepository.authenticate("yourself", "your awesome secret"));
        assertNull(rdbmsUserIdentityRepository.authenticate("yourself", "your bad secret"));

        assertNull(rdbmsUserIdentityRepository.authenticate("clearmyself", "your bad secret"));
        assertNotNull(rdbmsUserIdentityRepository.authenticate("clearmyself", "my cleartext secret"));
        assertNull(rdbmsUserIdentityRepository.authenticate("clearmyself", "your bad secret"));
        assertNull(rdbmsUserIdentityRepository.authenticate("clearmyself", "your bad secret"));
    }

    private String createIdentityServer3Hash(String password) {
        SecureRandom rnd = new SecureRandom();
        byte[] salt = new byte[16];
        rnd.nextBytes(salt);
        byte[] subkey = PBKDF2WithHMACSha1PasswordAuthenticator.hashPassword(password, salt);
        byte[] buf = new byte[49];
        ByteBuffer bb = ByteBuffer.wrap(buf);
        bb.put((byte) 0);
        bb.put(salt);
        bb.put(subkey);
        return Base64.getEncoder().encodeToString(buf);
    }
}