window.org_jumpmind_symmetric_is_ui_mapping_MappingDiagram = function() {

	var self = this;
	var state = this.getState();
	
    var instance = jsPlumb.getInstance({
        Endpoint: ["Rectangle", { width: 10, height: 10 }],
        Anchor: [ "Left", "Right" ],
        HoverPaintStyle: {strokeStyle: "orange", lineWidth: 2 },
        ConnectionOverlays: [ [ "Arrow", { location: 1, id: "arrow", length: 12, width: 12, foldback: 1 } ] ],
        Container: "mapping-diagram"
    });
    window.jsp = instance;

	var left = 1;
    var top = 1;
    var entities = state.inputModel.modelEntities;
    var parentDiv = document.getElementById("mapping-diagram");
    		
    for (i = 0; i < entities.length; i++) {
    	var entity = entities[i];
        var entityDiv = document.createElement("div");
        entityDiv.id = "src" + entity.id;
        entityDiv.style.top = top + "em";
        entityDiv.style.left = "1em";
        entityDiv.innerHTML = entity.name;
        entityDiv.className = "entity";
    	parentDiv.appendChild(entityDiv);
    	
    	top += 2;
    	var attrs = entity.modelAttributes;
    	for (j = 0; j < attrs.length; j++) {
    		var attr = attrs[j];
            var attrDiv = document.createElement("div");
            attrDiv.id = "src" + attr.id;
            attrDiv.style.top = top + "em";
            attrDiv.style.left = "3em";
            attrDiv.innerHTML = attr.name;
            attrDiv.className = "entity src";
            parentDiv.appendChild(attrDiv);
    		if (attr.name.length > left) {
    			left = attr.name.length;
    		}
        	top += 2;
    	}
    	top += 2;
    }

    var top = 1;
    left += 5;
    var entities = state.outputModel.modelEntities;
    for (i = 0; i < entities.length; i++) {
    	var entity = entities[i];
        var entityDiv = document.createElement("div");
        entityDiv.id = "dst" + entity.id;
        entityDiv.style.top = top + "em";
        entityDiv.style.left = "30em";
        entityDiv.innerHTML = entity.name;
        entityDiv.className = "entity";
    	parentDiv.appendChild(entityDiv);
    	
    	top += 2;
    	var attrs = entity.modelAttributes;
    	for (j = 0; j < attrs.length; j++) {
    		var attr = attrs[j];
            var attrDiv = document.createElement("div");
            attrDiv.id = "dst" + attr.id;
            attrDiv.style.top = top + "em";
            attrDiv.style.left = "31em";
            attrDiv.innerHTML = attr.name;
            attrDiv.className = "entity dst";
            parentDiv.appendChild(attrDiv);
        	top += 2;
    	}
    	top += 2;
    }

    var src = jsPlumb.getSelector(".mapping-diagram .src");
    var dst = jsPlumb.getSelector(".mapping-diagram .dst");

    instance.batch(function () {
        instance.makeSource(src, {
            anchor: "Continuous",
            connector: [ "StateMachine", { curviness: 1 } ],
            connectorStyle: { strokeStyle: "#5c96bc", lineWidth: 2, outlineColor: "transparent", outlineWidth: 4 }
        });
        
        instance.makeTarget(dst, {
            dropOptions: { hoverClass: "dragHover" },
            anchor: "Continuous",
            maxConnections: 1
        });

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