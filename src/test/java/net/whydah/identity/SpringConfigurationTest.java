package net.whydah.identity;

import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import org.apache.commons.dbcp.BasicDataSource;
import net.whydah.identity.config.ApplicationConfiguration;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.assertNotNull;

public class SpringConfigurationTest {

    @Test
    public void verifyConfigurationTest() {
        ApplicationConfiguration configuration = new ApplicationConfiguration("classpath:useridentitybackend.properties", "file:./useridentitybackend_override.properties");
        BasicDataSource dataSource = initBasicDataSource(configuration);
        DatabaseMigrationHelper dbHelper = new DatabaseMigrationHelper(dataSource);
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();

        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("context.xml");
        assertNotNull("lucene userdirectory is not null", classPathXmlApplicationContext.getBean("luceneUserDirectory"));
        assertNotNull("lucene applicationdirectory is not null", classPathXmlApplicationContext.getBean("luceneApplicationDirectory"));
        assertNotNull(classPathXmlApplicationContext.getBean("objectMapper"));
    }

    private static BasicDataSource initBasicDataSource(ApplicationConfiguration configuration) {
        String jdbcdriver = configuration.evaluateToString("roledb.jdbc.driver");
        String jdbcurl = configuration.evaluateToString("roledb.jdbc.url");
        String roledbuser = configuration.evaluateToString("roledb.jdbc.user");
        String roledbpasswd = configuration.evaluateToString("roledb.jdbc.password");

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jdbcdriver);
        dataSource.setUrl(jdbcurl);
        dataSource.setUsername(roledbuser);
        dataSource.setPassword(roledbpasswd);
        return dataSource;
    }
}
