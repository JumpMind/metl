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
package org.jumpmind.metl.core.util;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.io.PrintStream;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class SftpUtils {
	public static int DEFAULT_OPTION = JOptionPane.DEFAULT_OPTION;
	public static int YES_NO_OPTION = JOptionPane.YES_NO_OPTION;
	public static int YES_NO_CANCEL_OPTION = JOptionPane.YES_NO_CANCEL_OPTION;
	public static int OK_CANCEL_OPTION = JOptionPane.OK_CANCEL_OPTION;
	
	public static int ERROR_MESSAGE = JOptionPane.ERROR_MESSAGE;
	public static int INFORMATION_MESSAGE = JOptionPane.INFORMATION_MESSAGE;
	public static int WARNING_MESSAGE = JOptionPane.WARNING_MESSAGE;
	public static int QUESTION_MESSAGE = JOptionPane.QUESTION_MESSAGE;
	public static int PLAIN_MESSAGE = JOptionPane.PLAIN_MESSAGE;
	
	private static boolean headless;
	private static PrintStream out;
	
	static {
		headless = isReallyHeadless();
		out = System.out;
	}
	
	public static boolean isHeadless() {return headless;}
	
	private static boolean isReallyHeadless() {
        if (GraphicsEnvironment.isHeadless()) {
            return true;
        }
        try {
            GraphicsDevice[] screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            return screenDevices == null || screenDevices.length == 0;
        } catch (HeadlessException e) {
            e.printStackTrace();
            return true;
        }
    }

	public static String showInputDialog(String prompt, String defaultValue) {
		String host = defaultValue;
		if(headless) {
			String message = prompt + " (default: " + defaultValue + ") ";
			host = getLine(message);
		} else {
			host=JOptionPane.showInputDialog(prompt, defaultValue);
		}
		return host;
	}
	
	public static int showOptionDialog(Object message, String title, int optionType, int messageType,
			Icon icon, Object[] options, Object initialValue)
	{
		int ret = 0;
		if(headless) {
			String defaultValue = initialValue != null && initialValue.toString().length() > 0 ? initialValue.toString() : "";
			String optionsValues = "";
			for(Object option : options) {
				optionsValues += option + ", ";
			}
			String prompt = message + ": " + optionsValues + " ( default: " + defaultValue + ") ";
			String choice = getLine(prompt);
			if(choice == null || choice.length() == 0) {
				// Nothing entered
				choice = defaultValue;
			} else {
				// Check to see if entered value matches any options
				boolean foundOption = false;
				for(Object option : options) {
					if(choice.equalsIgnoreCase(option.toString())) {
						foundOption = true;
						break;
					}
				}
				if(! foundOption) {
					choice = defaultValue;
				}
			}
			int i = 0;
			for(Object option : options) {
				if(choice.equalsIgnoreCase(option.toString())) {
					ret = i;
					break;
				}
				i++;
			}
		} else {
			ret = JOptionPane.showOptionDialog(null, message, title, optionType, messageType, icon, options, initialValue);
		}
		return ret;
	}
	
	public static String[] promptKeyboardInteractive(String destination,
			String name,
			String instruction,
			String[] prompt,
			boolean[] echo)
	{
		if(headless) {
			String[] response=new String[prompt.length];
			for(int i = 0; i < prompt.length; i++) {
				if(echo[i]) {
					response[i] = getLine(prompt[i]);
				} else {
					response[i] = getPassword(prompt[i]);
				}
			}
			return response;
		} else {
			final GridBagConstraints gbc = 
					new GridBagConstraints(0,0,1,1,1,1,
							GridBagConstraints.NORTHWEST,
							GridBagConstraints.NONE,
							new Insets(0,0,0,0),0,0);
			JPanel panel = new JPanel();
			panel.setLayout(new GridBagLayout());
	
			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.gridx = 0;
			panel.add(new JLabel(instruction), gbc);
			gbc.gridy++;
	
			gbc.gridwidth = GridBagConstraints.RELATIVE;
	
			JTextField[] texts=new JTextField[prompt.length];
			for(int i=0; i<prompt.length; i++){
				gbc.fill = GridBagConstraints.NONE;
				gbc.gridx = 0;
				gbc.weightx = 1;
				panel.add(new JLabel(prompt[i]),gbc);
	
				gbc.gridx = 1;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weighty = 1;
				if(echo[i]){
					texts[i]=new JTextField(20);
				}
				else{
					texts[i]=new JPasswordField(20);
				}
				panel.add(texts[i], gbc);
				gbc.gridy++;
			}
	
			if(JOptionPane.showConfirmDialog(null, panel, 
					destination+": "+name,
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE)
					==JOptionPane.OK_OPTION){
				String[] response=new String[prompt.length];
				for(int i=0; i<prompt.length; i++){
					response[i]=texts[i].getText();
				}
				return response;
			}
			else{
				return null;  // cancel
			}
		}
	}
	
	public static String promptPassword(String message) {
		if(headless) {
			return getPassword(message);
		} else {
			String passwd;
		    JTextField passwordField=(JTextField)new JPasswordField(20);
			Object[] ob={passwordField}; 
			int result=
					JOptionPane.showConfirmDialog(null, ob, message,
							JOptionPane.OK_CANCEL_OPTION);
			if(result==JOptionPane.OK_OPTION){
				passwd=passwordField.getText();
				return passwd;
			}
			else {
				return null;
			}
		}
	}
	
	public static void showNote(String note) {
		out.print(note);
	}

	private static String getLine(String message) {
		showNote(message);
		return System.console().readLine();
	}
	
	private static String getPassword(String message) {
		return new String(System.console().readPassword("%s", message));
	}
}
