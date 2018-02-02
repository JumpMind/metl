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
package org.jumpmind.metl.core.runtime.component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.BinaryMessage;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IDirectory;

public class BinaryFileWriter extends AbstractFileWriter {

    public static final String TYPE = "Binary File Writer";

    IDirectory streamable;
    
    @Override
    public void start() {
        init();
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {

        streamable = (IDirectory) getResourceReference();
        if (inputMessage instanceof BinaryMessage) {
            BinaryMessage message = (BinaryMessage) inputMessage;
            String fileName = getFileName(inputMessage);
            if (!append) {
                streamable.delete(fileName, false);
            }

            OutputStream fos = streamable.getOutputStream(fileName, mustExist, false, false);

            try {
                fos.write(message.getPayload());
            } catch (IOException e) {
                throw new IoException(e);
            } finally {
                IOUtils.closeQuietly(fos);
            }
        }

        if ((inputMessage instanceof ControlMessage || unitOfWorkBoundaryReached) && callback != null) {
            ArrayList<String> results = new ArrayList<>();
            results.add("{\"status\":\"success\"}");
            callback.sendTextMessage(null, results);
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }
    
    @Override
    public void stop() {
        streamable.close();
    }

}
