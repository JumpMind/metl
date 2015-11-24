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
    
    public String getName() {
        int index = relativePath.lastIndexOf("/");
        if (index > 0) {
            return relativePath.substring(index + 1, relativePath.length());
        } else {
            return relativePath;
        }
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
