package org.openlumify.core.formula;

import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@SuppressWarnings("unused")
public class RequireJsSupport extends ScriptableObject {
    private static final long serialVersionUID = 1L;
    private static OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(RequireJsSupport.class);

    @Override
    public String getClassName() {
        return "RequireJsSupport";
    }

    public static void print(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        for (Object arg : args) {
            LOGGER.debug(Context.toString(arg));
        }
    }

    public static void consoleWarn(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        for (Object arg : args) {
            LOGGER.warn(Context.toString(arg));
        }
    }

    public static void consoleError(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        for (Object arg : args) {
            LOGGER.error(Context.toString(arg));
        }
    }

    public static void load(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws IOException {
        RequireJsSupport shell = (RequireJsSupport) getTopLevelScope(thisObj);
        for (Object arg : args) {
            shell.processSource(cx, Context.toString(arg));
        }
    }

    public static String readFully(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws IOException {
        RequireJsSupport shell = (RequireJsSupport) getTopLevelScope(thisObj);
        if (args.length == 1) {
            return shell.getFileContents(Context.toString(args[0]));
        }
        return null;
    }

    public static String transformFilePath(String filename) {
        return filename.startsWith("../") ? (filename.replace("../", "")) : ("jsc/" + filename);
    }

    private void processSource(Context cx, String filename) throws IOException {
        String fileContents = getFileContents(filename);
        cx.evaluateString(this, fileContents, filename, 1, null);
    }

    private String getFileContents(String file) {
        String transformed = transformFilePath(file);
        try (InputStream is = RequireJsSupport.class.getResourceAsStream(transformed)) {
            if (is == null) {
                throw new OpenLumifyException("File not found: " + transformed);
            }
            return IOUtils.toString(is, Charset.forName("UTF-8"));
        } catch (IOException ex) {
            throw new OpenLumifyException("Could not read file contents: " + transformed, ex);
        }
    }
}

