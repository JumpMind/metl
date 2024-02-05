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
package org.jumpmind.metl.ui.mapping;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.html.Div;

public class ConnectionEvent extends ComponentEvent<Div> {

    private static final long serialVersionUID = 1L;

    String sourceId;

    String targetId;

    boolean removed;

    public ConnectionEvent(Div source, String sourceId, String targetId, boolean removed) {
        super(source, false);
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.removed = removed;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public boolean isRemoved() {
        return removed;
    }

}
