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
package org.jumpmind.metl.core.runtime.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;
import org.jdom2.output.XMLOutputter;
import org.jumpmind.properties.TypedProperties;

abstract public class AbstractXMLComponentRuntime extends AbstractComponentRuntime {

    public final static String IGNORE_NAMESPACE = "xml.formatter.ignore.namespace";

    public final static String XML_FORMATTER_XPATH = "xml.formatter.xpath";

    boolean ignoreNamespace = true;
    
    protected String toXML(Element element) {
        XMLOutputter xmlOutputter = new XMLOutputter();
        return xmlOutputter.outputString(element);
    }
    

    @Override
    public void start() {
        TypedProperties properties = getTypedProperties();
        ignoreNamespace = properties.is(IGNORE_NAMESPACE);

    }

    protected Map<Element, Namespace> removeNamespaces(Document document) {
        Map<Element, Namespace> namespaces = new HashMap<Element, Namespace>();
        if (ignoreNamespace && document.hasRootElement()) {
            namespaces.put(document.getRootElement(), document.getRootElement().getNamespace());
            document.getRootElement().setNamespace(null);
            for (Element el : document.getRootElement().getDescendants(new ElementFilter())) {
                Namespace nsp = el.getNamespace();
                if (nsp != null) {
                    el.setNamespace(null);
                    namespaces.put(el, nsp);
                }
            }
        }
        return namespaces;
    }
    
    protected String removeNameSpace(String name) {
        if (ignoreNamespace && name != null) {
            name = name.substring(name.lastIndexOf(':')+1);
        }
        return name;
    }

    protected void restoreNamespaces(Document document, Map<Element, Namespace> namespaces) {
        if (ignoreNamespace) {
            Set<Element> elements = namespaces.keySet();
            for (Element element : elements) {
                element.setNamespace(namespaces.get(element));
            }
        }
    }

}
