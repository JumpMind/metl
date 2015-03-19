window.org_jumpmind_symmetric_is_ui_diagram_Diagram = function() {
    var self = this;
    var state = this.getState();

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

    var selectedType = {
        connector : "Flowchart",
        paintStyle : {
            strokeStyle : "orange",
            lineWidth : 2
        }
       
    };
    instance.registerConnectionType("selected", selectedType);

    var connectorPaintStyle = {
        lineWidth : 2,
        strokeStyle : "#0072C6",
        joinstyle : "round",
        outlineColor : "white",
        outlineWidth : 2
    },
    connectorHoverStyle = {
        lineWidth : 2,
        strokeStyle : "#216477",
        outlineWidth : 2,
        outlineColor : "white"
    }, endpointHoverStyle = {
        fillStyle : "#216477",
        strokeStyle : "#216477"
    },
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
            gap : 6,
            cornerRadius : 0,
            alwaysRespectStubs : false
        } ],
        connectorStyle : connectorPaintStyle,
        hoverPaintStyle : endpointHoverStyle,
        connectorHoverStyle : connectorHoverStyle,
        dragOptions : {}
    },
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

    /**
     * Enhance an node with input and output end points
     */
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

    /**
     * Layout the diagram
     */
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
                unselectAll();
                event.currentTarget.className = event.currentTarget.className + " selected ";
                self.onNodeSelected({
                    'id' : event.currentTarget.id
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
        instance.bind("click", function(connection, originalEvent) {
            unselectAll();
            instance.detach(connection, {fireEvent:false});
            connection = instance.connect({
                uuids : [ "source-" + connection.sourceId, "target-" + connection.targetId ],
                editable : true
            });
            connection.toggleType("selected");
            self.onLinkSelected({
                'sourceNodeId' : connection.sourceId,
                'targetNodeId' : connection.targetId,
            });
        });
    };

    instance.bind("ready", function() {
        instance.batch(function() {
            self.layoutAll();
        });
    });

    this.onStateChange = function() {
        instance.batch(function() {
            // self.layoutAll();
        });
    };
    
    var unselectAll = function() {
        var diagramDiv = document.getElementById("diagram");
        var children = diagramDiv.childNodes;
        for (var i = 0; i < children.length; i++) {
            if (children[i].tagName == 'DIV') {
                children[i].className = children[i].className.replace(/(?:^|\s)selected(?!\S)/g, '');
            }
        }
        
        var connections = instance.getAllConnections();
        for (j = 0; j < connections.length; j++) {
            var connection = connections[j];
            connection.removeType("selected")
        }
    }

}