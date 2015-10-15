package org.jumpmind.metl.core.runtime.component.helpers;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.utils.TestUtils;
import org.junit.Assert;

public class MessageAssert extends Assert {
	
	public static void assertMessage(int messageNumber, Message expected, Message actual, boolean isPayloadXML) {
		TestUtils.assertNullNotNull(expected, actual);
		if (expected != null && actual != null) {
			// TODO Assert Message Header
			
			PayloadAssert.assertPayload(messageNumber, expected.getPayload(), actual.getPayload(), isPayloadXML);
		}
	}
}
