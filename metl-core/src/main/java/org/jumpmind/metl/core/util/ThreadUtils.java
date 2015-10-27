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
