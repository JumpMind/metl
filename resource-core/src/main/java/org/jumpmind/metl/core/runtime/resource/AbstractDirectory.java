package org.jumpmind.metl.core.runtime.resource;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

abstract public class AbstractDirectory implements IDirectory {

    public AbstractDirectory() {
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
        return false;
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist) {
        return null;
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist, boolean closeSession) {
        return null;
    }

    @Override
    public boolean supportsOutputStream() {
        return false;
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist) {
        return null;
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist, boolean closeSession, boolean append) {
        return null;
    }
    
    @Override
    public void close(boolean success) {
    }

    @Override
    public void close() {
        close(true);
    }

    @Override
    public boolean delete(String relativePath) {
        return false;
    }

    @Override
    public boolean delete(String relativePath, boolean closeSession) {
        return false;
    }

    @Override
    public boolean supportsDelete() {
        return false;
    }

    @Override
    public List<FileInfo> listFiles(String... relativePaths) {
        return null;
    }

    @Override
    public List<FileInfo> listFiles(boolean closeSession, String... relativePaths) {
        return null;
    }

    @Override
    public FileInfo listFile(String relativePath) {
        return null;
    }

    @Override
    public FileInfo listFile(String relativePath, boolean closeSession) {
        return null;
    }

    @Override
    public void copyFile(String fromFilePath, String toFilePath) {
    }

    @Override
    public void copyFile(String fromFilePath, String toFilePath, boolean closeSession) {
    }

    @Override
    public void moveFile(String fromFilePath, String toFilePath) {
    }

    @Override
    public void moveFile(String fromFilePath, String toFilePath, boolean closeSession) {
    }

    @Override
    public void copyToDir(String fromFilePath, String toDirPath) {
    }

    @Override
    public void copyToDir(String fromFilePath, String toDirPath, boolean closeSession) {
    }

    @Override
    public void moveToDir(String fromFilePath, String toDirPath) {
    }

    @Override
    public void moveToDir(String fromFilePath, String toDirPath, boolean closeSession) {
    }

    @Override
    public boolean renameFile(String fromFilePath, String toFilePath) {
        return false;
    }

    @Override
    public boolean renameFile(String fromFilePath, String toFilePath, boolean closeSession) {
        return false;
    }

    @Override
    public void connect() {
    }

}
