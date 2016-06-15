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
package org.jumpmind.metl.ui.diagram;

import java.util.List;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.ui.AbstractJavaScriptComponent;

import elemental.json.JsonArray;
import elemental.json.JsonObject;


/**
 * TODO: ADB: Refactor the RunDiagram screen to reuse the Diagram screen.
 *
 */
@JavaScript({ "jquery-2.2.0.min.js", "dom.jsPlumb-1.7.5-min.js", "run-diagram.js" })
@StyleSheet({ "run-diagram.css" })
public class RunDiagram extends AbstractJavaScriptComponent {

    private static final long serialVersionUID = 1L;

    public RunDiagram() {
        setPrimaryStyleName("diagram");
        setId("run-diagram");

        addFunction("onNodeSelected", (arguments) -> {
            DiagramState state = getState();
            List<String> ids = state.selectedNodeIds;
            ids.clear();
            if (arguments.length() > 0) {
                Object obj = arguments.get(0);
                if (obj instanceof JsonObject) {
                    JsonObject json = arguments.getObject(0);
                    if (json.hasKey("nodes")) {
                        JsonArray nodes = json.getArray("nodes");
                        for (int i = 0; i < nodes.length(); i++) {
                            ids.add(nodes.getObject(i).getString("id"));
                        }
                    }
                }
            }
            fireEvent(new NodeSelectedEvent(RunDiagram.this, ids));
        });

    }

    public void setSelectedNodeIds(List<String> ids) {
        getState().selectedNodeIds = ids;
    }

    public List<String> getSelectedNodeIds() {
        return getState().selectedNodeIds;
    }

    public void setNodes(List<Node> nodes) {
        getState().nodes = nodes;
    }

    public void addNode(Node node) {
        getState().nodes.add(node);
    }

    @Override
    protected DiagramState getState() {
        return (DiagramState) super.getState();
    }

    public List<Node> getNodes() {
        return getState().nodes;
    }
}
