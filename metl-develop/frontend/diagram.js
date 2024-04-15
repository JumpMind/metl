window.org_jumpmind_metl_ui_diagram_Diagram = function(javaElement) {
    var connectorColor = "#0072C6";
    var outlineColor = "white";
    var selectedColor = "orange";
    var diagramStart = {};
    var nodeStart = {};
    var nodeClickStart = {};
    var ctrlPress = false;
    var allowDetach = false;
    var state = null;
    var parentDiv = null;

    parentDiv = javaElement;
    if (javaElement != null) {
        /**
         * When you configure jsPlumb you can give it default settings to use.  These can be
         * overwritten on a component by component basis if need be
         */
        var instance = jsPlumb.getInstance({
            Endpoint : "Dot",
            EndpointStyle : {
                stroke : "transparent",
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
                stroke : connectorColor,
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
            } ] ]
        });

        /**
         * Define a connection type that can be used when a connection is selected
         */
        instance.registerConnectionType("selected", {
            paintStyle : {
                lineWidth: 2,
                stroke : selectedColor,
                joinstyle : "round",
                outlineColor : outlineColor,
                outlineWidth : 2
            }       
        });

        instance.setContainer(parentDiv.id);

        //removing all child elements so it will redraw fresh
        while (parentDiv.firstChild) {
            parentDiv.removeChild(parentDiv.firstChild);
        }
        //in order to do $server the caller or javaElement must be at the highest level container, which is diagram for this file
        state = javaElement.$server.getCurState().then(
            (st)  => { 
                state = JSON.parse(st);
                layoutAll();
                //repainting jsplumb objects to make sure they are updated correctly
                instance.repaintEverything();
                console.log("************* STATE = " + state);
             }
        );

        /**
         * Enhance a node with input and output end points
         */
        function addEndpoints(node, nodeDiv) {
            //when re-drawing the diagram, all objects must be deleted or else it will not draw correctly because it uses the object from memory.
            instance.deleteObject(nodeDiv);

            if (node.outputLabel != null) {
                var s = instance.addEndpoint(nodeDiv, {
                    anchor : "RightMiddle",
                    uuid : "source-" + node.id,
                    maxConnections : -1,
                    isSource : true,
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
                    isTarget : true,
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
        function layoutAll() {
            //remove all of the listeners just to make sure everything is clean when creating nodes/links
            instance.unbind("beforeDrop");
            instance.unbind("dblclick");
            instance.unbind("connection");
            instance.unbind("beforeDetach");
            instance.unbind("connectionMoved");
            instance.unbind("connectionDrag");

            var nodeList = state.nodes;
            for (i = 0; i < nodeList.length; i++) {
                var node = nodeList[i];
                var draggableDiv = document.createElement('div');
                draggableDiv.classList.add('diagram-node-wrapper');
                draggableDiv.setAttribute('style', "top:" + node.y
                        + "px;left:" + node.x + "px;width:" + node.width +"px");
                
                var nodeDiv = document.createElement('div');
                draggableDiv.appendChild(nodeDiv);
                nodeDiv.id = node.id;

                //sets height and width of node
                nodeDiv.setAttribute('style', 'width:' + 80 + 'px;height:' + 80 + "px");

                //sets the image
                nodeDiv.innerHTML = node.text;
                nodeDiv.className = "diagram-node";
                if (!node.enabled) {
                    nodeDiv.classList.add("disabled");
                }
                
                //this is the new way to set a listener
                nodeDiv.addEventListener("dblclick", function(event) {
                    document.getElementById(parentDiv.id).$server.onNodeDoubleClick({
                        'id' : event.currentTarget.id
                    });
                }, false);

                nodeDiv.addEventListener("click",function(){
                    node_Click(event);
                });
                
                nodeDiv.addEventListener("mousedown",function(){
                    node_MouseDown(event);
                });
                nodeDiv.addEventListener("mouseup",function(){
                    node_MouseUp(event);
                });
            
                var labelDiv = document.createElement('div');
                labelDiv.setAttribute('style', 'margin-top: 10px;');
                labelDiv.classList.add("diagram-node-label");
                labelDiv.innerHTML = node.name;
                nodeDiv.appendChild(labelDiv);
                parentDiv.appendChild(draggableDiv); 

                //state.readOnly does not exist at all
                //state was deprecated as of Vaadin 14
                //this loop will always fire because !(undefined) will always return true
                if (!(state.readOnly)) {
                    instance.draggable(draggableDiv, {
                        constrain : true,
                        containment: 'diagram',
                        start : function(event) {
                            var coords = event.pos;
                            if(coords != null){
                                nodeStart[event.el.firstChild.id] = event.pos();
                            } else {
                                nodeStart[event.el.firstChild.id] = [event.e.clientX,event.e.clientY];
                            }
                            
                            event.el.dragging = true;
                        },
                        stop : function(event) {
                            event.el.dragging = false;
                            var start = nodeStart[event.el.firstChild.id];
                            if (start != undefined && (start.left !== event.pos[0] || start.top !== event.pos[1])) {
                                var serverComms = document.getElementById(parentDiv.id);
                                serverComms.$server.onNodeMoved({
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
                        nodeDiv.classList.add("selected");
                        instance.addToDragSelection(nodeDiv.parentNode);
                    }
                }
                
                addEndpoints(node, nodeDiv);
            }
            
            for (var j = 0; j < nodeList.length; j++) {
                for (var i = 0; i < nodeList[j].targetNodeIds.length; i++) {
                    instance.connect({
                        uuids : [ "source-" + nodeList[j].id, "target-" + nodeList[j].targetNodeIds[i] ],
                        editable : true
                    });
                }
            }       

            //This section is a bunch of calls to execute java code
            if (!(state.readOnly)) {
                instance.bind("beforeDrop", function(info) {
                    var source = info.connection.endpoints[0].getOverlay("label").label;
                    var target = info.dropEndpoint.getOverlay("label").label;
                    return source === target || source === '*' || target == '*' || (source === 'M' && (target === 'R' || target === 'H')) || ((source === 'R' || source === 'H') && target === 'M');
                });
                instance.bind("connection", function(info, originalEvent) {
                    allowDetach = false;
                    var sourceNode = info.source;
                    var diagramDiv = document.getElementById(parentDiv.id);
                    diagramDiv.$server.onConnection({
                        "sourceNodeId" : info.connection.sourceId,
                        "targetNodeId" : info.connection.targetId,
                        "removed" : false
                    });
                });
                instance.bind("connectionMoved", function(info, originalEvent) {
                    allowDetach = false;
                    var sourceNode = info.source;
                    var diagramDiv = document.getElementById(parentDiv.id);
                    diagramDiv.$server.onConnectionMoved({
                        "sourceNodeId" : info.newSourceId,
                        "targetNodeId" : info.newTargetId,
                        "origSourceNodeId" : info.originalSourceId,
                        "origTargetNodeId" : info.originalTargetId
                    });
                });
                instance.bind("connectionDrag", function(connection) {
                    allowDetach = true;
                });
                instance.bind("beforeDetach", function(connection) {
                    if (!allowDetach) {
                        return false;
                    }
                    allowDetach = false;
                    var diagramDiv = document.getElementById(parentDiv.id);
                    diagramDiv.$server.onConnection({
                        "sourceNodeId" : connection.sourceId,
                        "targetNodeId" : connection.targetId,
                        "removed" : true
                    });
                    return true;
                });

                instance.bind("mousedown", function(connection, originalEvent) {
                    originalEvent.stopImmediatePropagation();
                    unselectAllLinks();
                    var diagramDiv = document.getElementById(parentDiv.id);
                    for (var x = 0; x < diagramDiv.children.length; x++) {
                        if (diagramDiv.children[x].classList.contains("diagram-node-wrapper")) {
                            var innerKids = diagramDiv.children[x].children
                            for (var k = 0; k < innerKids.length; k++) {
                                if (innerKids[k].classList.contains("diagram-node")) {
                                    innerKids[k].classList.remove("selected");
                                }
                            }
                        }
                    }
                    state.selectedNodeId = null;
                    instance.deleteConnection(connection, {
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
            rubberband.id = parentDiv.id + "-rubberband";
            rubberband.classList.add("rubberband");
            parentDiv.appendChild(rubberband);
            

            //17 is for control and 91 is for command. control + click on mac = right click and causes issues
            document.addEventListener("keydown", (function(event) {
                if (event.which == "17" || event.which == "91") {
                    ctrlPress = true;
                }
            }));

            //onkeyup was changed to keyup
            document.addEventListener("keyup", (function(event) {
                ctrlPress = false;
            }));

            //not sure what this does.
            parentDiv.click(diagramContainer_Click);
            if (!(state.readOnly)) {
                parentDiv.addEventListener("mousedown",function() {
                diagramContainer_MouseDown(event);
            });
            }

            //looks like these are set to trigger when the diagram is clicked... not quite sure why. Diagram is anything that isnt the nodes or links
            parentDiv.addEventListener("mouseup",function() {
                diagramContainer_MouseUp(event);
            });
            parentDiv.addEventListener("mousemove",function() {
                diagramContainer_MouseMove(event);
            });
        };

        var unselectAllLinks = function() {
            var connections = instance.getAllConnections();
            for (var j = 0; j < connections.length; j++) {
                var connection = connections[j];
                connection.removeType("selected");
            }
        }

        function diagramContainer_Click(event) {
            if (diagramStart.x === event.pageX && diagramStart.y === event.pageY) {
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
            var nodeList = document.getElementsByClassName("diagram-node");
            for (var z = 0; z < nodeList.length; z++) {
                if (nodeList[z].classList.contains("diagram-node") && nodeList[z].classList.contains("selected")) {
                    selected.nodes.push({
                        "id" : nodeList[z].id
                    })
                }
            }
            document.getElementById(parentDiv.id).$server.onNodeSelected(selected);
        }

        function node_MouseDown(event) {
            if (!(event.type == "readystatechange")) {
                var currentElement = event.srcElement;
                var node = getContainerNode(currentElement);
                // Immediately unselect other nodes if new node is not selected.
                if (!(node.classList.contains("selected"))) {
                    if (!(ctrlPress)) {
                        instance.clearDragSelection();
                        //create a list of all the nodes
                        var nodeList = document.getElementsByClassName("diagram-node");
                        //cycle through and remove the selected class from all of the nodes
                        for (var x = 0; x < nodeList.length; x++) {
                            if (nodeList[x].classList.contains("selected")) {
                                nodeList[x].classList.remove("selected");
                            }
                        }

                        if (document.getElementsByClassName("diagram-node").classList != null) {
                            document.getElementsByClassName("diagram-node").classList.remove("selected");
                        }
                        
                        unselectAllLinks();
                    }
                }

                // Unselect node if selected while and pressing control
                if (node.classList.contains("selected")) {
                    if (ctrlPress) {
                        var node = getContainerNode(currentElement);
                        var nodeParent = node.parentNode;
                        instance.removeFromDragSelection(nodeParent);
                        node.classList.remove("selected");
                        unselectAllLinks();
                    }
                } else {
                    var node = getContainerNode(currentElement);
                    var nodeParent = node.parentNode;
                    instance.addToDragSelection(nodeParent);
                    node.classList.add("selected");
                }
                selectAssociatedNodeLinksWithEvent(event);
                
                // If mouse down and mouse up at same location,
                // un-select other selected nodes.
                nodeClickStart.x = event.pageX;
                nodeClickStart.y = event.pageY;
            }
        }

        //The purpose of this function is to search for the Div that contains the node class, meaning it is the parent node for things like endpoints and connections.
        function getContainerNode(currElement) {
            var node = currElement;
            while (!(node.classList.contains("diagram-node"))) {
                node = node.parentNode;
            }
            return node;
        }

        function node_MouseUp(event) {
            if (!(event.type == "readystatechange")) {
                var currentElement = event.srcElement;
                var node = getContainerNode(currentElement);
                // Unselect other nodes when we aren't dragging.
                if ((nodeClickStart.x == event.pageX) && (nodeClickStart.y == event.pageY)) {
                    if (!ctrlPress) {
                        // Unselect all but this node.
                        var selectedNodes = 0;
                        var nodeList = document.getElementsByClassName("diagram-node");
                        for (var x = 0; x < nodeList.length; x++) {
                            if (nodeList[x].classList.contains("diagram-node") && (nodeList[x] !== node)) {
                                if (nodeList[x].classList.contains("selected")) {
                                    //this is in place to prevent the drag selection from being cleared if one node is selected and no other nodes are.
                                    //this would cause issues when selecting multiple nodes to be dragged together
                                    selectedNodes = selectedNodes + 1;
                                }
                                // nodeList[x].classList.remove("selected");
                            }
                        }
                        //if multiple nodes are selected, and you click on one singular node, remove the selected attribute from the others and clear the drag selection
                        if (selectedNodes > 0) {
                            for (var x = 0; x < nodeList.length; x++) {
                                if (nodeList[x].classList.contains("diagram-node") && (nodeList[x] !== node)) {
                                    nodeList[x].classList.remove("selected");
                                }
                            }
                            instance.clearDragSelection();
                        }
                        unselectAllLinks();
                    }
                }

                selectAssociatedNodeLinksWithEvent(event);
                sendSelected();
                
                diagramStart.x = event.pageX;
                diagramStart.y = event.pageY;
            }
        }

        function diagramContainer_MouseDown(event) {
            if (!(event.type == "readystatechange")) {
                diagramStart.x = event.pageX;
                diagramStart.y = event.pageY;
                
                var rectangle = event.currentTarget.getBoundingClientRect();
                var l = diagramStart.x - rectangle.left;
                var t = diagramStart.y - rectangle.top;
            
                $("#" + parentDiv.id + "-rubberband").css({top:t, left:l, height:1, width:1, position:'absolute'});
                $("#" + parentDiv.id + "-rubberband").show();
            }
        }

        function diagramContainer_MouseMove(event) {   
            if ($("#" + parentDiv.id + "-rubberband").is(":visible") !== true) { return; }
            
            var t = (event.pageY > diagramStart.y) ? diagramStart.y : event.pageY;
            var l = (event.pageX >= diagramStart.x) ? diagramStart.x : event.pageX;

            var rectangle = event.currentTarget.getBoundingClientRect();
            t = t - rectangle.top;
            l = l - rectangle.left;
            
            var wcalc = event.pageX - diagramStart.x;
            var w = (event.pageX > diagramStart.x) ? wcalc : (wcalc * -1); 
            
            var hcalc = event.pageY - diagramStart.y;
            var h = (event.pageY > diagramStart.y) ? hcalc : (hcalc * -1); 
            
            $("#" + parentDiv.id + "-rubberband").css({top:t, left:l, height:h, width:w, position:'absolute'});
        }

        function diagramContainer_FindSelectedItem(event) {
            if ($("#" + parentDiv.id + "-rubberband").is(":visible") !== true) { return; }

            $(".diagram-node").removeClass("selected");
            instance.clearDragSelection();
            unselectAllLinks();
            var rubberbandOffset = getTopLeftOffset($("#" + parentDiv.id + "-rubberband")); 

            $(".diagram-node").each(function() {
                var itemOffset = getTopLeftOffset($(this));
                
                if (itemOffset.top > rubberbandOffset.top &&
                    itemOffset.left > rubberbandOffset.left &&
                    itemOffset.right < rubberbandOffset.right &&
                    itemOffset.bottom < rubberbandOffset.bottom) 
                {
                    $(this).addClass("selected");
                    instance.addToDragSelection(this.parentNode);
                }
                
            });
            sendSelected();
            selectAssociatedNodeLinksWithEvent(event);
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
            if (diagramStart.x !== event.pageX && diagramStart.y !== event.pageY) {
                diagramContainer_FindSelectedItem(event);
            }
            $("#" + parentDiv.id + "-rubberband").hide();
        }

        function selectAssociatedNodeLinksWithEvent(event) {
            var outerContainer = document.getElementById(parentDiv.id);
            for (var x = 0; x < outerContainer.children.length; x++) {
                if (outerContainer.children[x].classList.contains("diagram-node-wrapper")) {
                    var innerKids = outerContainer.children[x].children
                    for (var k = 0; k < innerKids.length; k++) {
                        if ((innerKids[k].classList.contains("diagram-node")) && innerKids[k].classList.contains("selected")) {
                            for (var i = 0; i < state.nodes.length; i++) {
                                for (var j = 0; j < state.nodes[i].targetNodeIds.length; j++) {
                                    if ((state.nodes[i].id === event.currentTarget.id) || (state.nodes[i].targetNodeIds[j] === event.currentTarget.id)) {
                                        /*connection = instance.connect({
                                            uuids : [ "source-" + state.nodes[i].id, "target-" + state.nodes[i].targetNodeIds[j] ],
                                            editable : true,
                                            fireEvent : false
                                        });
                                        connection.toggleType("selected");*/
                                        var conExists = instance.select({source:state.nodes[i].id,target:state.nodes[i].targetNodeIds[j]});
                                        if (conExists.length > 0) {
                                            conExists.setType("selected");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }    
        }
    }
}

// Client side conversion from design view to image.
window.exportDiagram = function(width, height, id) {
    html2canvas($('#' + id),
        {
            onrendered: function(canvas) {
                var svgList = $('#' + id).find( 'svg' );
                svgList.each(function(index, value) {
                    try {
                        var svgExample = this;
                        var serializer = new XMLSerializer();
                        var svgMarkup = serializer.serializeToString(svgExample);
            
                        if (svgMarkup.indexOf('jtk-connector') > -1) {
                            var left = parseInt($(svgExample).css('left'),10);
                            var top = parseInt($(svgExample).css('top'),10);
                        
                            svgMarkup = svgMarkup.replace('xmlns=\"http://www.w3.org/2000/svg\"','');
                        
                            var connectorCanvas = document.createElement('canvas');
                            canvg(connectorCanvas, svgMarkup);
                        
                            var context = canvas.getContext('2d');
                            context.drawImage(connectorCanvas, left, top);
                        }
                    } catch (err) {
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
                $('#canvasContainer').append('<img src='+canvas.toDataURL()+' />');
            },
            logging: false,
            // View port used by html2canvas. Use the full canvas.
            width: 10000,
            height: 10000
        });
}