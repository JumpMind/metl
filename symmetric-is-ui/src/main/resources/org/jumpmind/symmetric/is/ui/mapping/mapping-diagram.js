window.org_jumpmind_symmetric_is_ui_mapping_MappingDiagram = function() {
	self = this;
	state = this.getState();

    parentDiv = document.getElementById("mapping-diagram");
    parentDiv.parentNode.addEventListener("click", function(event) {
    	if (event.target.tagName == "DIV") {
    		unselectAll();
    		self.onSelect();
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
        self.onConnection({
            "sourceId" : info.connection.sourceId,
            "targetId" : info.connection.targetId,
            "removed" : false
        });
    });

    instance.bind("connectionDetached", function(info, originalEvent) {
        self.onConnection({
            "sourceId" : info.connection.sourceId,
            "targetId" : info.connection.targetId,
            "removed" : true
        });
    });

    instance.bind("connectionMoved", function(info, originalEvent) {
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
    	unselectAll();
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

    unselectAll = function() {
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
        	{ dropOptions: { hoverClass: "dragHover" }, maxConnections: 1 });

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
	parentDiv.appendChild(div);
}
