package org.jumpmind.metl.ui.diagram;

import java.util.List;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.JavaScriptFunction;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

@JavaScript({ "dom.jsPlumb-1.7.5-min.js", "diagram.js" })
@StyleSheet({ "diagram.css" })
public class Diagram extends AbstractJavaScriptComponent {

    private static final long serialVersionUID = 1L;

    public Diagram() {
        setPrimaryStyleName("diagram");
        setId("diagram");

        addFunction("onLinkSelected", new JavaScriptFunction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void call(JsonArray arguments) {
                if (arguments.length() > 0) {
                    Object obj = arguments.get(0);
                    if (obj instanceof JsonObject) {
                        JsonObject json = arguments.getObject(0);
                        String sourceNodeId = json.getString("sourceNodeId");
                        String targetNodeId = json.getString("targetNodeId");
                        fireEvent(new LinkSelectedEvent(Diagram.this, sourceNodeId, targetNodeId));
                    }
                }
            }
        });

        addFunction("onNodeSelected", new JavaScriptFunction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void call(JsonArray arguments) {
                if (arguments.length() > 0) {
                    Object obj = arguments.get(0);
                    if (obj instanceof JsonObject) {
                        JsonObject json = arguments.getObject(0);
                        if (json.hasKey("id")) {
                            String id = json.getString("id");
                            DiagramState state = getState();
                            for (Node node : state.nodes) {
                                if (node.getId().equals(id)) {
                                    state.selectedNodeId = id;
                                    fireEvent(new NodeSelectedEvent(Diagram.this, node));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        });

        addFunction("onNodeDoubleClick", new JavaScriptFunction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void call(JsonArray arguments) {
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
            }
        });

        addFunction("onNodeMoved", new JavaScriptFunction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void call(JsonArray arguments) {
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
                                node.setX((int) x);
                                node.setY((int) y);
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
            public void call(JsonArray arguments) {
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
            }
        });

        addFunction("onConnectionMoved", new JavaScriptFunction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void call(JsonArray arguments) {
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
            }
        });

    }

    public void setSelectedNodeId(String nodeId) {
        getState().selectedNodeId = nodeId;
        markAsDirty();
    }

    public String getSelectedNodeId() {
        return getState().selectedNodeId;
    }

    public void setNodes(List<Node> nodes) {
        getState().nodes = nodes;
        markAsDirty();
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
