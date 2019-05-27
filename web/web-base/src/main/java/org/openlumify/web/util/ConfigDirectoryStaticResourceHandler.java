package org.openlumify.web.util;

import org.openlumify.webster.HandlerChain;
import org.openlumify.webster.RequestResponseHandler;
import org.apache.commons.io.IOUtils;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.file.FileSystemRepository;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

public class ConfigDirectoryStaticResourceHandler implements RequestResponseHandler {
    private final String relativePath;
    private final String contentType;
    private final FileSystemRepository fileSystemRepository;

    public static ConfigDirectoryStaticResourceHandler create(String relativePath, String contentType) {
        FileSystemRepository fileSystemRepository = InjectHelper.getInstance(FileSystemRepository.class);
        return new ConfigDirectoryStaticResourceHandler(relativePath, contentType, fileSystemRepository);
    }

    public ConfigDirectoryStaticResourceHandler(
            String relativePath,
            String contentType,
            FileSystemRepository fileSystemRepository
    ) {
        this.relativePath = relativePath;
        this.contentType = contentType;
        this.fileSystemRepository = fileSystemRepository;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        try (InputStream in = openInputStream()) {
            if (in == null) {
                throw new IOException("Could not find file at: " + this.relativePath);
            }
            try (ServletOutputStream out = response.getOutputStream()) {
                response.setContentType(this.contentType);
                IOUtils.copy(in, out);
            }
        }
    }

    private InputStream openInputStream() {
        try {
            return fileSystemRepository.getInputStream(relativePath);
        } catch (Exception ex) {
            throw new OpenLumifyException("Could not open input stream for file: " + relativePath);
        }
    }
}
