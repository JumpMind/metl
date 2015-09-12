package org.jumpmind.metl.core.runtime.component;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.jumpmind.metl.core.runtime.component.ModelAttributeScriptHelper;
import org.junit.Test;

public class ModelAttributeScriptHelperTest {

    @Test
    public void testParsedateWithNullValue() {
        ModelAttributeScriptHelper helper = new ModelAttributeScriptHelper(null, null, null, "0000-00-01");
        assertNull(helper.parsedate("yyyy-MM-dd", "0000-00-01"));
        
        helper = new ModelAttributeScriptHelper(null, null, null, "2015-01-01");
        assertNotNull(helper.parsedate("yyyy-MM-dd", "0000-00-01"));

    }
}
