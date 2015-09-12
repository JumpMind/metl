package org.jumpmind.symmetric.is.core.util;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.runtime.EntityData;

public class ComponentUtil {

    public static Object getAttributeValue(Model model, List<EntityData> rows, String entityName, String attributeName) {
        List<Object> values = getAttributeValues(model, rows, entityName, attributeName);
        if (values.size() > 0) {
            return values.get(0);
        }
        return null;
    }

    public static List<Object> getAttributeValues(Model model, List<EntityData> rows, String entityName, String attributeName) {
        List<Object> values = new ArrayList<Object>();
        if (model != null && rows != null) {
            ModelAttribute attribute = model.getAttributeByName(entityName, attributeName);
            if (attribute != null) {
                for (EntityData data : rows) {
                    if (data.containsKey(attribute.getId())) {
                        values.add(data.get(attribute.getId()));
                    }
                }
            }
        }
        return values;
    }

}
