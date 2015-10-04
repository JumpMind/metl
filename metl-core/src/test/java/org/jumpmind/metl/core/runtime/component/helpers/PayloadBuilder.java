package org.jumpmind.metl.core.runtime.component.helpers;

import java.util.ArrayList;

import org.jumpmind.metl.core.runtime.EntityData;

public class PayloadBuilder {
	ArrayList<EntityData> payload = new ArrayList<>();
	ArrayList<String> payloadString = new ArrayList<>();
	
	public PayloadBuilder() {
	}
	
	public PayloadBuilder addRow(EntityData data) {
		this.payload.add(data);
		return this;
	}
	
	public PayloadBuilder addRow(String data) {
		this.payloadString.add(data);
		return this;
	}
	
	public ArrayList<EntityData> buildED() {
		return this.payload;
	}
	
	public ArrayList<String> buildString() {
		return this.payloadString;
	}
}
