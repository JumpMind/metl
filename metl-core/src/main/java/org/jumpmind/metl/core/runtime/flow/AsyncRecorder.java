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
package org.jumpmind.metl.core.runtime.flow;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.ExecutionStepLog;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.symmetric.csv.CsvWriter;
import org.jumpmind.util.AppUtils;
import org.jumpmind.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncRecorder implements Runnable {

    final Logger log = LoggerFactory.getLogger(getClass());

    protected BlockingQueue<AbstractObject> inQueue;

    protected IExecutionService executionService;

    protected boolean running = false;

    protected boolean stopping = false;

    protected Map<String, CsvWriter> logWriters = new HashMap<>();

    public AsyncRecorder(IExecutionService executionService) {
        this.inQueue = new LinkedBlockingQueue<AbstractObject>();
        this.executionService = executionService;
    }

    public void record(AbstractObject object) {
        try {
            synchronized (inQueue) {
                inQueue.put(object);
                inQueue.notify();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void logAll() {
        try {
            HashSet<String> processed = new HashSet<>();
            List<AbstractObject> toProcess = new ArrayList<>();
            synchronized (inQueue) {
                inQueue.drainTo(toProcess);
                if (toProcess.isEmpty()) {
                    inQueue.wait();
                }
            }
            
            for (int i = toProcess.size() - 1; i >= 0; i--) {
                AbstractObject object = toProcess.get(i);
                if (object != null && !(object instanceof ExecutionStepLog)) {
                    if (!processed.contains(object.getId())) {
                        executionService.save(object);
                        processed.add(object.getId());
                    }
                }
            }

            for (int i = 0; i < toProcess.size(); i++) {
                AbstractObject object = toProcess.get(i);
                if (object instanceof ExecutionStepLog) {
                    ExecutionStepLog stepLog = (ExecutionStepLog) object;
                    String executionStepId = stepLog.getExecutionStepId();
                    CsvWriter writer = logWriters.get(executionStepId);
                    if (writer == null) {
                        File logFile = new File(LogUtils.getLogDir(), executionStepId + ".log");
                        writer = new CsvWriter(logFile.getAbsolutePath(), '"',
                                Charset.forName("UTF-8"));
                        logWriters.put(executionStepId, writer);
                    }
                    try {
                        writer.writeRecord(new String[] { stepLog.getLevel(),
                                FormatUtils.TIMESTAMP_FORMATTER.format(stepLog.getCreateTime()),
                                StringUtils.abbreviate(stepLog.getLogText(), 100000) });
                        writer.flush();
                    } catch (IOException e) {
                        writer.close();
                        logWriters.remove(executionStepId);
                        log.error("", e);
                    }
                }
            }

            if (toProcess.size() == 0) {
                AppUtils.sleep(5);
            }

        } catch (Throwable e) {
            log.error("Failed to persist log message", e);
        }
    }

    @Override
    public void run() {
        running = true;

        try {
            while (!stopping || inQueue.size() > 0) {
                try {
                    logAll();
                } catch (Throwable e) {
                    log.error("Failed to persist log message", e);
                }
            }

        } finally {
            running = false;
        }
    }

    public void shutdown() {
        this.stopping = true;

        while (this.running) {
            AppUtils.sleep(10);
        }

        Collection<CsvWriter> writers = logWriters.values();
        for (CsvWriter csvWriter : writers) {
            csvWriter.close();
        }
    }

}
