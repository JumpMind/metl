package org.jumpmind.symmetric.is.ui;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Broadcaster implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Logger log = LoggerFactory.getLogger(Broadcaster.class);

    static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface BroadcastListener {
        void receiveBroadcast(String type, Object message);
    }

    private static Map<String, LinkedList<BroadcastListener>> listeners = new HashMap<String, LinkedList<BroadcastListener>>();

    public static synchronized void destroy() {
        executorService.shutdown();    
    }
    
    public static synchronized void register(String type, BroadcastListener listener) {

        LinkedList<BroadcastListener> list = listeners.get(type);
        if (list == null) {
            list = new LinkedList<Broadcaster.BroadcastListener>();
            listeners.put(type, list);
        }
        if (!list.contains(listener)) {
            list.add(listener);
            log.info("registered broadcast listener: " + listener);
        }
    }

    public static synchronized void unregister(String type, BroadcastListener listener) {
        LinkedList<BroadcastListener> list = listeners.get(type);
        if (list != null) {
            if (list.remove(listener)) {
                log.info("unregistered broadcast listener: " + listener);
            }
        }
    }

    public static synchronized void broadcast(final String type, final Object message) {
        log.info("Broadcasting: " + message + ", of type: " + type);
        LinkedList<BroadcastListener> list = listeners.get(type);
        if (list != null) {
            for (final BroadcastListener listener : list) {
                log.info("Queuing broadcast message: " + message + ", of type: " + type
                        + ", to listener: " + listener);
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        log.info("Broadcasting: " + message + ", of type: " + type
                                + ", to listener: " + listener);
                        listener.receiveBroadcast(type, message);
                    }
                });
            }
        }
    }
}