package org.jumpmind.symmetric.is.ui.support;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.apache.commons.lang.StringUtils;

import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.TextField;

public class ImmediateUpdateTextField extends TextField {

    private static final long serialVersionUID = 1L;

    String startValue;
    
    public ImmediateUpdateTextField(String caption) {
        super(caption);
        setImmediate(true);
        setNullRepresentation("");        
        addFocusListener(new FocusListener() {
            private static final long serialVersionUID = 1L;
            @Override
            public void focus(FocusEvent event) {
                startValue = getValue();
            }
        });
        addBlurListener(new BlurListener() {                
            private static final long serialVersionUID = 1L;                
            @Override
            public void blur(BlurEvent event) {
                if (isNotBlank(getState().errorMessage)) {
                    setValue(startValue);
                } else if (!StringUtils.equals(startValue, getValue())) {
                    save();
                }
            }
        });
        addShortcutListener(new ShortcutListener("field", KeyCode.ENTER, null) {
            private static final long serialVersionUID = 1L;
            @Override
            public void handleAction(Object sender, Object target) {
                if (!StringUtils.equals(startValue, getValue())) {
                    save();
                }
            }
        });
    }
    
    protected void save() {
        
    }
}
