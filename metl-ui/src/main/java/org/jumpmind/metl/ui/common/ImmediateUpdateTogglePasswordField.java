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
package org.jumpmind.metl.ui.common;


import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;

public abstract class ImmediateUpdateTogglePasswordField extends CustomField<String> {
    private static final long serialVersionUID = 1L;
    
    private static final String BUTTON_SHOW = "Show";
    private static final String BUTTON_HIDE = "Hide";
    
    private final Button button = new Button(BUTTON_SHOW);
    private final PasswordField passwordField = new PasswordField();
    private final TextField textField = new TextField();
    private boolean toggleAllowed = true;

    public ImmediateUpdateTogglePasswordField() {
    }
    
    public ImmediateUpdateTogglePasswordField(String caption) {
        setCaption(caption);
    }

    @Override
    protected Component initContent() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.addComponent(passwordField);
        layout.addComponent(button);
        
        button.addClickListener(event -> {
            if (toggleAllowed) {
                if (button.getCaption().equals(BUTTON_SHOW)) {
                    // Show password
                    button.setCaption(BUTTON_HIDE);
                    textField.setValue(passwordField.getValue());
                    layout.replaceComponent(passwordField, textField);
                } else {
                    // Hide password
                    button.setCaption(BUTTON_SHOW);
                    passwordField.setValue(textField.getValue());
                    layout.replaceComponent(textField, passwordField);
                }
            }
        });
        
        // Valo FORMLAYOUT_LIGHT style does not apply to nested fields  
        // https://github.com/vaadin/framework/issues/8837
        // Work around is to remove the border manually to match the other 
        // fields that are not nested. 
        // Once bug is fixed, remove the following two lines. 
        passwordField.addStyleName("noborder");
        textField.addStyleName("noborder");
        // End Work Around
        
        passwordField.setImmediate(true);
        textField.setImmediate(true);
        passwordField.setNullRepresentation("");
        textField.setNullRepresentation("");
        passwordField.setTextChangeEventMode(TextChangeEventMode.LAZY);
        textField.setTextChangeEventMode(TextChangeEventMode.LAZY);
        passwordField.setTextChangeTimeout(200);
        textField.setTextChangeTimeout(200);
        passwordField.addTextChangeListener(new TextChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void textChange(TextChangeEvent event) {
                save(event.getText());
            }
        });
        textField.addTextChangeListener(new TextChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void textChange(TextChangeEvent event) {
                save(event.getText());
            }
        });

        return layout;
    }
    
    @Override
    public Class<? extends String> getType() {
        return String.class;
    }

    public boolean isToggleAllowed() {
        return toggleAllowed;
    }

    public void setToggleAllowed(boolean toggleAllowed) {
        this.toggleAllowed = toggleAllowed;
        button.setVisible(toggleAllowed);
    }
    
    @Override
    public void setValue(String value) {
        passwordField.setValue(value);
        textField.setValue(value);
    }
    
    abstract protected void save(String text);
    
}

