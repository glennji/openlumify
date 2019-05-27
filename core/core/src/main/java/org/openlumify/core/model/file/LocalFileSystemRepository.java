package org.openlumify.core.model.file;

import com.google.common.collect.Lists;
import org.openlumify.core.config.FileConfigurationLoader;
import org.openlumify.core.exception.OpenLumifyException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class LocalFileSystemRepository extends FileSystemRepository {
    @Override
    public File getLocalFileFor(String path) {
        File result = FileConfigurationLoader.resolveLocalFileName(path);
        if (result.exists()) {
            return result;
        }
        throw new OpenLumifyException("Could not find file: " + path);
    }

    @Override
    public InputStream getInputStream(String path) {
        try {
            return new FileInputStream(getLocalFileFor(path));
        } catch (FileNotFoundException ex) {
            throw new OpenLumifyException("Could not find file: " + path, ex);
        }
    }

    @Override
    public Iterable<String> list(String path) {
        return Lists.newArrayList(getLocalFileFor(path).list());
    }
}
