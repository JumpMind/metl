window.org_jumpmind_symmetric_is_ui_mapping_MappingDiagram = function() {
	self = this;
	state = this.getState();
	
    instance = jsPlumb.getInstance({
        Endpoint: ["Rectangle", { width: 10, height: 10 }],
        Anchor: [ "Left", "Right" ],
        HoverPaintStyle: {strokeStyle: "orange", lineWidth: 2 },
        Connector: [ "StateMachine", { curviness: 1 } ],
        ConnectionOverlays: [ [ "Arrow", { location: 1, id: "arrow", length: 12, width: 12, foldback: 1 } ] ],
        Container: "mapping-diagram"
    });

    window.jsp = instance;
    entities = state.inputModel.modelEntities;
    parentDiv = document.getElementById("mapping-diagram");

    widest = appendChildren(parentDiv, state.inputModel.modelEntities, "src", 1, 1);
    appendChildren(parentDiv, state.outputModel.modelEntities, "dst", (widest / 2) + 10, 1)

    settings = state.component.attributeSettings;
    for (i = 0; i < settings.length; i++) {
    	setting = settings[i];
    	if (setting.name == state.mapsToAttrName) {
            instance.connect({ source: "src" + setting.attributeId, target: "dst" + setting.value });
    	}
    }

    instance.batch(function () {
        instance.makeSource(jsPlumb.getSelector(".mapping-diagram .src"),
        	{ connectorStyle: { strokeStyle: "#5c96bc", lineWidth: 2, outlineColor: "transparent", outlineWidth: 4 } });
        
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
    });
}

function appendChildren(parentDiv, entities, prefix, x, y) {
	widest = 0;
    for (i = 0; i < entities.length; i++, y += 2) {
    	entity = entities[i];
        entityDiv = document.createElement("div");
        entityDiv.id = "src" + entity.id;
        entityDiv.style.top = y + "em";
        entityDiv.style.left = x + "em";
        entityDiv.innerHTML = entity.name;
        entityDiv.className = "entity";
    	parentDiv.appendChild(entityDiv);
    	
    	attrs = entity.modelAttributes;
    	for (j = 0, y += 2; j < attrs.length; j++, y += 2) {
    		attr = attrs[j];
            attrDiv = document.createElement("div");
            attrDiv.id = prefix + attr.id;
            attrDiv.style.top = y + "em";
            attrDiv.style.left = (x + 1) + "em";
            attrDiv.innerHTML = attr.name;
            attrDiv.className = "entity " + prefix;
            parentDiv.appendChild(attrDiv);
    		if (attr.name.length > widest) {
    			widest = attr.name.length;
    		}
    	}
    }
    return widest;
}
