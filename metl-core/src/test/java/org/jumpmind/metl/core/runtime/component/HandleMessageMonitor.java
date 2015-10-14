package org.jumpmind.metl.core.runtime.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HandleMessageMonitor {
	private int shutdownMessageCount;
	private int startupMessageCount;
	private int sendMessageCount;
	private List<Serializable> payloads = new ArrayList<Serializable>();
	private List<String> targetStepIds = new ArrayList<String>();
	private int expectedPayloadSize;
	private boolean xmlPayload;
	
	public void incrementShutdownMessageCount() {
		this.shutdownMessageCount++;
	}
	
	public void incrementStartupMessageCount() {
		this.startupMessageCount++;
	}
	
	public void incrementSendMessageCount() {
		this.sendMessageCount++;
	}
	
	public int getShutdownMessageCount() {
		return shutdownMessageCount;
	}
	public void setShutdownMessageCount(int shutdownMessageCount) {
		this.shutdownMessageCount = shutdownMessageCount;
	}
	public int getStartupMessageCount() {
		return startupMessageCount;
	}
	public void setStartupMessageCount(int startupMessageCount) {
		this.startupMessageCount = startupMessageCount;
	}
	public int getSendMessageCount() {
		return sendMessageCount;
	}
	public void setSendMessageCount(int sendMessageCount) {
		this.sendMessageCount = sendMessageCount;
	}
	public List<Serializable> getPayloads() {
		return payloads;
	}
	public void setPayloads(List<Serializable> payloads) {
		this.payloads = payloads;
	}
	
	public List<String> getTargetStepIds() {
		return targetStepIds;
	}
	public void setTargetStepIds(List<String> targetStepIds) {
		this.targetStepIds = targetStepIds;
	}

	int getExpectedPayloadSize() {
		return expectedPayloadSize;
	}

	void setExpectedPayloadSize(int expectedPayloadSize) {
		this.expectedPayloadSize = expectedPayloadSize;
	}

	boolean isXmlPayload() {
		return xmlPayload;
	}

	void setXmlPayload(boolean xmlPayload) {
		this.xmlPayload = xmlPayload;
	}
	
	
	
	
}
