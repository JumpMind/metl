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

import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

public class ButtonBar extends HorizontalLayout {

    private static final long serialVersionUID = 1L;

    public static final String STYLE = "button-bar";

    HorizontalLayout bar;

    HorizontalLayout wrapper;

    HorizontalLayout left;

    HorizontalLayout right;

    public ButtonBar() {
        setWidth(100, Unit.PERCENTAGE);
        setMargin(new MarginInfo(true, false, true, false));

        wrapper = new HorizontalLayout();
        wrapper.setSpacing(true);
        wrapper.setWidth(100, Unit.PERCENTAGE);
        wrapper.addStyleName(STYLE);
        wrapper.setMargin(new MarginInfo(false, true, false, false));

        left = new HorizontalLayout();       
        wrapper.addComponent(left);        
        wrapper.setComponentAlignment(left, Alignment.MIDDLE_LEFT);

        bar = new HorizontalLayout();

        wrapper.addComponent(bar);
        wrapper.setComponentAlignment(bar, Alignment.MIDDLE_LEFT);

        Label spacer = new Label();
        spacer.addStyleName(STYLE);
        wrapper.addComponent(spacer);
        wrapper.setExpandRatio(spacer, 1);

        right = new HorizontalLayout();
        right.setSpacing(false);
        right.setMargin(false);
        wrapper.addComponent(right);
        wrapper.setComponentAlignment(right, Alignment.MIDDLE_RIGHT);

        addComponent(wrapper);
    }

    public TextField addFilter() {
        TextField textField = new TextField();
        textField.setColumns(15);
        textField.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        textField.setInputPrompt("Filter");
        textField.setIcon(FontAwesome.SEARCH);
        textField.setTextChangeEventMode(TextChangeEventMode.LAZY);
        textField.setTextChangeTimeout(500);
        textField.setNullRepresentation("");
        wrapper.addComponent(textField);
        wrapper.setComponentAlignment(textField, Alignment.BOTTOM_RIGHT);
        return textField;
    }

    public void addRight(Component component) {
        right.addComponent(component);
        right.setComponentAlignment(component, Alignment.BOTTOM_RIGHT);
    }

    public void addLeft(Component component) {
        left.setMargin(new MarginInfo(false, false, false, true));
        left.addComponent(component);
    }

    public Button addButton(String name, Resource icon) {
        return addButton(name, icon, null);
    }

    public Button addButton(String name, Resource icon, ClickListener clickListener) {
        Button button = createButton(name, icon, clickListener);
        bar.addComponent(button);
        return button;
    }

    public Button addButtonRight(String name, Resource icon, ClickListener clickListener) {
        Button button = createButton(name, icon, clickListener);
        right.addComponent(button);
        return button;
    }
    
    public Button createButton(String name, Resource icon, ClickListener clickListener) {
        Button button = new Button(name);
        button.addStyleName(STYLE);
        button.setIcon(icon);
        if (clickListener != null) {
            button.addClickListener(clickListener);
        }
        return button;
    }

}
