package org.jumpmind.symmetric.is.ui.support;

import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.vaadin.data.util.converter.Converter.ConversionException;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class SqlField extends CustomField<String> {

    private static final long serialVersionUID = 1L;

    Button button;

    public SqlField() {
        getContent();        
    }
    
    @Override
    public void setValue(String newFieldValue) throws com.vaadin.data.Property.ReadOnlyException,
            ConversionException {
        super.setValue(newFieldValue);
        button.setCaption(buttonValue(newFieldValue));
    }

    @Override
    protected Component initContent() {
        HorizontalLayout layout = new HorizontalLayout();
        button = new Button();
        button.addStyleName(ValoTheme.BUTTON_LINK);
        button.addStyleName(UiConstants.STYLE_BUTTON_LEFT_ALIGNED);
        button.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                SqlEntryWindow window = new SqlEntryWindow(getValue()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean onClose() {
                        SqlField.this.setValue(editor.getValue());
                        return super.onClose();
                    }
                };
                window.showAtSize(.5);
            }
        });
        layout.addComponent(button);
        layout.setComponentAlignment(button, Alignment.MIDDLE_LEFT);
        return layout;
    }

    @Override
    public Class<? extends String> getType() {
        return String.class;
    }

    protected String buttonValue(String value) {
        return isNotBlank(value) ? abbreviate(value, 30) : "Click to edit";
    }

}
