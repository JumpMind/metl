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
package org.jumpmind.metl.ui.common;

import org.apache.commons.lang.StringUtils;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;

/**
 * Custom Vaadin filter for filtering on more than one container item property.
 */
public class MultiPropertyFilter implements Container.Filter {
    private static final long serialVersionUID = 1L;
    
    protected String text;
    String[] properties;
    
    public MultiPropertyFilter(String text, String... properties) {
        this.text = text;
        this.properties = properties;
    }

    @SuppressWarnings("rawtypes")
    public boolean passesFilter(Object itemId, Item item) throws UnsupportedOperationException {
        for (String property : properties) {
			Property prop = item.getItemProperty(property);
        	if (prop != null) {
        		String value = null;
        		if (prop.getValue() != null) {
        			value = prop.getValue().toString();
        		}
	            if (StringUtils.containsIgnoreCase(value, text)) {
	                return true;
	            }
        	} else {
        		throw new RuntimeException("Property " + property + " does not exist in item, valid properties are: " + item.getItemPropertyIds());
        	}
        }
        return false;
    }

    public boolean appliesToProperty(Object propertyId) {
        for (String property : properties) {
            if (property.equals(propertyId)) {
                return true;
            }
        }
        return false;
    }

}
