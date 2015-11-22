package org.jumpmind.metl.core.runtime.resource;

public class FileInfo {

    String relativePath;
    boolean directory;
    long lastUpdated;
    
    public FileInfo(String path, boolean directory, long lastUpdated) {
        this.relativePath = path;
        this.directory = directory;
        this.lastUpdated = lastUpdated;
    }
    
    public String getRelativePath() {
        return relativePath;
    }
    
    public boolean isDirectory() {
        return directory;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }

}
