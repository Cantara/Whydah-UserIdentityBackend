package net.whydah.identity.user.resource;

//import com.sun.jersey.spi.template.ViewProcessor;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Hentet fra https://github.com/cwinters/jersey-freemarker/blob/master/src/com/cwinters/jersey/FreemarkerTemplateProvider.java
 *
 * Match a Viewable-named view with a Freemarker template.
 *
 * This class is based on the following original implementation:
 * http://github.com/cwinters/jersey-freemarker/
 *
 * <p>
 * You can configure the location of your templates with the context param
 * 'freemarker.template.path'. If not assigned we'll use a default of
 * <tt>WEB-INF/templates</tt>. Note that this uses Freemarker's
 * {@link freemarker.cache.WebappTemplateLoader} to load/cache the templates, so
 * check its docs (or crank up the logging under the 'freemarker.cache' package)
 * if your templates aren't getting loaded.
 * </p>
 *
 * <p>
 * This will put your Viewable's model object in the template variable "it",
 * unless the model is a Map. If so, the values will be assigned to the template
 * assuming the map is of type <tt>Map&lt;String,Object></tt>.
 * </p>
 *
 * <Useradminhelperp>
 * There are a number of methods you can override to change the behavior, such
 * as handling processing exceptions, changing the default template extension,
 * or adding variables to be assigned to every template context.
 * </p>
 *
 * @author Chris Winters <chris@cwinters.com> // original code
 * @author Olivier Grisel <ogrisel@nuxeo.com> // ViewProcessor refactoring
 */

//https://jersey.java.net/documentation/latest/mvc.html#d0e15092
@Provider
public class FreemarkerViewProcessor implements ViewProcessor<Template> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Configuration freemarkerConfig;

    public FreemarkerViewProcessor() {
    }

    /**
     * Catch any exception generated during template processing.
     *
     * @param e Exception caught
     * @param template path of template we're executing
     * @param out output stream from servlet container
     * @throws IOException on any write errors, or if you want to rethrow
     */
    private void onProcessException(final Exception e,
            final Template template, final OutputStream out) throws IOException {
        log.error("Error processing freemarker template @ {}", template.getName(), e);
        out.write("<pre>".getBytes("UTF-8"));
        e.printStackTrace(new PrintStream(out));
        out.write("</pre>". getBytes("UTF-8"));
       
    }

    private Configuration getConfig() {
        if (freemarkerConfig == null) {
            Configuration config = new Configuration();
            config.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates"));
            config.setDefaultEncoding("UTF-8");
            config.setOutputEncoding("UTF-8");
            config.setURLEscapingCharset("UTF-8");
            // don't always put a ',' in numbers (e.g., id=2000 vs id=2,000)
            config.setNumberFormat("0");
            config.setLocalizedLookup(false);
            config.setTemplateUpdateDelay(1800);
            freemarkerConfig = config;
        }
        return freemarkerConfig;
    }

    public Template resolve(final String path) {
        log.debug("Resolving path " + path);
        // accept both '/path/to/template' and '/path/to/template.ftl'
        final String defaultExtension = ".ftl";
        final String filePath = path.endsWith(defaultExtension) ? path : path
                + defaultExtension;
        try {
            return getConfig().getTemplate(filePath);
        } catch (IOException e) {
            log.error("Failed to load freemaker template: " + filePath);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public void writeTo(Template template, Viewable viewable, OutputStream out)
            throws IOException {
        out.flush(); // send status + headers

        Object model = viewable.getModel();
        final Map<String, Object> vars = new HashMap<String, Object>();
        if (model instanceof Map<?, ?>) {
            vars.putAll((Map<String, Object>) model);
        } else {
            vars.put("it", model);
        }

        final OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
        try {
            template.process(vars, writer);
        } catch (Exception e) {
            onProcessException(e, template, out);
        }
    }
}
