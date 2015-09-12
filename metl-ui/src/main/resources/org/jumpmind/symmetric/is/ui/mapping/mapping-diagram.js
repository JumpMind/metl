window.org_jumpmind_metl_ui_mapping_MappingDiagram = function() {
	self = this;
	state = this.getState();
    selectedSrcId = null;
    selectedDstId = null;
    inputModelFilter = null;
    outputModelFilter = null;

    mappingDiv = document.getElementById("mapping-diagram");
    scrollDiv = mappingDiv.parentNode;
    topDiv = scrollDiv.parentNode;    

    scrollDiv.parentNode.addEventListener("click", function(event) {
    	if (event.target.tagName == "DIV") {
    		unselectAllConnections();
    		self.onSelect();
    	}
    	if (event.target.classList !== undefined && !event.target.classList.contains("dst")) {
			selectedSrcId = selectedDstId = null;
			unselectAllNodes();			
		}
    }, false);

    instance = jsPlumb.getInstance({
    	Endpoint: ["Dot", { radius: 7 }],
    	EndpointStyle: { fillStyle: "#0072C6" }, 
        Anchor: [ "Left", "Right" ],
    	Connector: [ "Straight", { gap: 7 } ],
        ConnectionOverlays: [ [ "Arrow", { location: 1, width: 12, length: 12, foldback: 1 } ] ],
        PaintStyle: { lineWidth : 2, strokeStyle : "#0072C6", outlineColor: "white", outlineWidth: 2 },
        Container: "mapping-diagram"
    });

    instance.registerConnectionType("selected", { paintStyle : { strokeStyle : "orange" } });
    instance.registerEndpointType("selected", { paintStyle : { fillStyle: "orange" } });

    instance.bind("connection", function(info, originalEvent) {
    	unselectAllNodes();
        self.onConnection({
            "sourceId" : info.connection.sourceId,
            "targetId" : info.connection.targetId,
            "removed" : false
        });
    });

    instance.bind("connectionDetached", function(info, originalEvent) {
    	unselectAllNodes();
        self.onConnection({
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
    	self.onConnection({
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
    	instance.detach(connection, { fireEvent: false });
        var connection = instance.connect({ source: connection.sourceId, target: connection.targetId, fireEvent: false });
        connection.toggleType("selected");
        for (var i = 0; i < connection.endpoints.length; i++) {
        	connection.endpoints[i].toggleType("selected");
        }
        self.onSelect({
            "sourceId" : connection.sourceId,
            "targetId" : connection.targetId
        });
    });

    instance.bind("connectionDrag", function(connection) {
    	dragConnection = connection;
    	console.log("drag start");
    });
    
    unselectAllConnections = function() {
        var connections = instance.getAllConnections();
        for (var i = 0; i < connections.length; i++) {
            connections[i].removeType("selected");
            for (var j = 0; j < connections[i].endpoints.length; j++) {
            	connections[i].endpoints[j].removeType("selected");
            }
        }
    }

    addConnections = function() {
        var settings = state.component.attributeSettings;
        for (var i = 0; i < settings.length; i++) {
        	var setting = settings[i];
        	if (setting.name == state.mapsToAttrName) {
                instance.connect({ source: "src" + setting.attributeId, target: "dst" + setting.value, fireEvent: false });
        	}
        }
    }

    removeConnections = function() {
        var connections = instance.getAllConnections();
        while (connections.length > 0) {
        	instance.detach(connections[0], { fireEvent: false });            	
        }
    }

    this.onStateChange = function() {
    	instance.batch(function() {
    		removeConnections();
            addConnections();
    	});
    };
    
    this.filterInputModel = function(text) {
    	inputModelFilter = text;
    	rebuildAll();
    };

    this.filterOutputModel = function(text) {
    	outputModelFilter = text;
    	rebuildAll();
    };

    instance.ready(function () {
    	rebuildAll();
    });
}

function rebuildAll() {
	removeConnections();
	removeNodes();
	appendNodes(mappingDiv, state.inputModel.modelEntities, "src", 10, 10, inputModelFilter);
    appendNodes(mappingDiv, state.outputModel.modelEntities, "dst", (mappingDiv.clientWidth / 2) + 12, 10, outputModelFilter);

    var srcNodes = jsPlumb.getSelector(".mapping-diagram .src");
    if (srcNodes.length > 0) {
    	instance.makeSource(srcNodes);
    }
    var dstNodes = jsPlumb.getSelector(".mapping-diagram .dst");
    if (dstNodes.length > 0) {
    	instance.makeTarget(dstNodes, { dropOptions: { hoverClass: "dragHover" }});
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

function appendNodes(parentDiv, entities, prefix, left, top, filterText) {
	var lineHeight = 23;
	var filteredEntities = [];
    for (var i = 0; i < entities.length; i++) {
    	var entity = entities[i];
    	if (filterText == null || entity.name.toUpperCase().indexOf(filterText.toUpperCase()) != -1) {
    		filteredEntities[filteredEntities.length] = entity;
    	} else {
    		var attrs = entity.modelAttributes;
        	for (var j = 0; j < attrs.length; j++) {
        		var attr = attrs[j];
        		if (filterText == null || attr.name.toUpperCase().indexOf(filterText.toUpperCase()) != -1) {
        			filteredEntities[filteredEntities.length] = entity;
        			break;
        		}
        	}
    	}
    }
	for (var i = 0; i < filteredEntities.length; i++, top += lineHeight) {
    	var entity = filteredEntities[i];
    	var attrs = entity.modelAttributes;
    	createNode(parentDiv, prefix + entity.id, entity.name, "entity", left, top);
    	for (j = 0, top += lineHeight; j < attrs.length; j++, top += lineHeight) {
    		var attr = attrs[j];
    		createNode(parentDiv, prefix + attr.id, attr.name, "entity " + prefix, left + 10, top);
    	}
    }
}

function createNode(parentDiv, id, name, className, left, top) {
    var div = document.createElement("div");
    div.id = id;
    div.style.top = top + "px";
    div.style.left = left + "px";
    div.innerHTML = name;
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
		instance.connect({ source: selectedSrcId, target: selectedDstId });
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
	for (var child in children) {
		var div = children[child];
		if (div.classList !== undefined && div.classList.contains(className)) {
			div.classList.remove("selected");
		}
	}
}
