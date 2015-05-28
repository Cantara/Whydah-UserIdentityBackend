package net.whydah.identity.user.email;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.HashMap;


@Component
public class EmailBodyGenerator {
    private final Configuration freemarkerConfig;
    private static final String NEW_USER_EMAIL_TEMPLATE = "WelcomeNewUser.ftl";
    private static final String RESET_PASSWORD_EMAIL_TEMPLATE = "PasswordResetEmail.ftl";

    public EmailBodyGenerator() {
        freemarkerConfig = new Configuration();
        freemarkerConfig.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates/email"));
        freemarkerConfig.setDefaultEncoding("UTF-8");
        freemarkerConfig.setLocalizedLookup(false);
        freemarkerConfig.setTemplateUpdateDelay(60000);
    }


    public String resetPassword(String url, String username) {
        HashMap<String, String> model = new HashMap<>();
        model.put("username", username);
        model.put("url", url);
        return createBody(RESET_PASSWORD_EMAIL_TEMPLATE, model);
    }

    /*
    public String newUser(String name, String systemname, String url) {
        HashMap<String, String> model = new HashMap<>();
        model.put("name", name);
        model.put("url", url);
        return createBody(NEW_USER_EMAIL_TEMPLATE, model);
    }
    */

    private String createBody(String templateName, HashMap<String, String> model) {
        StringWriter stringWriter = new StringWriter();
        try {
            Template template = freemarkerConfig.getTemplate(templateName);
            template.process(model, stringWriter);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Populating template failed. templateName=" + templateName, e);
        }
        return stringWriter.toString();
    }
}
