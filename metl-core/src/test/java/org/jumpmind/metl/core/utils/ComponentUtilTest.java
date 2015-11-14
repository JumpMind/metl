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
package org.jumpmind.metl.core.utils;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.util.ComponentUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class ComponentUtilTest {
	
	@Test
	public void testGetPayloadTypeNonList() {
		assertEquals(ComponentUtil.PAYLOAD_TYPE_UNKNOWN, ComponentUtil.getPayloadType("test"));
	}
	
	@Test
	public void testGetPayloadTypeListNotSupported() {
		Serializable intList = new ArrayList<Integer>();
		assertEquals(ComponentUtil.PAYLOAD_TYPE_UNKNOWN, ComponentUtil.getPayloadType(intList));
	}
	
	@Test
	public void testGetPayloadTypeListEntity() {
		 Serializable entityList = new ArrayList<EntityData>();
		 ((List<EntityData>)entityList).add(new EntityDataBuilder().withKV("k", "v").build());
		assertEquals(ComponentUtil.PAYLOAD_TYPE_LIST_ENTITY, ComponentUtil.getPayloadType(entityList));
	}
	
	@Test
	public void testGetPayloadTypeListString() {
		 Serializable stringList = new ArrayList<String>();
		 ((List<String>)stringList).add("test");
		assertEquals(ComponentUtil.PAYLOAD_TYPE_LIST_STRING, ComponentUtil.getPayloadType(stringList));
	}
}
