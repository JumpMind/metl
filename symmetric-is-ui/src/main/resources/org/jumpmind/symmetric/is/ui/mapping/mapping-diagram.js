window.org_jumpmind_symmetric_is_ui_mapping_MappingDiagram = function() {
	self = this;
	state = this.getState();

    parentDiv = document.getElementById("mapping-diagram");

    selectedDstId = null;
    selectedSrcId = null;
    
    isScrolling = false;
    scrollMultiple = 1;
    scrollDiv = parentDiv.parentNode.parentNode.parentNode.parentNode;
    parentDiv.addEventListener("mousemove", function(event) {
    	if (event.pageY < 100) startScrolling(-60);
    	else if (event.pageY < 125) startScrolling(-40);
    	else if (event.pageY < 150) startScrolling(-20);
    	else isScrolling = false;
    }, false);

    scrollDiv.parentNode.addEventListener("click", function(event) {
    	if (event.target.tagName == "DIV") {
    		unselectAllConnections();
    		self.onSelect();
    	}
    	if (event.target.classList !== undefined && !event.target.classList.contains("dst")) {
			selectedSrcId = selectedDstId = null;
			unselectAllDivs();			
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
    	unselectAllDivs();
        self.onConnection({
            "sourceId" : info.connection.sourceId,
            "targetId" : info.connection.targetId,
            "removed" : false
        });
    });

    instance.bind("connectionDetached", function(info, originalEvent) {
    	unselectAllDivs();
        self.onConnection({
            "sourceId" : info.connection.sourceId,
            "targetId" : info.connection.targetId,
            "removed" : true
        });
    });

    instance.bind("connectionMoved", function(info, originalEvent) {
    	unselectAllDivs();
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

    this.onStateChange = function() {
    	instance.batch(function() {
            var connections = instance.getAllConnections();
            while (connections.length > 0) {
            	instance.detach(connections[0], { fireEvent: false });            	
            }
            addConnections();
    	});
    	instance.repaintEverything();
    };

    instance.ready(function () {
        var widest = appendChildren(parentDiv, state.inputModel.modelEntities, "src", 1, 1);
        appendChildren(parentDiv, state.outputModel.modelEntities, "dst", (widest / 2) + 10, 1)
        addConnections();

        instance.makeSource(jsPlumb.getSelector(".mapping-diagram .src"));
        
        instance.makeTarget(jsPlumb.getSelector(".mapping-diagram .dst"),
        	{ dropOptions: { hoverClass: "dragHover" }});
    });
}

function appendChildren(parentDiv, entities, prefix, x, y) {
	var widest = 0;
    for (var i = 0; i < entities.length; i++, y += 2) {
    	var entity = entities[i];
    	createDiv(parentDiv, prefix + entity.id, entity.name, "entity", x, y);    	
    	attrs = entity.modelAttributes;
    	for (j = 0, y += 2; j < attrs.length; j++, y += 2) {
    		var attr = attrs[j];
    		createDiv(parentDiv, prefix + attr.id, attr.name, "entity " + prefix, x + 1, y);
    		if (attr.name.length > widest) {
    			widest = attr.name.length;
    		}
    	}
    }
    return widest;
}

function createDiv(parentDiv, id, name, className, x, y) {
    var div = document.createElement("div");
    div.id = id;
    div.style.top = y + "em";
    div.style.left = x + "em";
    div.innerHTML = name;
    div.className = className;
    div.onmousedown = divClick;
	parentDiv.appendChild(div);
}

function divClick(event) {
	if (event.currentTarget.className.indexOf("src") != -1) {
		selectedSrcId = event.currentTarget.id;
		unselectDivs("src");
		unselectAllConnections();
		selectDiv(event.currentTarget);
	} else if (event.currentTarget.className.indexOf("dst") != -1) {
		selectedDstId = event.currentTarget.id;
		unselectDivs("dst");
		selectDiv(event.currentTarget);
	} else {
		selectedSrcId = selectedDstId = null;
	}
	if (selectedSrcId != null && selectedDstId != null) {
		instance.connect({ source: selectedSrcId, target: selectedDstId });
		selectedSrcId = selectedDstId = null;
	}
}

function selectDiv(div) {
	if (!div.classList.contains("selected")) {
		div.classList.add("selected");
	}
}

function unselectAllDivs() {
	unselectDivs("src");
	unselectDivs("dst");
}

function unselectDivs(className) {
	var children = parentDiv.childNodes;
	for (var child in children) {
		var div = children[child];
		if (div.classList !== undefined && div.classList.contains(className)) {
			div.classList.remove("selected");
		}
	}
}

function startScrolling(multiple) {
	scrollMultiple = multiple
	if (!isScrolling) {
		isScrolling = true;
		scrolling();		
	}
}

function scrolling() {
	var scrollY = scrollDiv.scrollTop;
	if (isScrolling) {
		if (scrollMultiple < 0 && scrollY > 0) {
			scrollY += (1 * scrollMultiple);
			scrollDiv.scrollTop = scrollY;
			setTimeout(scrolling, 100);
		} else if (scrollMultiple > 0) {
			scrollY += (1 * scrollMultiple);
			scrollDiv.scrollTop = scrollY;
			setTimeout(scrolling, 100);
		}
	}
}
