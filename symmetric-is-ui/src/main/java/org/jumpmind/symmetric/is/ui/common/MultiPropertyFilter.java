package org.jumpmind.symmetric.is.ui.common;

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
