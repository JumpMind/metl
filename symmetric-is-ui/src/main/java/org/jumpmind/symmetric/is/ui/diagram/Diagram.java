package org.jumpmind.symmetric.is.ui.diagram;

import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.JavaScriptFunction;

@JavaScript({ "dom.jsPlumb-1.6.4-min.js", "diagram.js" })
@StyleSheet({ "diagram.css" })
public class Diagram extends AbstractJavaScriptComponent {

    private static final long serialVersionUID = 1L;

    public Diagram() {
        setPrimaryStyleName("diagram");
        setId(UUID.randomUUID().toString());
        addFunction("onNodeMoved", new JavaScriptFunction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void call(JSONArray arguments) throws JSONException {
                if (arguments.length() > 0) {
                    Object obj = arguments.get(0);
                    if (obj instanceof JSONObject) {
                        JSONObject json = arguments.getJSONObject(0);
                        String id = json.getString("id");
                        int x = json.getInt("x");
                        int y = json.getInt("y");
                        DiagramState state = getState();
                        for (Node node : state.nodes) {
                            if (node.getId().equals(id)) {
                                node.setX(x);
                                node.setY(y);
                                fireEvent(new NodeMovedEvent(Diagram.this, node));
                                break;
                            }
                        }
                    }
                }
            }
        });
        
        addFunction("onConnection", new JavaScriptFunction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void call(JSONArray arguments) throws JSONException {
                if (arguments.length() > 0) {
                    Object obj = arguments.get(0);
                    if (obj instanceof JSONObject) {
                        JSONObject json = arguments.getJSONObject(0);
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
                                fireEvent(new ConnectionEvent(Diagram.this, sourceNodeId, targetNodeId, removed));
                                break;
                            }
                        }
                    }
                }
            }
        });

    }

    public void addNode(Node node) {
        getState().nodes.add(node);
        markAsDirty();
    }

    @Override
    protected DiagramState getState() {
        return (DiagramState) super.getState();
    }

    public List<Node> getNodes() {
        return getState().nodes;
    }
}
