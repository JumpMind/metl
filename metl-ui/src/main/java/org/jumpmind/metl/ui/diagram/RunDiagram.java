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

import org.jumpmind.metl.ui.views.manage.ExecutionRunPanel;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.Page;

import elemental.json.JsonArray;
import elemental.json.JsonObject;


/**
 * TODO: ADB: Refactor the RunDiagram screen to reuse the Diagram screen.
 *
 */
@CssImport("./run-diagram.css")
@JsModule("./run-diagram.js")
@JsModule("jquery")
@JsModule("jsplumb")
@JavaScript("./run-diagram.js")
public class RunDiagram extends Div {

    private static final long serialVersionUID = 1L;
    
    private DiagramDetail diagramDetail;
    
    private ExecutionRunPanel panel;

    public RunDiagram(ExecutionRunPanel panel) {
        addClassName("diagram");
        setId("run-diagram");
        diagramDetail = new DiagramDetail();
        this.panel = panel;
    }
    
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Page page = UI.getCurrent().getPage();
        page.executeJs("window.org_jumpmind_metl_ui_diagram_RunDiagram()");
    }
    
    @ClientCallable
    private void onNodeSelected(JsonObject json) {
        List<String> ids = diagramDetail.getSelectedNodeIds();
        ids.clear();
        if (json.hasKey("nodes")) {
            JsonArray nodes = json.getArray("nodes");
            for (int i = 0; i < nodes.length(); i++) {
                ids.add(nodes.getObject(i).getString("id"));
            }
        }
        panel.nodeSelectedEvent(new NodeSelectedEvent(RunDiagram.this, ids));
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
}
