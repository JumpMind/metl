window.org_jumpmind_symmetric_is_ui_diagram_Diagram = function() {
    var self = this;
    var state = this.getState();

    var connectorColor = "#0072C6";
    var outlineColor = "white";
    var selectedColor = "orange";

    /**
     * When you configure jsPlumb you can give it default settings to use.  These can be
     * overwritten on a component by component basis if need be
     */
    var instance = jsPlumb.getInstance({
        Endpoint : "Dot",
        EndpointStyle : {
            strokeStyle : "transparent",
            fillStyle : "transparent",
            radius : 9,
            lineWidth : 2
        },
        Connector : [ "Flowchart", {
            stub : [ 40, 60 ],
            gap : 8,
            cornerRadius : 0
        } ],
        PaintStyle : {
            lineWidth : 2,
            strokeStyle : connectorColor,
            joinstyle : "round",
            outlineColor : outlineColor,
            outlineWidth : 2
        },
        ConnectionOverlays : [ [ "Arrow", {
            location : 1,
            id : "arrow",
            length : 12,
            width : 12,
            foldback : 1
        } ] ],
        Container : "diagram"
    });
    

    /**
     * Define a connection type that can be used when a connection is selected
     */
    instance.registerConnectionType("selected", {
        paintStyle : {
            strokeStyle : selectedColor,
        }       
    });

    var sourceEndpoint = {
        maxConnections : -1,
        isSource : true,
        overlays: [
                   [ "Label", {
                       id: "label",
                       location: [0.5, 0.5],
                       label: "E",
                       cssClass: "endpointSourceLabel"
                   } ]
               ]
    },
    targetEndpoint = {
        maxConnections : -1,
        dropOptions : {
            hoverClass : "hover",
            activeClass : "active"
        },
        isTarget : true,
        overlays: [
                   [ "Label", {
                       id: "label",
                       location: [0.5, 0.5],
                       label: "E",
                       cssClass: "endpointSourceLabel"
                   } ]
               ]
    };

    /**
     * Enhance an node with input and output end points
     */
    this.addEndpoints = function(node) {
        if (node.outputLabel != null) {
            var s = instance.addEndpoint(node.id, sourceEndpoint, {
                anchor : "RightMiddle",
                uuid : "source-" + node.id
            });
            s.getOverlay("label").label = node.outputLabel;
        }

        if (node.inputLabel != null) {
            var t = instance.addEndpoint(node.id, targetEndpoint, {
                anchor : "LeftMiddle",
                uuid : "target-" + node.id
            });
            t.getOverlay("label").label = node.inputLabel;
        }
    };

    /**
     * Layout the diagram
     */
    this.layoutAll = function() {
        instance.unbind("beforeDrop");
        instance.unbind("dblclick");
        instance.unbind("connection");
        instance.unbind("connectionDetached");
        instance.unbind("connectionMoved");

        var parentDiv = document.getElementById(state.id);
        while (parentDiv.firstChild) {
            parentDiv.removeChild(parentDiv.firstChild);
        }

        var nodeList = state.nodes;
        for (i = 0; i < nodeList.length; i++) {
            var node = nodeList[i];

            var draggableDiv = document.createElement('div');
            draggableDiv.className='diagram-node-wrapper';
            draggableDiv.setAttribute('style', "top:" + node.y
                    + "px;left:" + node.x + "px");
            
            var nodeDiv = document.createElement('div');
            draggableDiv.appendChild(nodeDiv);
                        
            nodeDiv.id = node.id;
            nodeDiv.setAttribute('style', 'width:' + node.width + 'px;height:' + node.height + "px");
            nodeDiv.innerHTML = node.text;
            nodeDiv.className = "diagram-node";

            nodeDiv.addEventListener("click", function(event) {
                if (state.selectedNodeId !== event.currentTarget.id) {
                    self.onNodeSelected({
                        'id' : event.currentTarget.id
                    });
                } 
            }, false);
            
            nodeDiv.addEventListener("dblclick", function(event) {
                    self.onNodeDoubleClick({
                        'id' : event.currentTarget.id
                    });
            }, false);
            
            var labelDiv = document.createElement('div');
            labelDiv.className = "diagram-node-label";
            labelDiv.innerHTML = node.name;
            draggableDiv.appendChild(labelDiv);

            parentDiv.appendChild(draggableDiv);
            
            instance.draggable(draggableDiv, {
                constrain:true,
                start : function(event) {
                    event.el.dragging = true;
                },
                stop : function(event) {
                    event.el.dragging = false;
                    self.onNodeMoved({
                        'id' : event.el.firstChild.id,
                        'x' : event.pos[0],
                        'y' : event.pos[1]
                    });
                },
                grid : [10, 10],
                snapThreshold : 10
                
            });

            self.addEndpoints(node);

        }

        for (j = 0; j < nodeList.length; j++) {
            for (i = 0; i < nodeList[j].targetNodeIds.length; i++) {
                instance.connect({
                    uuids : [ "source-" + nodeList[j].id, "target-" + nodeList[j].targetNodeIds[i] ],
                    editable : true
                });
            }
        }
        instance.bind("beforeDrop", function(info) {
            var source = info.connection.endpoints[0].getOverlay("label").label;
            var target = info.dropEndpoint.getOverlay("label").label;
            return source === target || source === '*' || target == '*';
        });
        instance.bind("connection", function(info, originalEvent) {
            self.onConnection({
                "sourceNodeId" : info.connection.sourceId,
                "targetNodeId" : info.connection.targetId,
                "removed" : false
            });
        });
        instance.bind("connectionMoved", function(info, originalEvent) {
            self.onConnectionMoved({
                "sourceNodeId" : info.newSourceId,
                "targetNodeId" : info.newTargetId,
                "origSourceNodeId" : info.originalSourceId,
                "origTargetNodeId" : info.originalTargetId
            });
        }); 
        instance.bind("connectionDetached", function(info, originalEvent) {
            self.onConnection({
                "sourceNodeId" : info.connection.sourceId,
                "targetNodeId" : info.connection.targetId,
                "removed" : true
            });
        });
        instance.bind("click", function(connection, originalEvent) {
            unselectAll();            
            state.selectedNodeId = null;
            instance.detach(connection, {fireEvent:false});
            connection = instance.connect({
                uuids : [ "source-" + connection.sourceId, "target-" + connection.targetId ],
                editable : true,
                fireEvent:false
            });
            connection.toggleType("selected");
            self.onLinkSelected({
                'sourceNodeId' : connection.sourceId,
                'targetNodeId' : connection.targetId,
            });
        });
        
        self.onNodeSelected({
            'id' : state.selectedNodeId
        });
        
    };

    instance.bind("ready", function() {
        self.layoutAll();
    });

    this.onStateChange = function() {
        instance.batch(function() {
            unselectAll();  
            if (state.selectedNodeId != null) {
                var node = document.getElementById(state.selectedNodeId);
                if (node != null) {                    
                    node.className = node.className + " selected ";
                    node.parentNode.childNodes[1].innerHTML = findNode(state.selectedNodeId).name;
                }
            }
        });
    };
    
    var findNode = function(id) {
        var nodeList = state.nodes;
        for (i = 0; i < nodeList.length; i++) {
            var node = nodeList[i];
            if (node.id === id) {
                return node;
            }
        }
        return null;
    };
    
    var unselectAll = function() {
        var children = document.getElementsByClassName("diagram-node");
        for (var i = 0; i < children.length; i++) {
            children[i].className = children[i].className.replace(/(?:^|\s)selected(?!\S)/g, '');
        }
        
        var connections = instance.getAllConnections();
        for (j = 0; j < connections.length; j++) {
            var connection = connections[j];
            connection.removeType("selected");
        }
    }

}