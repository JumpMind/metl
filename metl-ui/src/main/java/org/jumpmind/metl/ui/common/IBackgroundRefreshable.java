package org.jumpmind.metl.ui.common;


public interface IBackgroundRefreshable {
    
    public <T> T onBackgroundDataRefresh();
    public <T> void onBackgroundUIRefresh(T backgroundData);
        
}
