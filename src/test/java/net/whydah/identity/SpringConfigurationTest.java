package net.whydah.identity;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.assertNotNull;

public class SpringConfigurationTest {

    @Test
    public void verifyConfigurationTest() {
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("context.xml");
        assertNotNull("lucene userdirectory is not null", classPathXmlApplicationContext.getBean("luceneUserDirectory"));
        assertNotNull("lucene applicationdirectory is not null", classPathXmlApplicationContext.getBean("luceneApplicationDirectory"));
        assertNotNull(classPathXmlApplicationContext.getBean("objectMapper"));
    }
}
