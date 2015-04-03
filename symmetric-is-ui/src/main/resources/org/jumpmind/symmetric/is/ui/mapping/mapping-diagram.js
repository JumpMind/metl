window.org_jumpmind_symmetric_is_ui_mapping_MappingDiagram = function() {
	self = this;
	state = this.getState();

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
    
    window.jsp = instance;
    entities = state.inputModel.modelEntities;
    parentDiv = document.getElementById("mapping-diagram");
    parentDiv.parentNode.addEventListener("click", function(event) {
    	if (event.target.tagName == "DIV") {
    		unselectAll();
    		self.onSelect();
    	}
    }, false);

    widest = appendChildren(parentDiv, state.inputModel.modelEntities, "src", 1, 1);
    appendChildren(parentDiv, state.outputModel.modelEntities, "dst", (widest / 2) + 10, 1)

    settings = state.component.attributeSettings;
    for (i = 0; i < settings.length; i++) {
    	setting = settings[i];
    	if (setting.name == state.mapsToAttrName) {
            instance.connect({ source: "src" + setting.attributeId, target: "dst" + setting.value});
    	}
    }
    
    this.layoutAll = function() {
        instance.makeSource(jsPlumb.getSelector(".mapping-diagram .src"));
        
        instance.makeTarget(jsPlumb.getSelector(".mapping-diagram .dst"),
        	{ dropOptions: { hoverClass: "dragHover" }, maxConnections: 1 });

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
            connection = instance.connect({ source: connection.sourceId, target: connection.targetId, fireEvent: false });
            connection.toggleType("selected");
            for (i = 0; i < connection.endpoints.length; i++) {
            	connection.endpoints[i].toggleType("selected");
            }
            self.onSelect({
                "sourceId" : connection.sourceId,
                "targetId" : connection.targetId
            });
        });
    };   
    
    unselectAll = function() {
        connections = instance.getAllConnections();
        for (i = 0; i < connections.length; i++) {
            connections[i].removeType("selected");
            for (j = 0; j < connections[i].endpoints.length; j++) {
            	connections[i].endpoints[j].removeType("selected");
            }
        }
    }
    
    this.onStateChange = function() {
        instance.batch(function() {
            self.layoutAll();
        });
    };
    
    instance.bind("ready", function() {
        instance.batch(function() {
            self.layoutAll();
        });
    });
}

function appendChildren(parentDiv, entities, prefix, x, y) {
	widest = 0;
    for (i = 0; i < entities.length; i++, y += 2) {
    	entity = entities[i];
    	createDiv(parentDiv, prefix + entity.id, entity.name, "entity", x, y);    	
    	attrs = entity.modelAttributes;
    	for (j = 0, y += 2; j < attrs.length; j++, y += 2) {
    		attr = attrs[j];
    		createDiv(parentDiv, prefix + attr.id, attr.name, "entity " + prefix, x + 1, y);
    		if (attr.name.length > widest) {
    			widest = attr.name.length;
    		}
    	}
    }
    return widest;
}

function createDiv(parentDiv, id, name, className, x, y) {
    div = document.createElement("div");
    div.id = id;
    div.style.top = y + "em";
    div.style.left = x + "em";
    div.innerHTML = name;
    div.className = className;
	parentDiv.appendChild(div);
}
