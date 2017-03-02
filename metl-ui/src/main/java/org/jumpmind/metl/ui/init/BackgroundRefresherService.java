/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.ui.init;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
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
@Scope(value = "ui")
public class BackgroundRefresherService implements Serializable {

    private static final long serialVersionUID = 1L;

    final protected Logger log = LoggerFactory.getLogger(getClass());

    protected transient Future<?> future;

    protected transient ThreadPoolTaskScheduler taskScheduler;

    protected AppUI appUi;

    protected Set<IBackgroundRefreshable<Object>> currentlyRefreshing = Collections.synchronizedSet(new HashSet<IBackgroundRefreshable<Object>>());

    protected void init(AppUI ui) {
        this.appUi = ui;
        initBackgroundThread();
    }

    private void initBackgroundThread() {
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setThreadNamePrefix("ui-refresher-");
        this.taskScheduler.setPoolSize(1);
        this.taskScheduler.setDaemon(true);
        this.taskScheduler.initialize();
        setPollingInterval(2500);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initBackgroundThread();
    }

    protected void setPollingInterval(int interval) {
        if (future != null) {
            future.cancel(false);
        }
        this.future = this.taskScheduler.scheduleWithFixedDelay(() -> refresh(), new Date(), interval);
    }

    public void doWork(IBackgroundRefreshable<Object> work) {
        taskScheduler.schedule(() -> run(work), new Date());
    }
    
    protected void refresh() {
        synchronized (currentlyRefreshing) {
            if (appUi.isAttached()) {
                for (final IBackgroundRefreshable<Object> refreshing : currentlyRefreshing) {
                    if (refreshing != null) {                        
                        run(refreshing);
                    } 
                }
            }
        }
    }

    public void register(IBackgroundRefreshable<Object> refreshing) {
        log.debug("registered background refresher " + refreshing.getClass().getSimpleName());
        currentlyRefreshing.add(refreshing);
    }

    public void unregister(IBackgroundRefreshable<Object> refreshing) {
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
    
    protected void run(IBackgroundRefreshable<Object> refreshing) {
        try {
            log.debug("refreshing background data " + refreshing.getClass().getSimpleName());
            final Object data = refreshing.onBackgroundDataRefresh();
            if (data != null) {
                appUi.access(() -> refreshing.onBackgroundUIRefresh(data));
            }
        } catch (Exception e) {
            appUi.access(() -> refreshing.onUIError(e));                            
            log.error(e.getMessage(), e);
        }
    }
}
