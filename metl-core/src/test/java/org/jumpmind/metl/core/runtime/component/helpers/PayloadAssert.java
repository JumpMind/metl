/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.runtime.component.helpers;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.utils.TestUtils;
import org.junit.Assert;

public class PayloadAssert extends Assert {
	
	public static void assertPayload(int messageNumber, Serializable expected, Serializable actual, boolean isPayloadXML) {
		TestUtils.assertNullNotNull(expected, actual);
		if (expected != null && actual != null) {
			if (expected instanceof List && actual instanceof List) {
				
				assertEquals("Payloads are not the same size [message " + messageNumber, ((List) expected).size(), ((List) actual).size());
				
				if (((List) expected).size() > 0 && ((List) actual).size() > 0) {
					
					// Check for EntityData list
					if (((List) expected).get(0) instanceof EntityData && ((List) actual).get(0) instanceof EntityData) {
						
						for (int i = 0; i < ((List) expected).size(); i++) {
							EntityData expectedData = (EntityData) ((List) expected).get(i);
							EntityData actualData = (EntityData) ((List) actual).get(i);
							
							for (String key : expectedData.keySet()) {
								assertTrue(printPayloadKeyNotFound(key, messageNumber, i+1, (List)actual), 
										actualData.containsKey(key));
								
								// TODO attempting to shortcut value assert with assertEquals may need more detailed asserts.
								assertEquals(printPayloadValuesNotEqual(messageNumber, i+1, key, (List)actual), 
										expectedData.get(key), actualData.get(key));
							}
						}
					}
					
					// Check for String list
					else if (((List) expected).get(0) instanceof String && ((List) actual).get(0) instanceof String) {
						TestUtils.assertList((List)expected, (List)actual, isPayloadXML);
					}
					
					// Oops not supported yet
					else {
						fail("Payload type not supported with testing yet [message " + messageNumber);
					}
				}
				
				
			}
			else if (expected instanceof List && !(actual instanceof List)) {
				fail("Expected payload was a List but actual payload was not a List");
			}
			else {
				// TODO future support for a payload that is not a list
			}
		}
	}
	
	public static String printPayloadKeyNotFound(String key, int messageNumber, int row, List<EntityData> payload) {
		StringBuffer sb = new StringBuffer();
		sb.append("Actual key[")
			.append(key)
			.append("] not found in message [")
			.append(messageNumber)
			.append("] row[")
			.append(row)
			.append("]");
		sb.append("\n");
		sb.append(printPayload(payload));
		return sb.toString();
	}
	
	public static String printPayloadValuesNotEqual(int messageNumber, int row, String key, List<EntityData> payload) {
		StringBuffer sb = new StringBuffer();
		sb.append("Values do not match in message [")
			.append(messageNumber)
			.append("] row[")
			.append(row)
			.append("] for key[")
			.append(key)
			.append("]");
		sb.append("\n");
		sb.append(printPayload(payload));
		return sb.toString();
	}
	
	public static String printPayload(List<EntityData> payload) {
		StringBuffer sb = new StringBuffer();
		sb.append("PAYLOAD\n");
		
		int row = 1;
		for (EntityData entity : payload) {
			sb.append("\nRow ").append(row).append(" - ");
			for (Map.Entry<String, Object> entry :  entity.entrySet()) {
				sb.append("[").append(entry.getKey()).append(",").append(entry.getValue()).append("] ");
			}
			row++;
		}
		sb.append("\n");
		return sb.toString();
		
	}
}
