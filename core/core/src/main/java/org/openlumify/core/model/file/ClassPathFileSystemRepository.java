package org.openlumify.core.model.file;

import org.apache.commons.io.IOUtils;
import org.openlumify.core.exception.OpenLumifyException;

import java.io.*;

public class ClassPathFileSystemRepository extends FileSystemRepository {
    private final String prefix;

    public ClassPathFileSystemRepository(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public File getLocalFileFor(String path) {
        InputStream in = getInputStream(path);
        File tempFile = new File(System.getProperty("java.io.tmpdir"), new File(path).getName());
        try (OutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        } catch (IOException ex) {
            throw new OpenLumifyException("Could not copy file: " + path + " to " + tempFile, ex);
        }
        return tempFile;
    }

    @Override
    public InputStream getInputStream(String path) {
        String fullPath = prefix + path;
        InputStream in = getClass().getResourceAsStream(fullPath);
        if (in == null) {
            throw new OpenLumifyException("Could not find classpath file: " + fullPath);
        }
        return in;
    }

    @Override
    public Iterable<String> list(String path) {
        throw new OpenLumifyException("Unsupported operation");
    }
}
