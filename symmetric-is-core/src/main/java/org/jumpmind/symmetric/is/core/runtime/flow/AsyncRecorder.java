package org.jumpmind.symmetric.is.core.runtime.flow;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.persist.IExecutionService;
import org.jumpmind.util.AppUtils;

public class AsyncRecorder implements Runnable {

    protected BlockingQueue<AbstractObject> inQueue;

    protected IExecutionService executionService;

    protected boolean running = false;
    
    protected boolean stopping = false;

    public AsyncRecorder(IExecutionService executionService) {
        this.inQueue = new LinkedBlockingQueue<AbstractObject>();
        this.executionService = executionService;
    }

    public void record(AbstractObject object) {
        try {
            inQueue.put(object);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        running = true;

        while (!stopping || inQueue.size() > 0) {
            try {
                AbstractObject object = inQueue.poll(5, TimeUnit.SECONDS);
                if (object != null) {
                    executionService.save(object);
                }
            } catch (InterruptedException e) {
            }
        }

        running = false;
    }

    
    public void shutdown() {
        this.stopping = true;
        
        while (this.running) {
            AppUtils.sleep(10);
        }
    }
    

}
