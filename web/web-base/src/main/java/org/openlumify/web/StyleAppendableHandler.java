package org.openlumify.web;

import com.asual.lesscss.LessEngine;
import com.asual.lesscss.LessOptions;
import org.visallo.webster.HandlerChain;
import org.visallo.webster.RequestResponseHandler;
import org.apache.commons.io.IOUtils;
import org.openlumify.core.exception.OpenLumifyException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkNotNull;

public class StyleAppendableHandler implements RequestResponseHandler {
    private LessEngine lessCompiler;
    private String css = "";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        response.setContentType("text/css");
        try (ServletOutputStream out = response.getOutputStream()) {
            out.write(css.getBytes());
        }
    }

    public void appendLessResource(String pathInfo) {
        try (InputStream in = this.getClass().getResourceAsStream(pathInfo)) {
            checkNotNull(in, "Could not find resource: " + pathInfo);
            try (StringWriter writer = new StringWriter()) {
                IOUtils.copy(in, writer, StandardCharsets.UTF_8);
                String inputLess = writer.toString();
                String output = lessCompiler().compile(inputLess);
                appendCss(output);
            }
        } catch (Exception ex) {
            throw new OpenLumifyException("Could not append less resource: " + pathInfo, ex);
        }
    }

    public void appendCssResource(String pathInfo) {
        try (InputStream in = this.getClass().getResourceAsStream(pathInfo)) {
            checkNotNull(in, "Could not find resource: " + pathInfo);
            appendCss(IOUtils.toString(in));
        } catch (IOException ex) {
            throw new OpenLumifyException("Could not append css resource: " + pathInfo, ex);
        }
    }

    private void appendCss(String output) {
        css += output + "\n";
    }

    private synchronized LessEngine lessCompiler() {
        if (lessCompiler == null) {
            LessOptions options = new LessOptions();
            options.setCompress(true);
            options.setCharset("UTF-8");
            lessCompiler = new LessEngine(options);
        }
        return lessCompiler;
    }
}
