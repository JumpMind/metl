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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityData.ChangeType;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.util.FormatUtils;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

public class ModelAttributeScriptHelper {

    protected Object value;

    protected EntityData data;

    protected ModelAttrib attribute;

    protected ModelEntity entity;

    protected Model model;

    protected ComponentContext context;

    protected Message message;

    public static final RemoveAttribute REMOVE_ATTRIBUTE = new RemoveAttribute();

    static private ThreadLocal<ScriptEngine> scriptEngine = new ThreadLocal<ScriptEngine>();

    public ModelAttributeScriptHelper(Message message, ComponentContext context, ModelAttrib attribute, ModelEntity entity, Model model,
            EntityData data, Object value) {
        this(context, attribute, entity, model);
        this.value = value;
        this.data = data;
        this.message = message;
    }

    public ModelAttributeScriptHelper(ComponentContext context, ModelAttrib attribute, ModelEntity entity, Model model) {
        this.context = context;
        this.attribute = attribute;
        this.entity = entity;
        this.model = model;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public void setData(EntityData data) {
        this.data = data;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object nullvalue() {
        return null;
    }

    public void mapChangeType(Object add, Object chg, Object del) {
        if (value != null && value.equals(add)) {
            data.setChangeType(ChangeType.ADD);
        } else if (value != null && value.equals(chg)) {
            data.setChangeType(ChangeType.CHG);
        } else if (value != null && value.equals(del)) {
            data.setChangeType(ChangeType.DEL);
        }
    }

    @Deprecated
    public Integer integer() {
        String text = value != null ? value.toString() : "0";
        text = isNotBlank(text) ? text : "0";
        return Integer.parseInt(text);
    }

    public Integer parseInt() {
        String text = value != null ? value.toString() : "0";
        text = isNotBlank(text) ? text : "0";
        return Integer.parseInt(text);
    }

    public Long parseLong() {
        String text = value != null ? value.toString() : "0";
        text = isNotBlank(text) ? text : "0";
        return Long.parseLong(text);
    }

    public Double parseDouble() {
        String text = value != null ? value.toString() : "0";
        text = isNotBlank(text) ? text : "0";
        return Double.parseDouble(text);
    }

    public BigDecimal parseBigDecimal() {
        return parseBigDecimal(value);
    }
    
    public BigDecimal parseBigDecimal(Object value) {
        String text = value != null ? value.toString() : "0";
        text = isNotBlank(text) ? text : "0";
        return new BigDecimal(text);
    }
    
    public Number biggest(Number... numbers) {
        Number biggest = null;
        for (int i = 0; i < numbers.length; i++) {
            if (biggest == null || biggest.doubleValue() < numbers[i].doubleValue()) {
                biggest = numbers[i];
            }
        }
        return biggest;
    }
    
    public Serializable subtract(String startingValueAttributeName, String... attributeNames) {
        BigDecimal value = parseBigDecimal(data.get(entity.getModelAttributeByName(startingValueAttributeName).getId()));
        for(int i = 0; i < attributeNames.length; i++) {
            value = value.subtract(parseBigDecimal(data.get(entity.getModelAttributeByName(attributeNames[i]).getId())));    
        }
        return value;
    }
    
    public Serializable subtract(String startingValueAttributeName, Number... numbers) {
        BigDecimal value = parseBigDecimal(data.get(entity.getModelAttributeByName(startingValueAttributeName).getId()));
        for(int i = 0; i < numbers.length; i++) {
            value = value.subtract(new BigDecimal(numbers[i].toString()));    
        }
        return value;
    }    
    
    public Serializable add(String... attributeNames) {
        BigDecimal value = BigDecimal.ZERO;
        for(int i = 0; i < attributeNames.length; i++) {
            value = value.add(parseBigDecimal(data.get(entity.getModelAttributeByName(attributeNames[i]).getId())));    
        }
        return value;
    }    

    public Serializable map(Map<Object, Serializable> lookup, Serializable defaultValue) {
        if (value != null) {
            value = lookup.get(value);
        }
        
        if (value == null) {
            value = defaultValue;
        }
        
        return (Serializable) value;
    }

    public Serializable flowParameter(String parameterName) {
        return context.getFlowParameters().get(parameterName);
    }

    public Serializable messageParameter(String parameterName) {
        return message.getHeader().get(parameterName);
    }

    public String abbreviate(int maxwidth) {
        if (value != null) {
            return StringUtils.abbreviate(value.toString(), maxwidth);
        } else {
            return null;
        }
    }

    public String left(int length) {
        if (value != null) {
            return StringUtils.left(value.toString(), length);
        } else {
            return null;
        }
    }

    public String right(int length) {
        if (value != null) {
            return StringUtils.right(value.toString(), length);
        } else {
            return null;
        }
    }

    public String rpad(String padChar, int length) {
        if (value != null) {
            return StringUtils.rightPad(value.toString(), length, padChar);
        } else {
            return null;
        }
    }

    public String lpad(String padChar, int length) {
        if (value != null) {
            return StringUtils.leftPad(value.toString(), length, padChar);
        } else {
            return null;
        }
    }

    public String substr(int start, int end) {
        if (value != null) {
            return StringUtils.substring(value.toString(), start, end);
        } else {
            return null;
        }
    }

    public String lower() {
        if (value != null) {
            return StringUtils.lowerCase(value.toString());
        } else {
            return null;
        }
    }

    public String upper() {
        if (value != null) {
            return StringUtils.upperCase(value.toString());
        } else {
            return null;
        }
    }

    public String trim() {
        if (value != null) {
            return StringUtils.trim(value.toString());
        } else {
            return null;
        }
    }

    public String format(String spec) {
        return String.format(spec, value);
    }

    public String replace(String searchString, String replacement) {
        if (value != null) {
            return StringUtils.replace(value.toString(), searchString, replacement);
        } else {
            return null;
        }
    }

    public Date daysFromNow(int days) {
        return DateUtils.addDays(new Date(), days);
    }

    public Date currentdate() {
        return new Date();
    }

    public String currentdate(String format) {
        Date currentDate = new Date();
        return formatdate(format, currentDate);

    }

    public RemoveAttribute remove() {
        return REMOVE_ATTRIBUTE;
    }

    public Date parsedate(String pattern, String nulldate) {
        String text = value != null ? value.toString() : "";
        if (isNotBlank(text) && !text.equals(nulldate)) {
            return parseDateFromText(pattern, text);
        } else {
            return null;
        }
    }

    public Date parsedate(String pattern) {
        String text = value != null ? value.toString() : "";
        return parseDateFromText(pattern, text);
    }

    public String formatdate(String pattern) {
        FastDateFormat formatter = FastDateFormat.getInstance(pattern);
        if (value instanceof Date) {
            return formatter.format((Date) value);
        } else if (value != null) {
            String text = value != null ? value.toString() : "";
            Date dateToParse = parseDateFromText(pattern, text);
            if (dateToParse != null) {
                return formatter.format((Date) value);
            } else {
                return "Not a datetime";
            }
        } else {
            return "";
        }
    }

    private String formatdate(String pattern, Date value) {
        FastDateFormat formatter = FastDateFormat.getInstance(pattern);
        if (value != null) {
            return formatter.format(value);
        } else {
            return null;
        }
    }

    public String parseAndFormatDate(String parsePattern, String formatPattern) {
        Date dateToFormat = parsedate(parsePattern);
        return formatdate(formatPattern, dateToFormat);
    }

    public Object nvl(Object substituteForNull) {
        if (value == null) {
            return substituteForNull;
        } else {
            return value;
        }
    }

    private Date parseDateFromText(String pattern, String valueToParse) {
        if (isNotBlank(valueToParse)) {
            return FormatUtils.parseDate(valueToParse, new String[] { pattern });
        } else {
            return null;
        }
    }

    public String stringConstant(String value) {
        return value;
    }

    public String capitalize() {
        if (value != null) {
            return StringUtils.capitalize(value.toString());
        } else {
            return null;
        }
    }

    protected Object eval() {
        return value;
    }

    public Long nextLongValueInMessage(String... attributeNamesToLookAt) {
        Long max = (Long) message.getHeader().get("_nextLongValueInMessage");
        if (max == null) {
            max = 0l;
        }
        EntityDataMessage dataMessage = (EntityDataMessage) message;
        List<EntityData> datas = dataMessage.getPayload();
        for (EntityData entityData : datas) {
            for (String attributeName : attributeNamesToLookAt) {
                List<ModelAttrib> attributes = model.getAttributesByName(attributeName);
                for (ModelAttrib attribute : attributes) {
                    long currentValue = 0;
                    Object obj = entityData.get(attribute.getId());
                    if (obj instanceof Number) {
                        currentValue = ((Number) obj).longValue();
                    } else if (obj != null) {
                        try {
                            currentValue = new Long(obj.toString());
                        } catch (NumberFormatException e) {
                        }
                    }
                    
                    if (currentValue > max) {
                        max = currentValue;
                    }
                }
            }
        }
        max++;
        message.getHeader().put("_nextLongValueInMessage", max);
        return max;
    }

    public Long sequence(long seed_value, int incrementValue, String breakAttributeName) {
        ModelAttrib breakAttribute = entity.getModelAttributeByName(breakAttributeName);
        if (breakAttribute == null) {
            throw new RuntimeException("Break attribute not found.  Specify the name of the attribute (no entity qualifier)");
        }
        Long sequenceValue = (Long) context.getContext().get(attribute.getId());
        Object breakAttributeValue = data.get(breakAttribute.getId());
        if (sequenceValue == null) {
            sequenceValue = new Long(seed_value);
        } else {
            if (context.getContext().get(attribute.getId() + "-" + breakAttribute.getId()).equals(breakAttributeValue)) {
                sequenceValue = new Long(sequenceValue.longValue() + incrementValue);
            } else {
                sequenceValue = new Long(seed_value);
            }
        }
        context.getContext().put(attribute.getId(), sequenceValue);
        context.getContext().put(attribute.getId() + "-" + breakAttribute.getId(), breakAttributeValue);
        return sequenceValue;
    }

    public Object getAttributeValueByName(String attributeName) {
        return data.get(entity.getModelAttributeByName(attributeName).getId());
    }

    public static String[] getSignatures() {
        List<String> signatures = new ArrayList<String>();
        Method[] methods = ModelAttributeScriptHelper.class.getMethods();
        LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
        for (Method method : methods) {
            if (method.getDeclaringClass().equals(ModelAttributeScriptHelper.class) && Modifier.isPublic(method.getModifiers())
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

    public static Object eval(Message message, ComponentContext context, ModelAttrib attribute, Object value, Model model, ModelEntity entity,
            EntityData data, String expression) {
        ScriptEngine engine = scriptEngine.get();
        if (engine == null) {
            engine = new GroovyScriptEngineImpl();
            scriptEngine.set(engine);
        }
        engine.put("value", value);
        engine.put("data", data);
        engine.put("entity", entity);
        engine.put("model", model);
        engine.put("attribute", attribute);
        engine.put("message", message);
        engine.put("context", context);

        try {
            String importString = "import org.jumpmind.metl.core.runtime.component.ModelAttributeScriptHelper;\n";
            String code = String.format(
                    "return new ModelAttributeScriptHelper(message, context, attribute, entity, model, data, value) { public Object eval() { return %s } }.eval()",
                    expression);
            return engine.eval(importString + code);
        } catch (ScriptException e) {
            throw new RuntimeException("Unable to evaluate groovy script.  Attribute ==> " + attribute.getName() + ".  Value ==> "
                    + value.toString() + "." + e.getCause().getMessage(), e);
        }
    }

    static class RemoveAttribute {

    }

}
