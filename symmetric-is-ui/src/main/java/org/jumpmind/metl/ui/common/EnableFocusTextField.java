package org.jumpmind.symmetric.is.ui.common;

import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class EnableFocusTextField extends TextField {

    protected boolean focusAllowed = true;
    
    public EnableFocusTextField() {
    }
    
    public void setFocusAllowed(boolean focusAllowed) {
        this.focusAllowed = focusAllowed;
    }
    
    public boolean isFocusAllowed() {
        return focusAllowed;
    }
    
    @Override
    public void focus() {
        if (focusAllowed) {
            super.focus();
        }
    }
    
    
}
