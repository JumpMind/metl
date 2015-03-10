package org.jumpmind.symmetric.is.ui.common;


public interface IBackgroundRefreshable {
    
    public <T> T onBackgroundDataRefresh();
    public <T> void onBackgroundUIRefresh(T backgroundData);
        
}
