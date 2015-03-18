window.org_jumpmind_symmetric_is_ui_diagram_Diagram = function() {
    var self = this;
    var state = this.getState();
    this.startX = -1;
    this.startY = -1;

    var instance = jsPlumb.getInstance({
        ConnectionOverlays : [ [ "Arrow", {
            location : 1,
            id : "arrow",
            length : 12,
            width : 12,
            foldback : 1
        } ] ],
        Container : "diagram"
    });

    var basicType = {
        connector : "Flowchart",
        paintStyle : {
            strokeStyle : "red",
            lineWidth : 2
        },
        ConnectionOverlays : [ [ "Arrow", {
            location : 1,
            id : "arrow",
            length : 14,
            width : 12,
            foldback : 1
        } ] ]
    };
    instance.registerConnectionType("basic", basicType);

    var connectorPaintStyle = {
        lineWidth : 2,
        strokeStyle : "#0072C6",
        joinstyle : "round",
        outlineColor : "white",
        outlineWidth : 2
    },
    // .. and this is the hover style.
    connectorHoverStyle = {
        lineWidth : 2,
        strokeStyle : "#216477",
        outlineWidth : 2,
        outlineColor : "white"
    }, endpointHoverStyle = {
        fillStyle : "#216477",
        strokeStyle : "#216477"
    },
    // the definition of source endpoints (the small blue ones)
    sourceEndpoint = {
        endpoint : "Dot",
        paintStyle : {
            strokeStyle : "#0072C6",
            fillStyle : "transparent",
            radius : 5,
            lineWidth : 2
        },
        maxConnections : -1,
        isSource : true,
        connector : [ "Flowchart", {
            stub : [ 40, 60 ],
            gap : 10,
            cornerRadius : 0,
            alwaysRespectStubs : false
        } ],
        connectorStyle : connectorPaintStyle,
        hoverPaintStyle : endpointHoverStyle,
        connectorHoverStyle : connectorHoverStyle,
        dragOptions : {}
    },
    // the definition of target endpoints (will appear when the user drags a connection)
    targetEndpoint = {
        endpoint : "Dot",
        paintStyle : {
            fillStyle : "#0072C6",
            radius : 6
        },
        hoverPaintStyle : endpointHoverStyle,
        maxConnections : -1,
        dropOptions : {
            hoverClass : "hover",
            activeClass : "active"
        },
        isTarget : true
    };

    this.addEndpoints = function(toId) {
        instance.addEndpoint(toId, sourceEndpoint, {
            anchor : "RightMiddle",
            uuid : "source-" + toId
        });
        instance.addEndpoint(toId, targetEndpoint, {
            anchor : "LeftMiddle",
            uuid : "target-" + toId
        });
    };

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
            nodeDiv.className = "diagram-node";

            nodeDiv.addEventListener("click", function(event) {
//                var endpoints = instance.getEndpoints(event.currentTarget);
//                for (j = 0; j < endpoints.length; j++) {
//                    var endpoint = endpoints[j];
//                    endpoint.setPaintStyle({
//            strokeStyle : "red",
//            fillStyle : "transparent",
//            radius : 5,
//            lineWidth : 2
//        });
//                }
                self.onNodeMoved({
                    'id' : event.currentTarget.id,
                    'x' : 0,
                    'y' : 0
                });
            }, false);

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

            self.addEndpoints(nodeDiv.id);

        }

        for (j = 0; j < nodeList.length; j++) {
            for (i = 0; i < nodeList[j].targetNodeIds.length; i++) {
                instance.connect({
                    uuids : [ "source-" + nodeList[j].id, "target-" + nodeList[j].targetNodeIds[i] ],
                    editable : true
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
        instance.bind("click", function(conn, originalEvent) {
            // if (confirm("Delete connection from " + conn.sourceId + " to " + conn.targetId + "?"))
            //   instance.detach(conn);
            conn.toggleType("basic");
        });
    };

    instance.bind("ready", function() {
        instance.batch(function() {
            self.layoutAll();
        });
    });

    this.onStateChange = function() {
        instance.batch(function() {
            //self.layoutAll();
        });
    };

}