/**
f * Licensed to JumpMind Inc under one or more contributor
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

import static org.jumpmind.metl.core.model.GlobalSetting.PASSWORD_MIN_LENGTH;
import static org.jumpmind.metl.core.model.GlobalSetting.PASSWORD_PROHIBIT_COMMON_WORDS;
import static org.jumpmind.metl.core.model.GlobalSetting.PASSWORD_PROHIBIT_PREVIOUS;
import static org.jumpmind.metl.core.model.GlobalSetting.PASSWORD_REQUIRE_ALPHANUMERIC;
import static org.jumpmind.metl.core.model.GlobalSetting.PASSWORD_REQUIRE_MIXED_CASE;
import static org.jumpmind.metl.core.model.GlobalSetting.PASSWORD_REQUIRE_SYMBOL;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.jumpmind.metl.core.authentication.ConsoleAuthenticationConnectionException;
import org.jumpmind.metl.core.authentication.ConsoleAuthenticationCredentialException;
import org.jumpmind.metl.core.authentication.IConsoleUserAuthentication;
import org.jumpmind.metl.core.authentication.IConsoleUserAuthentication.AuthenticationStatus;
import org.jumpmind.metl.core.authentication.UserAuthenticationInternal;
import org.jumpmind.metl.core.authentication.UserAuthenticationLDAP;
import org.jumpmind.metl.core.model.GlobalSetting;
import org.jumpmind.metl.core.model.User;
import org.jumpmind.metl.core.model.UserHist;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.core.security.ISecurityService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.Page;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.LoginForm;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class LoginDialog extends Window {

    final Logger log = LoggerFactory.getLogger(getClass());

    private final String PASSWORD_EXPIRED = "Password Expired";

    private ApplicationContext context;

    private TextField userNameField;

    private PasswordField passwordField;

    private PasswordField validatePasswordField;
    
    private Button loginButton;

    private LoginListener loginListener;

    private int passwordExpiresInDays;

    private TypedProperties settings;

    public LoginDialog(ApplicationContext context, LoginListener loginListener) {
        super("Login to Metl");

        this.context = context;
        this.loginListener = loginListener;

        settings = new TypedProperties();
        settings.putAll(context.getOperationsService().findGlobalSettingsAsMap());
        passwordExpiresInDays = settings.getInt(GlobalSetting.PASSWORD_EXPIRE_DAYS, 60);

        setWidth(300, Unit.PIXELS);
        setResizable(false);
        setModal(true);
        setClosable(false);

        LoginForm loginForm = new LoginForm() {

            @Override
            protected Component createContent(TextField userNameField, PasswordField passwordField,
                    Button loginButton) {
                VerticalLayout layout = new VerticalLayout();
                layout.setMargin(true);
                layout.setSpacing(true);

                LoginDialog.this.userNameField = userNameField;
                userNameField.setWidth(100, Unit.PERCENTAGE);
                LoginDialog.this.passwordField = passwordField;
                passwordField.setWidth(100, Unit.PERCENTAGE);
                passwordField.setNullRepresentation("");

                layout.addComponent(userNameField);
                layout.addComponent(passwordField);

                validatePasswordField = new PasswordField("Verify Password");
                validatePasswordField.setWidth(100, Unit.PERCENTAGE);
                validatePasswordField.setNullRepresentation("");
                validatePasswordField.setVisible(false);
                layout.addComponent(validatePasswordField);

                HorizontalLayout buttonLayout = new HorizontalLayout();
                loginButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
                LoginDialog.this.loginButton = loginButton;
                buttonLayout.addComponent(loginButton);
                buttonLayout.setWidth(100, Unit.PERCENTAGE);
                layout.addComponent(buttonLayout);
                buttonLayout.addComponent(loginButton);
                buttonLayout.setComponentAlignment(loginButton, Alignment.BOTTOM_RIGHT);

                userNameField.focus();
                return layout;
            }

        };
        loginForm.addLoginListener((e) -> login((String) userNameField.getValue(), (String) passwordField.getValue()));
        loginForm.setWidth(300, Unit.PIXELS);

        setContent(loginForm);
    }

    protected boolean isNewPasswordMode() {
        return PASSWORD_EXPIRED.equals(getCaption());
    }

    // TODO: This should probably be moved to the authentication method object and should return
    // a status vs displaying notifications itself. 
    protected static boolean testPassword(PasswordField passwordField,
            PasswordField validatePasswordField, ApplicationContext context) {
        boolean passedTest = true;
        if (validatePasswordField.getValue() == null || passwordField.getValue() == null
                || !validatePasswordField.getValue().equals(passwordField.getValue())) {
            notify("Invalid Password", "The passwords did not match");
            passedTest = false;
        } else {
            TypedProperties settings = new TypedProperties();
            settings.putAll(context.getOperationsService().findGlobalSettingsAsMap());
            ISecurityService securityService = context.getSecurityService();

            int minPasswordLength = settings.getInt(PASSWORD_MIN_LENGTH, 6);
            if (passwordField.getValue().length() < minPasswordLength) {
                passedTest = false;
                notify("Password too short",
                        "The password is required to be at least " + minPasswordLength
                                + " characters long.  Please choose a different password.");
            }

            int prohibitNPreviousPasswords = settings.getInt(PASSWORD_PROHIBIT_PREVIOUS, 5);
            if (passedTest && prohibitNPreviousPasswords != 0) {
                List<UserHist> histories = context.getOperationsService()
                        .findUserHist(context.getUser().getId());
                if (prohibitNPreviousPasswords < 0) {
                    prohibitNPreviousPasswords = histories.size();
                }
                for (int i = 0; i < histories.size() && i < prohibitNPreviousPasswords; i++) {
                    UserHist hist = histories.get(i);
                    String toCompare = securityService.hash(hist.getSalt(),
                            passwordField.getValue());
                    if (toCompare.equals(hist.getPassword())) {
                        passedTest = false;
                        notify("Password Repeated",
                                "You have used this password in the past.  Please choose a different password.");
                        break;
                    }
                }
            }

            if (passedTest) {
                boolean requiresAlphaNumberic = settings.is(PASSWORD_REQUIRE_ALPHANUMERIC, true);
                if (requiresAlphaNumberic && !containsAlphanumeric(passwordField.getValue())) {
                    passedTest = false;
                    notify("At least one letter and one number is required",
                            "At least one letter and one number is required.  Please choose a different password.");
                }
            }

            if (passedTest) {
                boolean requiresSymbol = settings.is(PASSWORD_REQUIRE_SYMBOL, true);
                if (requiresSymbol && !containsSymbol(passwordField.getValue())) {
                    passedTest = false;
                    notify("Password requires a symbol",
                            "At least one symbol character is required.  Please choose a different password.");
                }
            }

            if (passedTest) {
                boolean requiresMixedCase = settings.is(PASSWORD_REQUIRE_MIXED_CASE, true);
                if (requiresMixedCase && !containsMixedCase(passwordField.getValue())) {
                    passedTest = false;
                    notify("Password requires mixed case",
                            "At least one upper case and one lower case character is required.  Please choose a different password.");
                }
            }

            if (passedTest) {
                boolean prohibitCommonWords = settings.is(PASSWORD_PROHIBIT_COMMON_WORDS, true);
                if (prohibitCommonWords && containsCommonWords(passwordField.getValue())) {
                    passedTest = false;
                    notify("Common word detected",
                            "You used a common word in your password.  Please choose a different password.");

                }
            }

        }

        return passedTest;
    }
    
    protected static boolean containsCommonWords(String password) {
        ZipInputStream zip = null;
        try {
            zip = new ZipInputStream(
                    LoginDialog.class.getResourceAsStream("/common-passwords.zip"));
            ZipEntry entry = null;
            for (entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    final byte buffer[] = new byte[4096];
                    int readCount;
                    while ((readCount = zip.read(buffer)) > 0) {
                        os.write(buffer, 0, readCount);
                    }
                } finally {
                    os.close();
                }

                String text = new String(os.toByteArray());
                String[] wordsArray = text.split("\\r?\\n");
                for (String word : wordsArray) {
                    if (password.contains(word)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(zip);
        }
    }

    protected static boolean containsAlphanumeric(String password) {
        boolean containsAlpha = false;
        boolean containsNumeric = false;
        char[] chars = password.toCharArray();
        for (char c : chars) {
            containsAlpha |= Character.isLetter(c);
            containsNumeric |= Character.isDigit(c);
        }
        return containsAlpha & containsNumeric;
    }

    protected static boolean containsMixedCase(String password) {
        boolean containsLower = false;
        boolean containsUpper = false;
        char[] chars = password.toCharArray();
        for (char c : chars) {
            containsLower |= Character.isLowerCase(c);
            containsUpper |= Character.isUpperCase(c);
        }
        return containsLower & containsUpper;
    }

    protected static boolean containsSymbol(String password) {
        Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(password);
        return m.find();
    }

    protected void login(String username, String password) {
        IOperationsService operationsService = context.getOperationsService();
        User user = operationsService.findUserByLoginId(username);
        if (user != null) {
            // TODO: Create an authentication service that can lookup the necessary authentication method.
            IConsoleUserAuthentication authenticationMethod = null;
            if (UserAuthenticationLDAP.AUTHENTICATION_METHOD.equals(user.getAuthMethod())) {
                authenticationMethod = new UserAuthenticationLDAP();
            } else {
                // Legacy systems have an auth method of SHASH which should be handled as INTERNAL.
                authenticationMethod = new UserAuthenticationInternal();
            }
            
            if (isNewPasswordMode()) {
                if (testPassword(passwordField, validatePasswordField, context)) {
                    operationsService.savePassword(user, passwordField.getValue());
                    UI.getCurrent().removeWindow(this);
                    loginListener.login(user);
                }
            } else {
                try {
                    AuthenticationStatus status = authenticationMethod.authenticate(username, password, context);
                    if (status.equals(AuthenticationStatus.VALID)) {
                        UI.getCurrent().removeWindow(this);
                        loginListener.login(user);
                    } else if (status.equals(AuthenticationStatus.LOCKED)) {
                        CommonUiUtils.notify(String.format("User '%s' is locked.  Please contact an admin to reset your password."
                                , user.getLoginId()), Type.WARNING_MESSAGE);
                    } else if (status.equals(AuthenticationStatus.EXPIRED)) {
                        userNameField.setVisible(false);
                        passwordField.setValue(null);
                        setCaption(PASSWORD_EXPIRED);
                        passwordField.setCaption("New Password");
                        loginButton.setCaption("Change Password");
                        validatePasswordField.setVisible(true);
                    } else {
                        CommonUiUtils.notify("Invalid user id or password", Type.WARNING_MESSAGE);
                    }
                } catch(ConsoleAuthenticationConnectionException ex) {
                    CommonUiUtils.notify("Unable to connect to network resource.", Type.WARNING_MESSAGE);
                } catch(ConsoleAuthenticationCredentialException ex) {
                    CommonUiUtils.notify("Invalid user id or password", Type.WARNING_MESSAGE);
                } catch(Throwable t) {
                    log.error("", t);
                    CommonUiUtils.notify(t);
                }
            }
            
        } else {
            CommonUiUtils.notify("Invalid user id or password", Type.WARNING_MESSAGE);
        }
        
    }

    protected static void notify(String caption, String message) {
        new Notification(caption, message).show(Page.getCurrent());
    }

    static public interface LoginListener {
        public void login(User user);
    }

}
