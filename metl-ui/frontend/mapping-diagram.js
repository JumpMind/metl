window.org_jumpmind_metl_ui_mapping_MappingDiagram = function(javaElement) {
    var state = null
    var selectedSrcId = null;
    var selectedDstId = null;
    var inputModelFilter = "";
    var outputModelFilter = "";
    var inputFilterPopulated = false;
    var outputFilterPopulated = false;

    if (javaElement != null) {
        var mappingDiv = javaElement;
        var scrollDiv = mappingDiv.parentNode;
        var topDiv = scrollDiv.parentNode;
    
        var instance = jsPlumb.getInstance({
            Endpoint : [ "Dot", {
                radius : 7
            } ],
            EndpointStyle : {
                fillStyle : "#0072C6"
            },
            Anchor : [ "Left", "Right" ],
            Connector : [ "Straight", {
                gap : 7
            } ],
            ConnectionOverlays : [ [ "Arrow", {
                location : 1,
                width : 12,
                length : 12,
                foldback : 1
            } ] ],
            PaintStyle : {
                lineWidth : 2,
                stroke : "#0072C6",
                outlineColor : "white",
                outlineWidth : 2
            }
        });
    
        instance.registerConnectionType("selected", {
            paintStyle : {
                lineWidth : 2,
                stroke : "orange",
                outlineColor : "white",
                outlineWidth : 2
            }
        });
        instance.registerEndpointType("selected", {
            paintStyle : {
                fillStyle : "orange"
            }
        });

        instance.setContainer(mappingDiv.id);

        //removing all child elements so it will redraw fresh
        while (mappingDiv.firstChild) {
            mappingDiv.removeChild(mappingDiv.firstChild);
        }
        //in order to do $server the caller or javaElement must be at the highest level container, which is diagram for this file
        state = javaElement.$server.getCurState().then(
            (st)  => { 
                state = JSON.parse(st);
                rebuildAll();
                //repainting jsplumb objects to make sure they are updated correctly
                instance.repaintEverything();
                console.log("************* STATE = " + state);
                }
        );
    
        if (!state.readOnly) {
            scrollDiv.parentNode.addEventListener("click", function(event) {
                if (event.target.tagName == "DIV") {
                    unselectAllConnections();
                    document.getElementById(mappingDiv.id).$server.onSelect({});
                }
                if (event.target.classList !== undefined && !event.target.classList.contains("dst")) {
                    selectedSrcId = selectedDstId = null;
                    unselectAllNodes();
                }
            }, false);

            instance.bind("connection", function(info, originalEvent) {
                unselectAllNodes();
                diagramDiv.$server.onConnection({
                    "sourceId" : info.connection.sourceId,
                    "targetId" : info.connection.targetId,
                    "removed" : false
                });
            });
    
            instance.bind("connectionDetached", function(info, originalEvent) {
                unselectAllNodes();
                diagramDiv.$server.onConnection({
                    "sourceId" : info.connection.sourceId,
                    "targetId" : info.connection.targetId,
                    "removed" : true
                });
            });
    
            instance.bind("connectionMoved", function(info, originalEvent) {
                unselectAllNodes();
                if (info.connection.getType().indexOf("selected") != -1) {
                    for (var i = 0; i < info.connection.endpoints.length; i++) {
                        info.connection.endpoints[i].toggleType("selected");
                    }
                }
                diagramDiv.$server.onConnection({
                    "sourceId" : info.originalSourceId,
                    "targetId" : info.originalTargetId,
                    "removed" : true
                });
            });
    
            instance.bind("click", function(connection, event) {
                if (connection.connections !== undefined) {
                    connection = connection.connections[0];
                }
                unselectAllConnections();
                instance.deleteConnection(connection, {
                    fireEvent : false
                });
                var connection = instance.connect({
                    source : connection.sourceId,
                    target : connection.targetId,
                    fireEvent : false
                });
                connection.toggleType("selected");
                for (var i = 0; i < connection.endpoints.length; i++) {
                    connection.endpoints[i].toggleType("selected");
                }
                document.getElementById(mappingDiv.id).$server.onSelect({
                    "sourceId" : connection.sourceId,
                    "targetId" : connection.targetId
                });
            });
    
            instance.bind("connectionDrag", function(connection) {
                dragConnection = connection;
            });
        }
    
        function unselectAllConnections() {
            var connections = instance.getAllConnections();
            for (var i = 0; i < connections.length; i++) {
                connections[i].removeType("selected");
                for (var j = 0; j < connections[i].endpoints.length; j++) {
                    connections[i].endpoints[j].removeType("selected");
                }
            }
        }
    
        function addConnections() {
                addModelConnections();
        }
        
        function addModelConnections() {
            var settings = state.component.modelSettings;
            for (var i = 0; i < settings.length; i++) {
                var setting = settings[i];
                if (setting.name == state.mapsToSchemaObjectName) {
                    instance.connect({
                        source : "src" + setting.modelObjectId,
                        target : "dst" + setting.value,
                        fireEvent : false
                    });
                }
            }    	
        }
        
        function removeConnections() {
            var connections = instance.getAllConnections();
            while (connections.length > 0) {
                instance.deleteConnection(connections[0], {
                    fireEvent : false
                });
            }
        }
        
        //TODO: create remove entity and attrib connections
    
        window.filterInputModel = function(text, filterPopulated) {
            inputModelFilter = text;
            inputFilterPopulated = filterPopulated;
            rebuildAll();
        };
    
        window.filterOutputModel = function(text, filterPopulated) {
            outputModelFilter = text;
            outputFilterPopulated = filterPopulated;
            rebuildAll();
        };

        function rebuildAll() {
            var xycoord = {left:0,top:0};
            removeConnections();
            removeNodes();
            if (state.relationalInputModel !== null) {
                appendRelationalNodes(mappingDiv, state.relationalInputModel.modelEntities, "src", 10, 10, inputModelFilter, inputFilterPopulated, true);    	
            } else {
                    //TODO: deal with this when we have a component that needs it
            }
            
            if (typeof state.relationalOutputModel !== 'undefined') {
                appendRelationalNodes(mappingDiv, state.relationalOutputModel.modelEntities, "dst", (mappingDiv.clientWidth / 2) + 12, 10,
                        outputModelFilter, outputFilterPopulated, false);	
            } else {
                xycoord.left = (mappingDiv.clientWidth / 2) + 12;
                xycoord.top = 10;
                appendHierarchicalNodes(mappingDiv, state.hierarchicalOutputModel, "dst", xycoord,
                        outputModelFilter, outputFilterPopulated, false);
                
            }
        
            var srcNodes = jsPlumb.getSelector(".mapping-diagram .src");
            if (srcNodes.length > 0) {
                instance.makeSource(srcNodes);
            }
            var dstNodes = jsPlumb.getSelector(".mapping-diagram .dst");
            if (dstNodes.length > 0) {
                instance.makeTarget(dstNodes, {
                    dropOptions : {
                        hoverClass : "dragHover"
                    }
                });
            }
            addConnections();
        }
        
        function removeNodes() {
            while (mappingDiv.childNodes.length > 0) {
                var child = mappingDiv.childNodes[0];
                if (child.classList !== undefined && child.classList.contains("entity")) {
                    instance.remove(mappingDiv.childNodes[0]);
                }
            }
        }
        
        function appendHierarchicalNodes(parentDiv, hierarchicalOutputModel, prefix, xycoord, filterText, filterMapped, src) {
            var rootNode=hierarchicalOutputModel.rootObject;
            addSchemaObject(parentDiv, rootNode, prefix, xycoord);
        }
        
        function addSchemaObject(parentDiv, parentSchemaObject, prefix, xycoord) {
            var lineHeight = 23;
            var key = "";
            var column = "";
            var table = "";
        
            createNode(parentDiv, prefix + parentSchemaObject.id, parentSchemaObject.name, "entity " + prefix, xycoord.left, xycoord.top, table);    
            xycoord.top += lineHeight
            xycoord.left += 10;
            var childObjects = parentSchemaObject.childObjects;
            for (var i = 0; i < childObjects.length; i++) {
                var childObject = childObjects[i];
                addSchemaObject(parentDiv, childObject, prefix, xycoord)
                //xycoord.top += lineHeight;
            }
            xycoord.left -= 10;
        }
        
        function appendRelationalNodes(parentDiv, entities, prefix, left, top, filterText, filterMapped, src) {
            var lineHeight = 23;
            var filteredEntities = [];
            var key = "";
            var column = "";
            var table = "";
            for (var i = 0; i < entities.length; i++) {
                var entity = entities[i];
        
                var mapped = !filterMapped;
                var match = (filterText == "");
                if (match || entity.name.toUpperCase().indexOf(filterText.toUpperCase()) != -1) {
                    match = true
                }
                var attrs = entity.modelAttributes;
                for (var j = 0; j < attrs.length && (!match || !mapped); j++) {
                    var attr = attrs[j];
                    if (match || attr.name.toUpperCase().indexOf(filterText.toUpperCase()) != -1) {
                        match = true;
                    }
                    if (mapped || hasMap(attr, src)) {
                        mapped = true;
                    }
                }
                if (match && mapped) {
                    filteredEntities[filteredEntities.length] = entity;
                }
            }
            for (var i = 0; i < filteredEntities.length; i++, top += lineHeight) {
                var entity = filteredEntities[i];
                var attrs = entity.modelAttributes;
                if (state.relationalOutputModel !== null) {
                        createNode(parentDiv, prefix + entity.id, entity.name, "entity", left, top, table);
                } else {
                        createNode(parentDiv, prefix + entity.id, entity.name, "entity " + prefix, left, top, table);        	
                }
                for (j = 0, top += lineHeight; j < attrs.length; j++, top += lineHeight) {
                    var attr = attrs[j];
                    var icon = column;
                    if (attr.pk) {
                        icon = key;
                    }
                    createNode(parentDiv, prefix + attr.id, attr.name, "entity " + prefix, left + 10, top, icon);
                }
            }
        }
        
        function createNode(parentDiv, id, name, className, left, top, icon) {
            var div = document.createElement("div");
            div.id = id;
            div.style.top = top + "px";
            div.style.left = left + "px";
            div.innerHTML = "<i class=\"v-icon FontAwesome\">" + icon + "</i><span>" + name + "</span>";
            div.className = className;
            div.onmousedown = nodeClick;
            parentDiv.appendChild(div);
        }
        
        function nodeClick(event) {
            if (event.currentTarget.className.indexOf("src") != -1) {
                selectedSrcId = event.currentTarget.id;
                unselectNodes("src");
                unselectAllConnections();
                selectNode(event.currentTarget);
            } else if (event.currentTarget.className.indexOf("dst") != -1) {
                selectedDstId = event.currentTarget.id;
                unselectNodes("dst");
                selectNode(event.currentTarget);
            } else {
                selectedSrcId = selectedDstId = null;
            }
            if (selectedSrcId != null && selectedDstId != null) {
                instance.connect({
                    source : selectedSrcId,
                    target : selectedDstId
                });
                selectedSrcId = selectedDstId = null;
            }
        }
        
        function selectNode(div) {
            if (!div.classList.contains("selected")) {
                div.classList.add("selected");
            }
        }
        
        function unselectAllNodes() {
            unselectNodes("src");
            unselectNodes("dst");
            selectedSrcId = selectedDstId = null;
        }
        
        function unselectNodes(className) {
            var children = mappingDiv.childNodes;
            for ( var child in children) {
                var div = children[child];
                if (div.classList !== undefined && div.classList.contains(className)) {
                    div.classList.remove("selected");
                }
            }
        }
        
        function hasMap(attribute, src) {
            var settings = state.component.modelSettings;
            for (var i = 0; i < settings.length; i++) {
                var setting = settings[i];
                if (setting.name == state.mapsToSchemaObjectName) {
                    // If evaluating source, compare setting id
                    // If evaluating target, compare setting value
                    if ((src && attribute.id == setting.modelObjectId) || (!src && attribute.id == setting.value)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
