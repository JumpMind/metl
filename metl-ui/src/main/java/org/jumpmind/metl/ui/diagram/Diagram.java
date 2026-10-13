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
import java.util.UUID;

import org.jumpmind.metl.ui.views.design.EditFlowPanel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.Page;

import tools.jackson.databind.JsonNode;

@JsModule("./html2canvas.js")
@JsModule("./canvg.js")
@CssImport("./diagram.css")
@JsModule("./jquery-3.7.1.min.js")
@JsModule("./jsplumb.min.js")
@JsModule("./diagram.js")
@JavaScript("./diagram.js")
@CssImport("./mapping-diagram.css")
@JsModule("./jsplumb.min.js")
@JsModule("./mapping-diagram.js")
@JavaScript("./mapping-diagram.js")
public class Diagram extends Div {

    private static final long serialVersionUID = 1L;
    
    private DiagramDetail diagramDetail;
    
    private EditFlowPanel panel;

    public Diagram(EditFlowPanel panel) {
        addClassName("diagram");
        setId("diagram-" + UUID.randomUUID());
        diagramDetail = new DiagramDetail();
        this.panel = panel;
    }
    
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (attachEvent.isInitialAttach()) {
            Page page = UI.getCurrent().getPage();
            page.executeJs("window.org_jumpmind_metl_ui_diagram_Diagram($0)", getElement());
        }
    }

    @ClientCallable
    private void onNodeSelected(JsonNode json) {
        List<String> ids = diagramDetail.getSelectedNodeIds();
        ids.clear();
        if (json.has("nodes")) {
            JsonNode nodes = json.get("nodes");
            for (int i = 0; i < nodes.size(); i++) {
                ids.add(nodes.get(i).get("id").asString());
            }
        }
        panel.nodeSelectedEvent(new NodeSelectedEvent(Diagram.this, ids));
    }
    
    @ClientCallable
    private void onLinkSelected(JsonNode json) {
        String sourceNodeId = json.get("sourceNodeId").asString();
        String targetNodeId = json.get("targetNodeId").asString();
        panel.linkSelectedEvent(new LinkSelectedEvent(Diagram.this, sourceNodeId, targetNodeId));
    }

    @ClientCallable
    private void onNodeDoubleClick(JsonNode json) {
        String id = json.get("id").asString();
        for (Node node : diagramDetail.getNodes()) {
            if (node.getId().equals(id)) {
                panel.nodeDoubleClickedEvent(new NodeDoubleClickedEvent(Diagram.this, node));
                break;
            }
        }
    }
    
    @ClientCallable
    private void onNodeMoved(JsonNode json) {
        String id = json.get("id").asString();
        double x = json.get("x").asDouble();
        double y = json.get("y").asDouble();
        for (Node node : diagramDetail.getNodes()) {
            if (node.getId().equals(id)) {
                // Why do we set the state on the server side?
                // Don't we have to send this back to the client
                // now?
                node.setX((int) x);
                node.setY((int) y);
                panel.nodeMovedEvent(new NodeMovedEvent(Diagram.this, node));
                break;
            }
        }
    }
    
    @ClientCallable
    private void onConnection(JsonNode json) {
        String sourceNodeId = json.get("sourceNodeId").asString();
        String targetNodeId = json.get("targetNodeId").asString();
        boolean removed = json.get("removed").asBoolean();
        for (Node node : diagramDetail.getNodes()) {
            if (node.getId().equals(sourceNodeId)) {
                if (!removed && !node.getTargetNodeIds().contains(targetNodeId)) {
                    node.getTargetNodeIds().add(targetNodeId);
                } else if (removed) {
                    node.getTargetNodeIds().remove(targetNodeId);
                }
                panel.linkEvent(new LinkEvent(Diagram.this, sourceNodeId, targetNodeId, removed));
                break;
            }
        }
    }
    
    @ClientCallable
    private void onConnectionMoved(JsonNode json) {
        String sourceNodeId = json.get("sourceNodeId").asString();
        String targetNodeId = json.get("targetNodeId").asString();
        String origSourceNodeId = json.get("origSourceNodeId").asString();
        String origTargetNodeId = json.get("origTargetNodeId").asString();

        for (Node node : diagramDetail.getNodes()) {
            if (node.getId().equals(sourceNodeId)) {
                if (!node.getTargetNodeIds().contains(targetNodeId)) {
                    node.getTargetNodeIds().add(targetNodeId);
                }
                panel.linkEvent(new LinkEvent(Diagram.this, sourceNodeId, targetNodeId, false));
                break;
            }
        }

        for (Node node : diagramDetail.getNodes()) {
            if (node.getId().equals(origSourceNodeId)) {
                node.getTargetNodeIds().remove(origTargetNodeId);
                panel.linkEvent(new LinkEvent(Diagram.this, origSourceNodeId, origTargetNodeId, true));
                break;
            }
        }
    }
    
    @ClientCallable
    public String getCurState() {
        ObjectMapper om = new ObjectMapper();
        try {
            return om.writeValueAsString(diagramDetail);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public void setSelectedNodeIds(List<String> ids) {
        diagramDetail.setSelectedNodeIds(ids);
    }

    public List<String> getSelectedNodeIds() {
        return diagramDetail.getSelectedNodeIds();
    }

    public void setNodes(List<Node> nodes) {
        diagramDetail.setNodes(nodes);
    }

    public void addNode(Node node) {
        diagramDetail.addNode(node);
    }

    public List<Node> getNodes() {
        return diagramDetail.getNodes();
    }
    
    public void export() {
        // Lookup how large the canvas needs to be based on node positions.
        int maxHeight = 0;
        int maxWidth = 0;
        for (Node node : diagramDetail.getNodes()) {
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
        UI.getCurrent().getPage()
                .executeJs("exportDiagram(" + maxWidth + "," + maxHeight + ",'" + getId().orElse(null) + "');");
    }
    
}
