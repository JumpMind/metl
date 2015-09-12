package org.jumpmind.symmetric.is.core.runtime.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.symmetric.is.core.model.Resource;

public class LocalFileStreamable implements IStreamable {

    String basePath;

    public LocalFileStreamable(Resource resource, String basePath, boolean mustExist) {
        this.basePath = basePath;
    }

    @Override
    public boolean requiresContentLength() {
        return false;
    }

    @Override
    public void setContentLength(int length) {
    }

    @Override
    public boolean supportsInputStream() {
        return true;
    }

    protected File toFile(String relativePath, boolean mustExist) {
        File file = new File(basePath, relativePath);
        if (!file.exists()) {
            if (!mustExist) {
                file.getParentFile().mkdirs();
            } else {
                throw new IoException("Could not find " + file.getAbsolutePath());
            }
        }
        return file;
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist) {
        try {
            return new FileInputStream(toFile(relativePath, mustExist));
        } catch (FileNotFoundException e) {
            throw new IoException(e);
        }
    }

    @Override
    public boolean supportsOutputStream() {
        return true;
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist) {
        try {
            return new FileOutputStream(toFile(relativePath, mustExist));
        } catch (FileNotFoundException e) {
            throw new IoException(e);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean delete(String relativePath) {
        return FileUtils.deleteQuietly(toFile(relativePath, false));
    }

    @Override
    public boolean supportsDelete() {
        return true;
    }

    @Override
    public String toString() {
        return basePath;
    }
}
