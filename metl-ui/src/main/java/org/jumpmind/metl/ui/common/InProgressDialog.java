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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.jumpmind.metl.ui.init.BackgroundRefresherService;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;

public class InProgressDialog<T> extends Dialog {

    private static final long serialVersionUID = 1L;

    static final Logger logger = LoggerFactory.getLogger(InProgressDialog.class);

    InProgressWorker<T> worker;

    BackgroundRefresherService backgroundService;
    
    String failureMessage;

    public InProgressDialog(String caption, InProgressWorker<T> worker, BackgroundRefresherService backgroundService) {
        this(caption, worker, backgroundService, null);        
    }
    
    public InProgressDialog(String caption, InProgressWorker<T> worker, BackgroundRefresherService backgroundService, String failureMessage) {
        this.backgroundService = backgroundService;
        this.worker = worker;
        this.failureMessage = failureMessage;
        setWidth("300px");
        setHeight("150px");
        setModal(true);

        Span header = new Span("<b>Working...</b><hr>");
        header.setWidthFull();
        header.getStyle().set("margin", null);
        add(header);

        VerticalLayout content = new VerticalLayout();
        add(content);

        HorizontalLayout middle = new HorizontalLayout();
        middle.setSpacing(true);
        middle.setMargin(true);

        ProgressBar pg = new ProgressBar();
        pg.setIndeterminate(true);
        middle.add(pg);

        if (isNotBlank(caption)) {
            Span span = new Span(caption);
            middle.add(span);
        }

        content.add(middle);
        content.expand(middle);

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();
        Button dismiss = new Button("Dismiss");
        dismiss.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dismiss.addClickShortcut(Key.ENTER);
        dismiss.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onComponentEvent(ClickEvent<Button> event) {
                InProgressDialog.this.close();
            }
        });
        buttonBar.addAndExpand(new Span());
        buttonBar.add(dismiss);
        buttonBar.setVerticalComponentAlignment(Alignment.CENTER, dismiss);
        content.add(buttonBar);

    }

    public void show() {
        open();
        backgroundService.doWork(new IBackgroundRefreshable<Object>() {

            @Override
            public void onBackgroundUIRefresh(Object backgroundData) {
                worker.doUI(null);
                InProgressDialog.this.close();
            }

            @Override
            public Object onBackgroundDataRefresh() {
                return worker.doWork();
                
            }

            @Override
            public void onUIError(Throwable ex) {
                if (failureMessage != null) {
                    CommonUiUtils.notifyError(failureMessage);
                } else {
                    CommonUiUtils.notifyError();
                }
                InProgressDialog.this.close();                
            }
        });
    }

    public interface InProgressWorker<T> {
        public T doWork();
        public void doUI(T data);
    }
}