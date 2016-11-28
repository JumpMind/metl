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
package org.jumpmind.metl.core.runtime.component.definition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStreamReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.jumpmind.metl.core.plugin.XMLDefinitions;
import org.junit.Test;

public class XMLDefinitionsTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testReadXml() throws Exception {
        JAXBContext jc = JAXBContext.newInstance(XMLDefinitions.class.getPackage().getName());
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("test-components.xml"));
        JAXBElement<XMLDefinitions> root = (JAXBElement<XMLDefinitions>) unmarshaller.unmarshal(reader);
        XMLDefinitions components = root.getValue();
        assertNotNull(components);
        assertEquals("test", components.getComponent().get(0).getName());

    }
}
