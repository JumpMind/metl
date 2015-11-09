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

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.utils.TestUtils;
import org.junit.Assert;

public class MessageAssert extends Assert {
	
	public static void assertMessage(int callbackNumber, int messageNumber, Message expected, Message actual, boolean isPayloadXML) {
		TestUtils.assertNullNotNull(expected, actual);
		if (expected != null && actual != null) {
			// TODO Assert Message Header
			
			PayloadAssert.assertPayload(callbackNumber, messageNumber, expected.getPayload(), actual.getPayload(), isPayloadXML);
		}
	}
}
