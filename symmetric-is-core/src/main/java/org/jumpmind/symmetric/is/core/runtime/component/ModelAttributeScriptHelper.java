package org.jumpmind.symmetric.is.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.util.FormatUtils;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

public class ModelAttributeScriptHelper {

    Object value;
    
    EntityData data;
    
    ModelAttribute attribute;
    
    ModelEntity entity;
    
    public static final RemoveAttribute REMOVE_ATTRIBUTE = new RemoveAttribute();
    
    static private ThreadLocal<ScriptEngine> scriptEngine = new ThreadLocal<ScriptEngine>();

    public ModelAttributeScriptHelper(ModelAttribute attribute, ModelEntity entity, EntityData data, Object value) {
        this.value = value;
        this.data = data;
        this.attribute = attribute;
        this.entity = entity;
    }
    
    public Object nullvalue() {
        return null;
    }
    
    public Integer integer() {
        String text = value != null ? value.toString() : "";
        return Integer.parseInt(text);
    }
    
    public String abbreviate(int maxwidth) {
        String text = value != null ? value.toString() : "";
        return StringUtils.abbreviate(text, maxwidth);
    }

    public String left(int length) {
        return StringUtils.left(value != null ? value.toString() : "", length);
    }

    public String right(int length) {
        return StringUtils.right(value != null ? value.toString() : "", length);
    }

    public String rpad(String padChar, int length) {
        String text = value != null ? value.toString() : "";
        return StringUtils.rightPad(text, length, padChar);
    }

    public String lpad(String padChar, int length) {
        String text = value != null ? value.toString() : "";
        return StringUtils.leftPad(text, length, padChar);
    }

    public String substr(int start, int end) {
        String text = value != null ? value.toString() : "";
        return StringUtils.substring(text, start, end);
    }

    public String lower() {
        String text = value != null ? value.toString() : "";
        return StringUtils.lowerCase(text);
    }

    public String upper() {
        String text = value != null ? value.toString() : "";
        return StringUtils.upperCase(text);
    }

    public String trim() {
        String text = value != null ? value.toString() : "";
        return StringUtils.trim(text);
    }

    public String format(String spec) {
        return String.format(spec, value);
    }

    public String replace(String searchString, String replacement) {
        String text = value != null ? value.toString() : "";
        return StringUtils.replace(text, searchString, replacement);
    }
    
    public Date currentdate() {
        return new Date();
    }
    
    public RemoveAttribute remove() {
        return REMOVE_ATTRIBUTE;
    }
    
    public Date parsedate(String pattern, String nulldate) {
        String text = value != null ? value.toString() : "";
        if (isNotBlank(text) && !text.equals(nulldate)) {
            return FormatUtils.parseDate(text, new String[] { pattern});
        } else {
            return null;
        }
    }
    
    public Date parsedate(String pattern) {
        String text = value != null ? value.toString() : "";
        if (isNotBlank(text)) {
            return FormatUtils.parseDate(text, new String[] { pattern});
        } else {
            return null;
        }
    }
    
    public String formatdate(String pattern) {
        if (value instanceof Date) {
            FastDateFormat formatter = FastDateFormat.getInstance(pattern);
            return formatter.format((Date) value);
        } else if (value != null) {
            return "Not a datetime";
        } else {
            return "";
        }
    }

    public String stringConstant(String value) {
        return value;
    }
    
    protected Object eval() {
        return value;
    }

    public static String[] getSignatures() {
        List<String> signatures = new ArrayList<String>();
        Method[] methods = ModelAttributeScriptHelper.class.getMethods();
        LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
        for (Method method : methods) {
            if (method.getDeclaringClass().equals(ModelAttributeScriptHelper.class)
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())) {
                StringBuilder sig = new StringBuilder(method.getName());
                sig.append("(");
                String[] names = discoverer.getParameterNames(method);
                for (String name : names) {
                    sig.append(name);
                    sig.append(",");

                }
                if (names.length > 0) {
                    sig.replace(sig.length() - 1, sig.length(), ")");
                } else {
                    sig.append(")");
                }
                signatures.add(sig.toString());
            }
        }
        Collections.sort(signatures);
        return signatures.toArray(new String[signatures.size()]);
    }

    public static Object eval(ModelAttribute attribute, Object value, ModelEntity entity, EntityData data, String expression) {
        ScriptEngine engine = scriptEngine.get();
        if (engine == null) {
            ScriptEngineManager factory = new ScriptEngineManager();
            engine = factory.getEngineByName("groovy");
            scriptEngine.set(engine);
        }

        engine.put("value", value);
        engine.put("data", data);
        engine.put("entity", entity);
        engine.put("attribute", attribute);

        try {
            String importString = "import org.jumpmind.symmetric.is.core.runtime.component.ModelAttributeScriptHelper;\n";
            String code = String.format(
                    "return new ModelAttributeScriptHelper(attribute, entity, data, value) { public Object eval() { return %s } }.eval()",
                    expression);
            return engine.eval(importString + code);
        } catch (ScriptException e) {
            throw new RuntimeException("Unable to evaluate groovy script", e);
        }
    }
    
    static class RemoveAttribute {
        
    }

}
