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

import java.util.Date;

import org.jumpmind.metl.core.model.User;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.Page;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class LoginDialog extends Window {

    final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationContext context;
    
    private TextField userField;

    private PasswordField passwordField;

    private LoginListener loginListener;

    public LoginDialog(ApplicationContext context, LoginListener loginListener) {
        super("Login to Metl");
        this.context = context;
        this.loginListener = loginListener;
        setWidth(300, Unit.PIXELS);
        setResizable(false);
        setReadOnly(true);
        setModal(true);
        setClosable(false);

        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);
        
        userField = new TextField("Login");
        userField.setWidth(100, Unit.PERCENTAGE);
        layout.addComponent(userField);

        passwordField = new PasswordField("Password");
        passwordField.setImmediate(true);
        passwordField.setWidth(100, Unit.PERCENTAGE);
        layout.addComponent(passwordField);

        HorizontalLayout buttonLayout = new HorizontalLayout();
        Button loginButton = new Button("Login");
        loginButton.addClickListener(new LoginClickListener());
        loginButton.setStyleName("primary");
        loginButton.setClickShortcut(KeyCode.ENTER);
        buttonLayout.addComponent(loginButton);
        buttonLayout.setWidth(100, Unit.PERCENTAGE);
        layout.addComponent(buttonLayout);
        buttonLayout.addComponent(loginButton);
        buttonLayout.setComponentAlignment(loginButton, Alignment.BOTTOM_RIGHT);

        setContent(layout);
        userField.focus();
    }

    class LoginClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            User user = context.getConfigurationService().findUserByLoginId(userField.getValue());            
            String password = User.hashValue(passwordField.getValue());
            
            if (user != null && user.getPassword() != null && user.getPassword().equals(password)) {
                UI.getCurrent().removeWindow(LoginDialog.this);
                user.setLastLoginTime(new Date());
                context.getConfigurationService().save(user);
                loginListener.login(user);
            } else {
                String address = Page.getCurrent().getWebBrowser().getAddress();
                log.warn("Invalid login attempt for user " + userField.getValue() + " from address " + address);
                Notification note = new Notification("Invalid Login", "You specified an invalid login or password");
                note.show(Page.getCurrent());
                userField.selectAll();
            }
        }
    }
    
    static public interface LoginListener {
        public void login(User user);
    }

}
