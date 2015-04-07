package org.jumpmind.symmetric.is.core.runtime.component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang.StringUtils;

public class Transformer {

    Object value;

    public Transformer(Object value) {
        this.value = value;
    }

    public String left(int length) {
        return StringUtils.left(value != null ? value.toString() : "", length);
    }

    protected Object eval() {
        return value;
    }

    public static String[] getSignatures() {
        List<String> signatures = new ArrayList<String>();
        Method[] methods = Transformer.class.getMethods();
        for (Method method : methods) {
            if (method.getDeclaringClass().equals(Transformer.class)
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())) {
                StringBuilder sig = new StringBuilder(method.getName());
                sig.append("(");
                Class<?>[] params = method.getParameterTypes();
                for (Class<?> class1 : params) {
                    sig.append(class1.getName());
                    sig.append(",");
                }
                if (params.length > 0) {
                    sig.replace(sig.length() - 1, sig.length(), ")");
                } else {
                    sig.append(")");
                }
                signatures.add(sig.toString());
            }
        }
        return signatures.toArray(new String[signatures.size()]);
    }

    public static Object eval(Object value, String expression) {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("groovy");
        if (engine == null) {
            throw new UnsupportedOperationException("Script type 'groovy' is not supported");
        }

        engine.put("value", value);

        try {
            String importString = "import org.jumpmind.symmetric.is.core.runtime.component.Transformer;\n";
            String code = String.format(
                    "return new Transformer(value) { public Object eval() { return %s } }.eval()",
                    expression);
            return engine.eval(importString + code);
        } catch (ScriptException e) {
            throw new RuntimeException("Unable to evaluate groovy script", e);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(Arrays.toString(Transformer.getSignatures()));
        System.out.println(Transformer.eval("abcd", "left(3)"));
    }

}
