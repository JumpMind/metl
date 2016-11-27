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
package org.jumpmind.metl.core.plugin;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


@XmlRegistry
public class ObjectFactory {

    private final static QName _Components_QNAME = new QName("", "definitions");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.jumpmind.metl.core.model.xml
     * 
     */
    public ObjectFactory() {
    }

    public XMLComponentDefinition createXmlComponent() {
        return new XMLComponentDefinition();
    }

    @XmlElementDecl(namespace="", name = "definitions")
    public JAXBElement<XMLDefinitions> createConfig(XMLDefinitions value) {
        return new JAXBElement<XMLDefinitions>(_Components_QNAME, XMLDefinitions.class, null, value);
    }

}
