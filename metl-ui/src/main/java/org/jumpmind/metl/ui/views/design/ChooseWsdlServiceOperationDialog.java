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
package org.jumpmind.metl.ui.views.design;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.reficio.ws.builder.SoapOperation;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;

@SuppressWarnings("serial")
public class ChooseWsdlServiceOperationDialog extends Dialog {

    public ChooseWsdlServiceOperationDialog(List<SoapOperation> operations,
            final ServiceChosenListener listener) {
        setModal(true);
        setResizable(false);
        setSizeUndefined();

        Span header = new Span("<b>Choose SOAP Operation</b><hr>");
        header.setWidthFull();
        header.getStyle().set("margin", null);
        add(header);

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        add(layout);

        layout.add(new Span("Choose the SOAP operation to use."));

        final ListBox<SoapOperation> field = new ListBox<SoapOperation>();
        field.setWidthFull();
        field.setHeight("540px");
        layout.add(field);

        Collections.sort(operations, new Comparator<SoapOperation>() {
            public int compare(SoapOperation o1, SoapOperation o2) {
                return o1.getOperationName().compareTo(o2.getOperationName());
            }
        });

        field.setItems(operations);
        field.setValue(operations.iterator().next());
        field.addValueChangeListener(event -> {
            if (event.getValue() == null) {
                field.setValue(event.getOldValue());
            }
        });
        field.setRenderer(new ComponentRenderer<>(item -> new Span(item.getBindingName().getLocalPart() + "." + item.getOperationName())));

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setWidthFull();

        Span spacer = new Span(" ");
        buttonLayout.addAndExpand(spacer);

        Button cancelButton = new Button("Cancel");
        cancelButton.addClickShortcut(Key.ESCAPE);
        cancelButton.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
            public void onComponentEvent(ClickEvent<Button> event) {
                close();
            }
        });
        buttonLayout.add(cancelButton);

        Button okButton = new Button("Ok");
        okButton.addClickShortcut(Key.ENTER);
        okButton.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
            public void onComponentEvent(ClickEvent<Button> event) {
                if (listener.onOk((SoapOperation) field.getValue())) {
                    close();
                }
            }
        });
        buttonLayout.add(okButton);

        layout.add(buttonLayout);
    }

    public static interface ServiceChosenListener extends Serializable {
        public boolean onOk(SoapOperation operation);
    }

}