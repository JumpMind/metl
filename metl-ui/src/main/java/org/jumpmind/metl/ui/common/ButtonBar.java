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

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

public class ButtonBar extends HorizontalLayout {

    private static final long serialVersionUID = 1L;

    public static final String STYLE = "button-bar";

    HorizontalLayout bar;

    HorizontalLayout wrapper;

    HorizontalLayout left;

    HorizontalLayout right;

    public ButtonBar() {
        setWidthFull();
        getStyle().set("margin", "4px 0");

        wrapper = new HorizontalLayout();
        wrapper.setSpacing(true);
        wrapper.setWidthFull();
        wrapper.addClassName(STYLE);
        wrapper.getStyle().set("margin", "0 16px 0 0");

        left = new HorizontalLayout();       
        wrapper.add(left);        
        wrapper.setVerticalComponentAlignment(Alignment.CENTER, left);

        bar = new HorizontalLayout();

        wrapper.add(bar);
        wrapper.setVerticalComponentAlignment(Alignment.CENTER, bar);

        Span spacer = new Span();
        spacer.addClassName(STYLE);
        wrapper.addAndExpand(spacer);

        right = new HorizontalLayout();
        right.setMargin(false);
        wrapper.add(right);
        wrapper.setVerticalComponentAlignment(Alignment.CENTER, right);

        add(wrapper);
    }

    public TextField addFilter() {
        TextField textField = new TextField();
        textField.setWidth("15em");
        textField.setPlaceholder("Filter");
        textField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        textField.setValueChangeMode(ValueChangeMode.LAZY);
        textField.setValueChangeTimeout(500);
        wrapper.add(textField);
        wrapper.setVerticalComponentAlignment(Alignment.END, textField);
        return textField;
    }
    
    public void addToBar(Component component) {
        bar.add(component);
    }

    public void addRight(Component component) {
        right.add(component);
        right.setVerticalComponentAlignment(Alignment.END, component);
    }

    public void addLeft(Component component) {
        left.getStyle().set("margin", "0 0 0 16px");
        left.add(component);
    }

    public Button addButton(String name, VaadinIcon icon) {
        return addButton(name, icon, null);
    }

    public Button addButton(String name, VaadinIcon icon, ComponentEventListener<ClickEvent<Button>> clickListener) {
        Button button = createButton(name, icon, clickListener);
        bar.add(button);
        return button;
    }

    public Button addButtonRight(String name, VaadinIcon icon, ComponentEventListener<ClickEvent<Button>> clickListener) {
        Button button = createButton(name, icon, clickListener);
        right.add(button);
        return button;
    }
    
    public Button createButton(String name, VaadinIcon icon, ComponentEventListener<ClickEvent<Button>> clickListener) {
        Icon buttonIcon = new Icon(icon);
        buttonIcon.setSize("16px");
        VerticalLayout buttonContent = new VerticalLayout(buttonIcon, new Span(name));
        buttonContent.setPadding(false);
        buttonContent.setSpacing(false);
        buttonContent.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        Button button = new Button(buttonContent);
        button.setHeight("60px");
        button.getStyle().set("min-width", "100px");
        button.addClassName(STYLE);
        if (clickListener != null) {
            button.addClickListener(clickListener);
        }
        return button;
    }

}
