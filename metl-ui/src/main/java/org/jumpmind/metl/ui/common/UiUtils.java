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

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.ui.definition.XMLComponentUI;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;

public final class UiUtils {

    private UiUtils() {
    }
    
    public static String whereAreYou(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("Proxy-Client-IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("WL-Proxy-Client-IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_CLIENT_IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getRemoteAddr();  
        }  
        return ip;  
    }    

    public static Label getName(String filter, String name) {
        if (isNotBlank(filter) && isNotBlank(name)) {
            int[] startEndIndex = getFilterMatchRange(filter, name);
            if (startEndIndex[0] != -1) {
                String pre = startEndIndex[0] < name.length() ? name.substring(0, startEndIndex[0]) : "";
                String highlighted = name.substring(startEndIndex[0], startEndIndex[1]);
                String post = startEndIndex[1] < name.length() ? name.substring(startEndIndex[1]) : "";
                name = pre + "<span class='highlight'>" + highlighted + "</span>" + post;
            }
        }
        Label label = new Label(name, ContentMode.HTML);
        return label;
    }

    public static boolean filterMatches(String needle, String haystack) {
        return getFilterMatchRange(needle, haystack)[0] != -1;
    }

    public static int[] getFilterMatchRange(String needle, String haystack) {
        int startIndex = -1;
        int endIndex = 0;
        if (needle != null && isNotBlank(needle)) {
            needle = needle.toLowerCase();
            haystack = haystack.toLowerCase();
            for (String filterStr : needle.split("\\*")) {
                endIndex = haystack.indexOf(filterStr, endIndex);
                if (endIndex == -1) {
                    startIndex = -1;
                    break;
                } else {
                    if (startIndex == -1) {
                        startIndex = endIndex;
                    }
                    endIndex += filterStr.length();
                }
            }
        } else {
            startIndex = 0;
            endIndex = haystack.length();
        }
        return new int[] { startIndex, endIndex };
    }
    
    public static String getBase64RepresentationOfImageForComponentType(String projectVersionId, String type, ApplicationContext context) {
        InputStream is = getComponentImageInputStream(projectVersionId, type, context);
        if (is != null) {
            try {
                byte[] bytes = IOUtils.toByteArray(is);
                return new String(Base64.encodeBase64(bytes));
            } catch (IOException e) {
                throw new IoException(e);
            }
        } else {
            return null;
        }
    }

    public static InputStream getComponentImageInputStream(String projectVersionId, String type, ApplicationContext context) {
        XMLComponentUI definition = context.getUiFactory().getUiDefinition(projectVersionId, type);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = null;
        if (definition != null && isNotBlank(definition.getIconImage())) {
                String icon = definition.getIconImage();
                classLoader = definition.getClassLoader();     
                is = classLoader.getResourceAsStream(icon);
                if (is == null && icon.startsWith("/")) {
                    is  = classLoader.getResourceAsStream(icon.substring(1));
                }
                if (is == null && !icon.startsWith("/")) {
                    is = classLoader.getResourceAsStream("/" + icon);
                }
        }
        
        if (is == null) {
            is = UiUtils.class.getResourceAsStream("/org/jumpmind/metl/core/runtime/component/metl-puzzle-48x48-color.png");
        }
        return is;
    }

}
