package org.jumpmind.metl.core.runtime.resource;

public class FileInfo {

    String path;
    boolean directory;
    
    public FileInfo(String path, boolean directory) {
        this.path = path;
        this.directory = directory;
    }
    
    public String getPath() {
        return path;
    }
    
    public boolean isDirectory() {
        return directory;
    }

}
