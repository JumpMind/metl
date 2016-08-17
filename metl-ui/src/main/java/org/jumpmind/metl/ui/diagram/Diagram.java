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
import com.vaadin.server.Page;
import com.vaadin.ui.AbstractJavaScriptComponent;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

@JavaScript({ "jquery-2.2.0.min.js", "dom.jsPlumb-1.7.5-min.js", "diagram.js", "html2canvas.js", "canvg.js"})
@StyleSheet({ "diagram.css" })
public class Diagram extends AbstractJavaScriptComponent {

    private static final long serialVersionUID = 1L;

    public Diagram(boolean readOnly) {
        setPrimaryStyleName("diagram");
        setId("diagram");
        getState().readOnly = readOnly;

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
            fireEvent(new NodeSelectedEvent(Diagram.this, ids));
        });
        
        addFunction("onLinkSelected", (arguments) -> {
            if (arguments.length() > 0) {
                Object obj = arguments.get(0);
                if (obj instanceof JsonObject) {
                    JsonObject json = arguments.getObject(0);
                    String sourceNodeId = json.getString("sourceNodeId");
                    String targetNodeId = json.getString("targetNodeId");
                    fireEvent(new LinkSelectedEvent(Diagram.this, sourceNodeId, targetNodeId));
                }
            }
        });        

        addFunction("onNodeDoubleClick", (arguments) -> {
            if (arguments.length() > 0) {
                Object obj = arguments.get(0);
                if (obj instanceof JsonObject) {
                    JsonObject json = arguments.getObject(0);
                    String id = json.getString("id");
                    DiagramState state = getState();
                    for (Node node : state.nodes) {
                        if (node.getId().equals(id)) {
                            fireEvent(new NodeDoubleClickedEvent(Diagram.this, node));
                            break;
                        }
                    }
                }
            }
        });

        addFunction("onNodeMoved", (arguments) -> {
            if (arguments.length() > 0) {
                Object obj = arguments.get(0);
                if (obj instanceof JsonObject) {
                    JsonObject json = arguments.getObject(0);
                    String id = json.getString("id");
                    double x = json.getNumber("x");
                    double y = json.getNumber("y");
                    DiagramState state = getState();
                    for (Node node : state.nodes) {
                        if (node.getId().equals(id)) {
                            // Why do we set the state on the server side?
                            // Don't we have to send this back to the client
                            // now?
                            node.setX((int) x);
                            node.setY((int) y);
                            fireEvent(new NodeMovedEvent(Diagram.this, node));
                            break;
                        }
                    }
                }
            }
        });

        addFunction("onConnection", (arguments) -> {
            if (arguments.length() > 0) {
                Object obj = arguments.get(0);
                if (obj instanceof JsonObject) {
                    JsonObject json = arguments.getObject(0);
                    String sourceNodeId = json.getString("sourceNodeId");
                    String targetNodeId = json.getString("targetNodeId");
                    boolean removed = json.getBoolean("removed");
                    DiagramState state = getState();
                    for (Node node : state.nodes) {
                        if (node.getId().equals(sourceNodeId)) {
                            if (!removed && !node.getTargetNodeIds().contains(targetNodeId)) {
                                node.getTargetNodeIds().add(targetNodeId);
                            } else if (removed) {
                                node.getTargetNodeIds().remove(targetNodeId);
                            }
                            fireEvent(new LinkEvent(Diagram.this, sourceNodeId, targetNodeId, removed));
                            break;
                        }
                    }
                }
            }
        });

        addFunction("onConnectionMoved", (arguments) -> {
            if (arguments.length() > 0) {
                Object obj = arguments.get(0);
                if (obj instanceof JsonObject) {
                    JsonObject json = arguments.getObject(0);
                    String sourceNodeId = json.getString("sourceNodeId");
                    String targetNodeId = json.getString("targetNodeId");
                    String origSourceNodeId = json.getString("origSourceNodeId");
                    String origTargetNodeId = json.getString("origTargetNodeId");

                    DiagramState state = getState();
                    for (Node node : state.nodes) {
                        if (node.getId().equals(sourceNodeId)) {
                            if (!node.getTargetNodeIds().contains(targetNodeId)) {
                                node.getTargetNodeIds().add(targetNodeId);
                            }
                            fireEvent(new LinkEvent(Diagram.this, sourceNodeId, targetNodeId, false));
                            break;
                        }
                    }

                    for (Node node : state.nodes) {
                        if (node.getId().equals(origSourceNodeId)) {
                            node.getTargetNodeIds().remove(origTargetNodeId);
                            fireEvent(new LinkEvent(Diagram.this, origSourceNodeId, origTargetNodeId, true));
                            break;
                        }
                    }

                }
            }
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
    
    public void export() {
        // Lookup how large the canvas needs to be based on node positions.
        int maxHeight = 0;
        int maxWidth = 0;
        for (Node node : getState().nodes) {
            if (node.getX() > maxWidth) {
                maxWidth = node.getX();
            }
            if (node.getY() > maxHeight) {
                maxHeight = node.getY();
            }
        }
        // Pad Boundary to include text and margins.
        maxWidth += 200;
        maxHeight += 200;
        // Call client side code to create the canvas and display it.
        Page.getCurrent().getJavaScript().execute("exportDiagram("+maxWidth+","+maxHeight+");");
    }
    
}
