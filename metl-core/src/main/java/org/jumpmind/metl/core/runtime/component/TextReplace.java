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

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class TextReplace extends AbstractComponentRuntime {

    public static final String TYPE = "Text Replace";

    public final static String SETTING_SEARCH_FOR = "search.for";

    public final static String SETTING_REPLACE_WITH = "replace.with";

    String searchFor;

    String replaceWith;

    @Override
    public void start() {
        Component component = getComponent();
        searchFor = component.get(SETTING_SEARCH_FOR, "");
        replaceWith = component.get(SETTING_REPLACE_WITH, "");
        if (isBlank(searchFor)) {
            throw new IllegalStateException("Requires a 'Search For' expression");
        }
        if (replaceWith == null) {
            replaceWith = "";
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof TextMessage) {
            List<String> in = ((TextMessage) inputMessage).getPayload();
            ArrayList<String> out = new ArrayList<String>();
            if (in != null) {
                for (String string : in) {
                    getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                    out.add(string.replaceAll(searchFor, replaceWith));
                }
            }

            callback.sendTextMessage(null, out);
        } else {
            callback.forward(inputMessage);
        }
    }

}
