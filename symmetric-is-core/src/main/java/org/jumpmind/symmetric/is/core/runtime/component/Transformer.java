package org.jumpmind.symmetric.is.core.runtime.component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

public class Transformer {

    Object value;

    static private ThreadLocal<ScriptEngine> scriptEngine = new ThreadLocal<ScriptEngine>();

    public Transformer(Object value) {
        this.value = value;
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

    protected Object eval() {
        return value;
    }

    public static String[] getSignatures() {
        List<String> signatures = new ArrayList<String>();
        Method[] methods = Transformer.class.getMethods();
        LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
        for (Method method : methods) {
            if (method.getDeclaringClass().equals(Transformer.class)
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
        return signatures.toArray(new String[signatures.size()]);
    }

    public static Object eval(Object value, String expression) {
        ScriptEngine engine = scriptEngine.get();
        if (engine == null) {
            ScriptEngineManager factory = new ScriptEngineManager();
            engine = factory.getEngineByName("groovy");
            scriptEngine.set(engine);
        }

        engine.put("value", value);

        try {
            String importString = "import org.jumpmind.symmetric.is.core.runtime.component.Transformer;\n";
            String code = String.format(
                    "return new Transformer(value) { public Object eval() { return %s } }.eval()",
                    expression);
            Object obj = engine.eval(importString + code);
            if (obj instanceof RuntimeException) {
                throw (RuntimeException)obj;
            } else if (obj instanceof Throwable) {
                throw new RuntimeException((Throwable)obj);
            } else {
                return obj;
            }            
        } catch (ScriptException e) {
            throw new RuntimeException("Unable to evaluate groovy script", e);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(Arrays.toString(Transformer.getSignatures()));
        System.out.println(Transformer.eval("abcd", "left(3)"));
    }

}
