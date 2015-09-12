package org.jumpmind.metl.ui.init;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.jumpmind.metl.ui.common.IBackgroundRefreshable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
@Scope(value="ui")
public class BackgroundRefresherService implements Serializable {

    private static final long serialVersionUID = 1L;

    final protected Logger log = LoggerFactory.getLogger(getClass());

    protected Future<?> future;

    protected transient ThreadPoolTaskScheduler taskScheduler;
    
    protected AppUI appUi;

    protected Set<IBackgroundRefreshable> currentlyRefreshing = Collections
            .synchronizedSet(new HashSet<IBackgroundRefreshable>());

    protected void init(AppUI ui) {
        this.appUi = ui;
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setThreadNamePrefix("ui-refresher-");
        this.taskScheduler.setPoolSize(1);
        this.taskScheduler.setDaemon(true);
        this.taskScheduler.initialize();   
        setPollingInterval(5000);
    }

    protected void setPollingInterval(int interval) {
        if (future != null) {
            future.cancel(false);
        }
        this.future = this.taskScheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                synchronized (currentlyRefreshing) {
                    for (final IBackgroundRefreshable refreshing : currentlyRefreshing) {
                        try {
                            log.debug("refreshing background data " + refreshing.getClass().getSimpleName());
                            final Object data = refreshing.onBackgroundDataRefresh();
                            appUi.access(new Runnable() {
                                @Override
                                public void run() {
                                    refreshing.onBackgroundUIRefresh(data);
                                }
                            });
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }

                    }
                }
            }
        }, interval);
    }    

    public void register(IBackgroundRefreshable refreshing) {
        log.debug("registered background refresher " + refreshing.getClass().getSimpleName());
        currentlyRefreshing.add(refreshing);
    }

    public void unregister(IBackgroundRefreshable refreshing) {
        log.debug("unregistered background refresher " + refreshing.getClass().getSimpleName());
        currentlyRefreshing.remove(refreshing);
    }

    public void unregisterAll() {
        currentlyRefreshing.clear();
    }

    protected void destroy() {
        log.debug("The background refresher service is shutting down");

        if (taskScheduler != null) {
            taskScheduler.shutdown();
            taskScheduler = null;
        }

    }

}
