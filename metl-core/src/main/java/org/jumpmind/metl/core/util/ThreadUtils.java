package org.jumpmind.metl.core.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final public class ThreadUtils {

    private ThreadUtils() {
    }

    public static int getThreadNumber() {
        int threadNumber = 1;
        String name = Thread.currentThread().getName();
        int lastIndex = name.lastIndexOf("-");
        if (lastIndex >= 0) {
            try {
                threadNumber = Integer.parseInt(name.substring(lastIndex + 1));
            } catch (NumberFormatException e) {
            }
        }
        return threadNumber;
    }

    public static ExecutorService createFixedThreadPool(String namePrefix, int threadCount) {
        return Executors.newFixedThreadPool(threadCount, new CustomThreadFactory(namePrefix));
    }

    public static ExecutorService createUnboundedThreadPool(String namePrefix) {
        return Executors.newCachedThreadPool(new CustomThreadFactory(namePrefix));
    }

    static class CustomThreadFactory implements ThreadFactory {

        String namePrefix;
        final AtomicInteger threadNumber = new AtomicInteger(1);

        public CustomThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(namePrefix + "-" + threadNumber.getAndIncrement());
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
