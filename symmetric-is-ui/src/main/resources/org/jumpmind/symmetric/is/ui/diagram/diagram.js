window.org_jumpmind_symmetric_is_ui_diagram_Diagram = function() {
    var self = this;
    var state = this.getState();
    var instance = jsPlumb.getInstance({
        Endpoint : [ "Dot", {
            radius : 2
        } ],
        HoverPaintStyle : {
            strokeStyle : "#1e8151",
            lineWidth : 2
        },
        ConnectionOverlays : [ [ "Arrow", {
            location : 1,
            id : "arrow",
            length : 14,
            foldback : 0.8
        } ], [ "Label", {
            label : "",
            id : "label",
            cssClass : "aLabel"
        } ] ],
        Container : "flow"
    });

    this.layoutAll = function() {
        instance.unbind("dblclick");
        instance.unbind("connection");
        instance.unbind("connectionDetached");
        var parentDiv = document.getElementById(state.id);
        while (parentDiv.firstChild) {
            parentDiv.removeChild(parentDiv.firstChild);
        }

        var nodeList = state.nodes;
        for (i = 0; i < nodeList.length; i++) {
            var node = nodeList[i];

            var nodeDiv = document.createElement('div');
            nodeDiv.id = node.id;
            nodeDiv.setAttribute('style', 'width:' + node.width + 'px;height:' + node.height + "px;top:" + node.y
                    + "px;left:" + node.x + "px");
            nodeDiv.innerHTML = node.text;
            nodeDiv.className = "w";

            var endpointDiv = document.createElement('div');
            endpointDiv.className = "ep";
            nodeDiv.appendChild(endpointDiv);

            parentDiv.appendChild(nodeDiv);
            instance.draggable(nodeDiv, {
                stop : function(event) {
                    self.onNodeMoved({
                        'id' : event.el.id,
                        'x' : event.pos[0],
                        'y' : event.pos[1]
                    });
                }
            });

            var isFilterSupported = instance.isDragFilterSupported();
            if (isFilterSupported) {
                instance.makeSource(nodeDiv, {
                    filter : ".ep",
                    anchor : "Continuous",
                    connector : [ "StateMachine", {
                        curviness : 20
                    } ],
                    connectorStyle : {
                        strokeStyle : "#5c96bc",
                        lineWidth : 2,
                        outlineColor : "transparent",
                        outlineWidth : 4
                    },
                    maxConnections : 5,
                    onMaxConnections : function(info, e) {
                        alert("Maximum connections (" + info.maxConnections + ") reached");
                    }
                });
            } else {
                var eps = jsPlumb.getSelector(".ep");
                for (var i = 0; i < eps.length; i++) {
                    var e = eps[i], p = e.parentNode;
                    instance.makeSource(e, {
                        parent : p,
                        anchor : "Continuous",
                        connector : [ "StateMachine", {
                            curviness : 20
                        } ],
                        connectorStyle : {
                            strokeStyle : "#5c96bc",
                            lineWidth : 2,
                            outlineColor : "transparent",
                            outlineWidth : 4
                        },
                        maxConnections : 5,
                        onMaxConnections : function(info, e) {
                            alert("Maximum connections (" + info.maxConnections + ") reached");
                        }
                    });
                }
            }

            // initialise all '.w' elements as connection targets.
            instance.makeTarget(nodeDiv, {
                dropOptions : {
                    hoverClass : "dragHover"
                },
                anchor : "Continuous",
                allowLoopback : false,
                anchor : "Continuous"
            });

        }

        for (j = 0; j < nodeList.length; j++) {
            for (i = 0; i < nodeList[j].targetNodeIds.length; i++) {
                instance.connect({
                    source : nodeList[j].id,
                    target : nodeList[j].targetNodeIds[i]
                });
            }
        }

        instance.bind("connection", function(info, originalEvent) {
            self.onConnection({
                "sourceNodeId" : info.connection.sourceId,
                "targetNodeId" : info.connection.targetId,
                "removed" : false
            });
        });
        instance.bind("connectionDetached", function(info, originalEvent) {
            self.onConnection({
                "sourceNodeId" : info.connection.sourceId,
                "targetNodeId" : info.connection.targetId,
                "removed" : true
            });
        });
        instance.bind("dblclick", function(connection, originalEvent) {
            self.onConnection({
                "sourceNodeId" : connection.sourceId,
                "targetNodeId" : connection.targetId,
                "removed" : true
            });
        });
    };

    instance.bind("ready", function() {
        instance.doWhileSuspended(function() {
            self.layoutAll();
        });
    });

    this.onStateChange = function() {
        instance.doWhileSuspended(function() {
            self.layoutAll();
        });
    };

}