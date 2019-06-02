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

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

public class InProgressDialog<T> extends Window {

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
        setWidth(300, Unit.PIXELS);
        setHeight(150, Unit.PIXELS);
        setCaption("Working...");
        setModal(true);

        VerticalLayout content = new VerticalLayout();
        setContent(content);

        HorizontalLayout middle = new HorizontalLayout();
        middle.setSpacing(true);
        middle.setMargin(true);

        ProgressBar pg = new ProgressBar();
        pg.setIndeterminate(true);
        middle.addComponent(pg);

        if (isNotBlank(caption)) {
            Label label = new Label(caption);
            middle.addComponent(label);
        }

        content.addComponent(middle);
        content.setExpandRatio(middle, 1);

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidth(100, Unit.PERCENTAGE);
        buttonBar.addStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
        Button dismiss = new Button("Dismiss");
        dismiss.addStyleName(ValoTheme.BUTTON_PRIMARY);
        dismiss.setClickShortcut(KeyCode.ENTER);
        dismiss.addClickListener(new ClickListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                InProgressDialog.this.close();
            }
        });
        buttonBar.addComponent(dismiss);
        buttonBar.setComponentAlignment(dismiss, Alignment.MIDDLE_RIGHT);
        content.addComponent(buttonBar);

    }

    public void show() {
        UI.getCurrent().addWindow(this);
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
                    CommonUiUtils.notify(failureMessage, Type.ERROR_MESSAGE);
                } else {
                    CommonUiUtils.notify(ex);
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