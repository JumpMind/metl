window.org_jumpmind_metl_ui_diagram_Diagram = function() {
    var self = this;
    var state = this.getState();

    var connectorColor = "#0072C6";
    var outlineColor = "white";
    var selectedColor = "orange";
    var diagramStart = {};
    var nodeStart = {};
    var nodeClickStart = {};
    var ctrlPress = false;
    
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

    /**
     * Enhance a node with input and output end points
     */
    this.addEndpoints = function(node, nodeDiv) {
        if (node.outputLabel != null) {
            var s = instance.addEndpoint(nodeDiv, {
                anchor : "RightMiddle",
                uuid : "source-" + node.id,
                maxConnections : -1,
                isSource : state.readOnly ? false : true,
                overlays: [
                           [ "Label", {
                               id: "label",
                               location: [0.5, 0.5],
                               label: node.outputLabel,
                               cssClass: "endpointSourceLabel"
                           } ]
                       ]                
            });
        }

        if (node.inputLabel != null) {
            var t = instance.addEndpoint(nodeDiv, {
                anchor : "LeftMiddle",
                uuid : "target-" + node.id,
                maxConnections : -1,
                dropOptions : {
                    hoverClass : "hover",
                    activeClass : "active"
                },
                isTarget : state.readOnly ? false : true,
                overlays: [
                           [ "Label", {
                               id: "label",
                               location: [0.5, 0.5],
                               label: node.inputLabel,
                               cssClass: "endpointSourceLabel"
                           } ]
                       ]                
            });
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

        var parentDiv = this.getElement();
        var nodeList = state.nodes;
        for (i = 0; i < nodeList.length; i++) {
            var node = nodeList[i];
            var draggableDiv = document.createElement('div');
            $(draggableDiv).addClass('diagram-node-wrapper');
            draggableDiv.setAttribute('style', "top:" + node.y
                    + "px;left:" + node.x + "px");
            
            var nodeDiv = document.createElement('div');
            draggableDiv.appendChild(nodeDiv);
            
            nodeDiv.id = node.id;
            nodeDiv.setAttribute('style', 'width:' + node.width + 'px;height:' + node.height + "px");
            nodeDiv.innerHTML = node.text;
            nodeDiv.className = "diagram-node";
            if (!node.enabled) {
                $(nodeDiv).addClass("disabled");
            }
            
            $(nodeDiv).click(node_Click);
            $(nodeDiv).mousedown(node_MouseDown);
            $(nodeDiv).mouseup(node_MouseUp);
            
            
            nodeDiv.addEventListener("dblclick", function(event) {
                    self.onNodeDoubleClick({
                        'id' : event.currentTarget.id
                    });
            }, false);
            
            var labelDiv = document.createElement('div');
            $(labelDiv).addClass("diagram-node-label");
            labelDiv.innerHTML = node.name;
            draggableDiv.appendChild(labelDiv);

            parentDiv.appendChild(draggableDiv);

            if (!state.readOnly) {
                instance.draggable(draggableDiv, {
                    constrain : true,
                    start : function(event) {
                        nodeStart[event.el.firstChild.id] = $(event.el).position();
                        event.el.dragging = true;
                    },
                    stop : function(event) {
                        event.el.dragging = false;
                        var start = nodeStart[event.el.firstChild.id];
                        if (start != undefined && (start.left !== event.pos[0] || start.top !== event.pos[1])) {
                            self.onNodeMoved({
                                'id' : event.el.firstChild.id,
                                'x' : event.pos[0],
                                'y' : event.pos[1]
                            });
                        }
                    },
                    grid : [ 10, 10 ],
                    snapThreshold : 10
                });
            }

            var selected = state.selectedNodeIds;
            for (var k = 0; k < selected.length; k++) {
                if (selected[k] == nodeDiv.id) {
                    $(nodeDiv).addClass("selected");
                    instance.addToDragSelection(nodeDiv.parentNode);
                }
            }
            
            self.addEndpoints(node, nodeDiv);
        }
        selectAssociatedNodeLinks();
        
        for (j = 0; j < nodeList.length; j++) {
            for (i = 0; i < nodeList[j].targetNodeIds.length; i++) {
                instance.connect({
                    uuids : [ "source-" + nodeList[j].id, "target-" + nodeList[j].targetNodeIds[i] ],
                    editable : state.readOnly ? false : true
                });
            }
        }       

        if (!state.readOnly) {
            instance.bind("beforeDrop", function(info) {
                var source = info.connection.endpoints[0].getOverlay("label").label;
                var target = info.dropEndpoint.getOverlay("label").label;
                return source === target || source === '*' || target == '*' || (source === 'M' && (target === 'R' || target === 'H')) || ((source === 'R' || source === 'H') && target === 'M');
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

            instance.bind("mousedown", function(connection, originalEvent) {
                originalEvent.stopImmediatePropagation();
                unselectAllLinks();
                $(".diagram-node").removeClass("selected");
                state.selectedNodeId = null;
                instance.detach(connection, {
                    fireEvent : false
                });
                connection = instance.connect({
                    uuids : [ "source-" + connection.sourceId, "target-" + connection.targetId ],
                    editable : true,
                    fireEvent : false
                });
                connection.toggleType("selected");
                sendSelected();
                self.onLinkSelected({
                    'sourceNodeId' : connection.sourceId,
                    'targetNodeId' : connection.targetId,
                });
            });
        }        
        
        var rubberband = document.createElement('div');
        rubberband.id = "rubberband";
        $(rubberband).addClass("rubberband");
        parentDiv.appendChild(rubberband);
        

        $(document).keydown(function(event){
            if(event.which=="17")
                ctrlPress = true;
        });
        $(document).keyup(function(){
            ctrlPress = false;
        });

        $(parentDiv).click(diagramContainer_Click);
        if (!state.readOnly) {
            $(parentDiv).mousedown(diagramContainer_MouseDown);
        }
        $(parentDiv).mousemove(diagramContainer_MouseMove);
        $(parentDiv).mouseup(diagramContainer_MouseUp);
    };

    instance.bind("ready", function() {
        self.layoutAll();
    });

    this.onStateChange = function() {
        instance.batch(function() {
            var selected = state.selectedNodeIds;
            for (var i = 0; i < selected.length; i++) {
                var node = document.getElementById(selected[i]);
                var serverNode = findNode(selected[i]);
                node.parentNode.childNodes[1].innerHTML = serverNode.name;
                if (serverNode.enabled) {
                    $(node).removeClass("disabled");
                } else {
                    $(node).addClass("disabled");
                }
            }
        });
        selectAssociatedNodeLinks();
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
    
    var unselectAllLinks = function() {
        var connections = instance.getAllConnections();
        for (j = 0; j < connections.length; j++) {
            var connection = connections[j];
            connection.removeType("selected");
        }
    }
    
    function diagramContainer_Click(event) {
        if(diagramStart.x === event.pageX && diagramStart.y === event.pageY) {
            instance.clearDragSelection();
            $(".diagram-node").removeClass("selected");
            sendSelected();
        }
    }
    
    function node_Click(event) {
        // Node click event logic is handled in node_mouseDown()
        // Prevent diagram click event from running
        event.stopPropagation();
    }
    
    function sendSelected() {
        var selected = {nodes: []};
        $( ".diagram-node.selected" ).each(function() {
            selected.nodes.push({
                "id" : this.id
            });
        });
        self.onNodeSelected(selected);
    }
    
    function node_MouseDown(event) {
        // Immediately unselect other nodes if new node is not selected.
        if(!$(this).hasClass("selected")) {
            if (!ctrlPress) {
                instance.clearDragSelection();
                $(".diagram-node").removeClass("selected");
                unselectAllLinks();
            }
        }

        // Unselect node if selected while and pressing control
        if($(this).hasClass("selected")) {
            if (ctrlPress) {
                instance.removeFromDragSelection(this.parentNode);
                $(this).removeClass("selected");
                unselectAllLinks();
            }
        } else {
            instance.addToDragSelection(this.parentNode);
            $(this).addClass("selected");
        }
        selectAssociatedNodeLinks();
        
        // If mouse down and mouse up at same location, 
        // un-select other selected nodes.
        nodeClickStart.x = event.pageX;
        nodeClickStart.y = event.pageY;
    }
    
    function node_MouseUp(event) {
        var node = this;
        // Unselect other nodes when we aren't dragging. 
        if(nodeClickStart.x === event.pageX && nodeClickStart.y === event.pageY)
        {
            if (!ctrlPress) {
                instance.clearDragSelection();
                // Unselect all but this node.
                $(".diagram-node").each(function() {
                    if(this!==node)  {
                        $(this).removeClass("selected");
                    }
                });
                unselectAllLinks();
            }
        }

        selectAssociatedNodeLinks();
        sendSelected();
        
        diagramStart.x = event.pageX;
        diagramStart.y = event.pageY;
    }


    function diagramContainer_MouseDown(event) {
        diagramStart.x = event.pageX;
        diagramStart.y = event.pageY;
        
        var offset = $("#diagram").offset();
        var l=diagramStart.x - offset.left;
        var t=diagramStart.y - offset.top;
        
        $("#rubberband").css({top:t, left:l, height:1, width:1, position:'absolute'});
        $("#rubberband").show();
    }
    
    function diagramContainer_MouseMove(event) {
        if($("#rubberband").is(":visible") !== true) { return; }
        
        var t = (event.pageY > diagramStart.y) ? diagramStart.y : event.pageY;
        var l = (event.pageX >= diagramStart.x) ? diagramStart.x : event.pageX;
        var offset = $("#diagram").offset();
        t=t - offset.top;
        l=l - offset.left;
        
        wcalc = event.pageX - diagramStart.x;
        var w = (event.pageX > diagramStart.x) ? wcalc : (wcalc * -1); 
        
        hcalc = event.pageY - diagramStart.y;
        var h = (event.pageY > diagramStart.y) ? hcalc : (hcalc * -1); 
        
        $("#rubberband").css({top:t, left:l, height:h, width:w, position:'absolute'});
    }
    
    function diagramContainer_FindSelectedItem() {
        if($("#rubberband").is(":visible") !== true) { return; }

        $(".diagram-node").removeClass("selected");
        instance.clearDragSelection();
        unselectAllLinks();
        var rubberbandOffset = getTopLeftOffset($("#rubberband")); 

        $(".diagram-node").each(function() {
            var itemOffset = getTopLeftOffset($(this));
            if( itemOffset.top > rubberbandOffset.top &&
                itemOffset.left > rubberbandOffset.left &&
                itemOffset.right < rubberbandOffset.right &&
                itemOffset.bottom < rubberbandOffset.bottom) 
            {
                $(this).addClass("selected");
                instance.addToDragSelection(this.parentNode);
            }
        });
        sendSelected();
        selectAssociatedNodeLinks();
    }
    
    function getTopLeftOffset(element) {
        var elementDimension = {};
        elementDimension.left = element.offset().left;
        elementDimension.top =  element.offset().top;
        elementDimension.right = elementDimension.left + element.outerWidth();
        elementDimension.bottom = elementDimension.top + element.outerHeight();
        return elementDimension;
    }
    
    function diagramContainer_MouseUp(event) {
        if(diagramStart.x !== event.pageX && diagramStart.y !== event.pageY) {
            diagramContainer_FindSelectedItem();
        }
        $("#rubberband").hide();
    }
    
    function diagramContainer_Click(event) {
        if(diagramStart.x === event.pageX && diagramStart.y === event.pageY)
        {
            instance.clearDragSelection();
            $(".diagram-node").removeClass("selected");
            unselectAllLinks();
            sendSelected();
        }
    }
    
    function selectAssociatedNodeLinks() {
        $( ".diagram-node.selected" ).each(function() {
            for (i = 0; i < state.nodes.length; i++) {
                for (j = 0; j < state.nodes[i].targetNodeIds.length; j++) {
                    if (state.nodes[i].id == this.id
                            || state.nodes[i].targetNodeIds[j] == this.id) {
                        connection = instance.connect({
                            uuids : [ "source-" + state.nodes[i].id, "target-" + state.nodes[i].targetNodeIds[j] ],
                            editable : true,
                            fireEvent : false
                        });
                        connection.toggleType("selected");
                    }
                }
            }
        });
    }
    
}

// Client side conversion from design view to image.
function exportDiagram(width, height) {
    html2canvas($('#diagram'),
        {
            onrendered: function(canvas) {
                var svgList = $('#diagram').find( 'svg' );
                svgList.each(function(index, value) {
                    try {
                        var svgExample = this;
                        var serializer = new XMLSerializer();
                        var svgMarkup = serializer.serializeToString(svgExample);
            
                        if(svgMarkup.indexOf('_jsPlumb_connector') > -1) {
                            var left = parseInt($(svgExample).css('left'),10);
                            var top = parseInt($(svgExample).css('top'),10);
                        
                            svgMarkup = svgMarkup.replace('xmlns=\"http://www.w3.org/2000/svg\"','');
                        
                            var connectorCanvas = document.createElement('canvas');
                            canvg(connectorCanvas, svgMarkup);
                        
                            var context = canvas.getContext('2d');
                            context.drawImage(connectorCanvas, left, top);
                        }
                    } catch(err) {
                        log.error('Error converting SVG link to the canvas: ' + err);
                    }
                });
            
                var tempCanvas = document.createElement('canvas');
                tempCanvas.width = canvas.width;
                tempCanvas.height = canvas.height;
            
                tempCanvas.getContext('2d').drawImage(canvas, 0, 0);
                canvas.width = width;
                canvas.height = height;
                
                canvas.getContext('2d').drawImage(tempCanvas, 0, 0);
                
                // Keep the download link if it doesn't work on IE?
//			    $('#downloadLink a').attr('href', canvas.toDataURL().replace('image/png', 'image/octet-stream'));
//			    $('#downloadLink a').attr('download', 'design.png');
                $('#canvasContainer .v-panel-content').append('<img src='+canvas.toDataURL()+' />');
            },
            logging: false,
            // View port used by html2canvas. Use the full canvas.
            width: 10000,
            height: 10000
        });
}