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

import org.jumpmind.metl.core.model.User;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.core.security.ISecurityService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.Page;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class ChangePasswordDialog extends Window {

    final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationContext context;

    private PasswordField currentPasswordField;

    private PasswordField newPasswordField;

    private PasswordField validatePasswordField;

    public ChangePasswordDialog(ApplicationContext context) {
        super("Change Password");

        this.context = context;

        setWidth(300, Unit.PIXELS);
        setResizable(false);
        setModal(true);

        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);

        FormLayout fieldLayout = new FormLayout();
        fieldLayout.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);
        //fieldLayout.setSpacing(true);
        
        currentPasswordField = new PasswordField("Current Password");
        currentPasswordField.setWidth(100, Unit.PERCENTAGE);
        currentPasswordField.setNullRepresentation("");
        fieldLayout.addComponent(currentPasswordField);

        newPasswordField = new PasswordField("New Password");
        newPasswordField.setWidth(100, Unit.PERCENTAGE);
        newPasswordField.setNullRepresentation("");
        fieldLayout.addComponent(newPasswordField);

        validatePasswordField = new PasswordField("Verify Password");
        validatePasswordField.setWidth(100, Unit.PERCENTAGE);
        validatePasswordField.setNullRepresentation("");
        fieldLayout.addComponent(validatePasswordField);

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.addStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
        buttonLayout.setWidth(100, Unit.PERCENTAGE);
        
        Label spacer = new Label();
        buttonLayout.addComponent(spacer);
        buttonLayout.setExpandRatio(spacer, 1);

        Button cancelButton = new Button("Cancel", (e) -> close());
        cancelButton.setClickShortcut(KeyCode.ESCAPE);
        buttonLayout.addComponent(cancelButton);
        buttonLayout.setComponentAlignment(cancelButton, Alignment.BOTTOM_RIGHT);

        Button changeButton = new Button("Change", (e) -> changePassword());
        changeButton.setClickShortcut(KeyCode.ENTER);
        changeButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
        buttonLayout.addComponent(changeButton);
        buttonLayout.setComponentAlignment(changeButton, Alignment.BOTTOM_RIGHT);

        layout.addComponent(fieldLayout);
        layout.setExpandRatio(fieldLayout, 1);
        layout.addComponent(buttonLayout);

        setContent(layout);
    }

    protected boolean testNewPassword() {
        return LoginDialog.testPassword(newPasswordField, validatePasswordField, context);
    }
    
    public static void show(ApplicationContext context) {
        ChangePasswordDialog dialog = new ChangePasswordDialog(context);
        UI.getCurrent().addWindow(dialog);
    }

    protected void changePassword() {
        ISecurityService securityService = context.getSecurityService();
        IOperationsService operationsService = context.getOperationsService();
        User user = context.getUser();
        String password = securityService.hash(user.getSalt(), currentPasswordField.getValue());
        if (user != null && user.getPassword() != null && user.getPassword().equals(password)) {
            if (testNewPassword()) {
                operationsService.savePassword(user, newPasswordField.getValue());
                close();
            }
        } else {
            String address = Page.getCurrent().getWebBrowser().getAddress();
            log.warn("Invalid change password attempt for user " + user.getLoginId()
                    + " from address " + address);
            notify("Invalid Password", "The current password is invalid");
            currentPasswordField.selectAll();
        }

    }

    protected void notify(String caption, String message) {
        LoginDialog.notify(caption, message);
    }

}
