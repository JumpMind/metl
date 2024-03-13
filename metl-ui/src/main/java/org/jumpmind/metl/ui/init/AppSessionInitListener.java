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
package org.jumpmind.metl.ui.init;

import org.jumpmind.vaadin.ui.common.Label;
import org.jumpmind.vaadin.ui.common.ResizableDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.CustomizedSystemMessages;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.SystemMessages;
import com.vaadin.flow.server.SystemMessagesInfo;
import com.vaadin.flow.server.SystemMessagesProvider;

public class AppSessionInitListener implements SessionInitListener {

    private static final long serialVersionUID = 1L;
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void sessionInit(final SessionInitEvent event)
            throws ServiceException {
        event.getService().setSystemMessagesProvider(
                new SystemMessagesProvider() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public SystemMessages getSystemMessages(
                            final SystemMessagesInfo systemMessagesInfo) {
                        CustomizedSystemMessages csm = new CustomizedSystemMessages();
                        csm.setSessionExpiredNotificationEnabled(false);
                        return csm;
                    }
                });
        event.getSession().setErrorHandler(e -> {
            String intro = "Exception of type <b>";
            String message = "";
            for (Throwable t = e.getThrowable(); t != null; t = t.getCause()) {
                if (t.getCause() == null) {
                    intro += t.getClass().getName()
                            + "</b> with the following message:\n\n";
                    message = t.getMessage();
                }
            }
            ErrorDialog dialog = new ErrorDialog(intro, message);
            dialog.show();

            Throwable ex = e.getThrowable();
            if (ex != null) {
                log.error(ex.getMessage(), ex);
            } else {
                log.error("An unexpected error occurred");
            }
        });
    }
    
    @SuppressWarnings({ "serial" })
    class ErrorDialog extends ResizableDialog {
        public ErrorDialog(String intro, String message) {
            super("Error");
            setWidth("750px");
            setHeight("450px");
            innerContent.setMargin(true);

            HorizontalLayout layout = new HorizontalLayout();
            Icon icon = new Icon(VaadinIcon.WARNING);
            icon.setColor("red");
            icon.setSize("70px");
            layout.add(icon);

            Label labelIntro = new Label(intro);
            labelIntro.setClassName("large");
            labelIntro.setWidth("530px");
            layout.add(labelIntro);
            add(layout);

            TextArea textField = new TextArea();
            textField.setSizeFull();
            textField.getStyle().set("white-space", "pre").set("overflow-x", "auto").set("padding-bottom", "1em");
            if (message != null) {
                textField.setValue(message);
            }
            add(textField);
            innerContent.expand(textField);

            buildButtonFooter(buildCloseButton());
        }
    }

}
