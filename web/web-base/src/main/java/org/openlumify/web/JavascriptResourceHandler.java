package org.openlumify.web;


import org.visallo.webster.HandlerChain;
import org.visallo.webster.RequestResponseHandler;
import org.apache.commons.io.IOUtils;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.visallo.web.closurecompiler.com.google.javascript.jscomp.*;
import org.visallo.web.closurecompiler.com.google.javascript.jscomp.Compiler;
import org.openlumify.web.util.js.CachedCompilation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;


public class JavascriptResourceHandler implements RequestResponseHandler {
    private static final int EXECUTOR_CONCURRENT = 3;
    private static final long EXECUTOR_IDLE_THREAD_RELEASE_SECONDS = 5;
    private static final ThreadPoolExecutor compilationExecutor = new ThreadPoolExecutor(
            EXECUTOR_CONCURRENT,
            EXECUTOR_CONCURRENT,
            EXECUTOR_IDLE_THREAD_RELEASE_SECONDS,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );

    static {
        compilationExecutor.allowCoreThreadTimeOut(true);
    }

    private String jsResourceName;
    private String jsResourcePath;
    private boolean enableSourceMaps;
    private String closureExternResourcePath;
    private Future<CachedCompilation> compilationTask;
    private volatile CachedCompilation previousCompilation;

    public JavascriptResourceHandler(final String jsResourceName, final String jsResourcePath, boolean enableSourceMaps) {
        this(jsResourceName, jsResourcePath, enableSourceMaps, null);
    }

    public JavascriptResourceHandler(final String jsResourceName, final String jsResourcePath, boolean enableSourceMaps, String closureExternResourcePath) {
        this.jsResourceName = jsResourceName;
        this.jsResourcePath = jsResourcePath;
        this.enableSourceMaps = enableSourceMaps;
        this.closureExternResourcePath = closureExternResourcePath;

        compilationTask = compilationExecutor.submit(() -> compileIfNecessary(null));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        CachedCompilation cache = getCache();

        if (request.getRequestURI().endsWith(".map")) {
            write(response, "application/json", cache.getSourceMap());
        } else if (request.getRequestURI().endsWith(".src")) {
            write(response, "application/javascript", cache.getInput());
        } else {
            if (this.enableSourceMaps && cache.getSourceMap() != null) {
                response.setHeader("X-SourceMap", request.getRequestURI() + ".map");
            }
            write(response, "application/javascript", cache.getOutput());
        }
    }

    private CachedCompilation getCache() throws IOException, InterruptedException, ExecutionException {
        CachedCompilation cache;

        if (compilationTask == null) {
            cache = compileIfNecessary(previousCompilation);
        } else if (compilationTask.isDone()) {
            previousCompilation = compilationTask.get();
            compilationTask = null;
            cache = compileIfNecessary(previousCompilation);
        } else {
            cache = compilationTask.get();
        }

        previousCompilation = cache;
        return cache;
    }

    private void write(HttpServletResponse response, String contentType, String output) throws IOException {
        if (output != null) {
            try (PrintWriter outWriter = response.getWriter()) {
                response.setContentType(contentType);
                outWriter.println(output);
            }
        } else {
            throw new OpenLumifyException("Errors during minify: " + jsResourceName);
        }
    }


    private CachedCompilation compileIfNecessary(CachedCompilation previousCompilation) throws IOException {
        URL url = this.getClass().getResource(jsResourceName);
        if (url == null) {
            throw new OpenLumifyException("Could not find resource: " + jsResourceName);
        }
        long lastModified = url.openConnection().getLastModified();

        if (previousCompilation == null || previousCompilation.isNecessary(lastModified)) {
            CachedCompilation newCache = new CachedCompilation();
            newCache.setLastModified(lastModified);
            try (InputStream in = this.getClass().getResourceAsStream(jsResourceName)) {
                checkNotNull(in, "Could not find resource: " + jsResourceName);
                try (StringWriter writer = new StringWriter()) {
                    IOUtils.copy(in, writer, StandardCharsets.UTF_8);
                    String inputJavascript = writer.toString();
                    newCache.setInput(inputJavascript);

                    runClosureCompilation(newCache);
                }
            }
            return newCache;
        }

        return previousCompilation;
    }

    private CachedCompilation runClosureCompilation(CachedCompilation cachedCompilation) throws IOException {
        Compiler.setLoggingLevel(Level.INFO);
        Compiler compiler = new Compiler(new JavascriptResourceHandlerErrorManager());

        CompilerOptions compilerOptions = new CompilerOptions();
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(compilerOptions);
        WarningLevel.VERBOSE.setOptionsForWarningLevel(compilerOptions);
        compilerOptions.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT6);
        compilerOptions.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT5);
        compilerOptions.setEnvironment(CompilerOptions.Environment.BROWSER);
        compilerOptions.setSourceMapOutputPath("");
        compilerOptions.setSourceMapFormat(SourceMap.Format.V3);
        compilerOptions.setSourceMapDetailLevel(SourceMap.DetailLevel.ALL);

        List<SourceFile> inputs = new ArrayList<>();
        inputs.add(SourceFile.fromCode(jsResourcePath + ".src", cachedCompilation.getInput()));

        List<SourceFile> externs = AbstractCommandLineRunner.getBuiltinExterns(compilerOptions);
        InputStream openlumifyExterns = JavascriptResourceHandler.class.getResourceAsStream("openlumify-externs.js");
        externs.add(SourceFile.fromInputStream("openlumify-externs.js", openlumifyExterns, Charset.forName("UTF-8")));

        if (closureExternResourcePath != null) {
            externs.add(SourceFile.fromInputStream(
                    closureExternResourcePath,
                    this.getClass().getResourceAsStream(closureExternResourcePath),
                    Charset.forName("UTF-8")
            ));
        }

        Result result = compiler.compile(externs, inputs, compilerOptions);
        if (result.success) {
            cachedCompilation.setOutput(compiler.toSource());

            if (enableSourceMaps) {
                StringBuilder sb = new StringBuilder();
                result.sourceMap.appendTo(sb, jsResourcePath);
                cachedCompilation.setSourceMap(sb.toString());
            }
        }
        return cachedCompilation;
    }

    private static class JavascriptResourceHandlerErrorManager extends BasicErrorManager {
        private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(JavascriptResourceHandlerErrorManager.class);

        @Override
        public void println(CheckLevel checkLevel, JSError jsError) {
            if (checkLevel.equals(CheckLevel.ERROR)) {
                LOGGER.error("%s:%s %s", jsError.sourceName, jsError.getLineNumber(), jsError.description);
            }
        }

        @Override
        protected void printSummary() {
            if (this.getErrorCount() > 0) {
                LOGGER.error("%d error(s)", this.getErrorCount());
            }
        }
    }

}

