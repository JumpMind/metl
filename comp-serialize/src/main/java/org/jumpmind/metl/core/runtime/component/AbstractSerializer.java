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
package org.jumpmind.metl.core.runtime.component;

import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.FORMAT;
import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.FORMAT_AUTOMATIC;
import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.FORMAT_JSON;
import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.FORMAT_XML;
import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.STRUCTURE;
import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.STRUCTURE_BY_INBOUND_ROW;

import org.apache.http.HttpHeaders;
import org.springframework.util.MimeTypeUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
public abstract class AbstractSerializer extends AbstractComponentRuntime {



    protected String format;
    protected String structure;

    @Override
    public void start() {
        format = properties.get(FORMAT, FORMAT_AUTOMATIC);
        structure = properties.get(STRUCTURE, STRUCTURE_BY_INBOUND_ROW);
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }
    
    protected String getDetectedFormat() {
        String detectedFormat = null;
        if (format.equals(FORMAT_AUTOMATIC)) {
            if (FORMAT_XML.equalsIgnoreCase(context.getFlowParameters().get(FORMAT))) {
                detectedFormat = FORMAT_XML;
            } else if (FORMAT_JSON.equalsIgnoreCase(context.getFlowParameters().get(FORMAT))) {
                detectedFormat = FORMAT_JSON;
            } else if (MimeTypeUtils.APPLICATION_XML.toString()
                    .equals(context.getFlowParameters().get(HttpHeaders.CONTENT_TYPE))) {
                detectedFormat = FORMAT_XML;
            } else if (MimeTypeUtils.APPLICATION_JSON.toString()
                    .equals(context.getFlowParameters().get(HttpHeaders.CONTENT_TYPE))) {
                detectedFormat = FORMAT_JSON;
            } else if (MimeTypeUtils.APPLICATION_XML.toString()
                    .equals(context.getFlowParameters().get(HttpHeaders.ACCEPT))) {
                detectedFormat = FORMAT_XML;
            } else {
                detectedFormat = FORMAT_JSON;
            }
        }
        return detectedFormat;
    }

    protected ObjectMapper getObjectMapper() {
        String detectedFormat = getDetectedFormat();

        ObjectMapper mapper = null;
        if (FORMAT_XML.equals(detectedFormat)) {
            mapper = new XmlMapper();
        } else {
            mapper = new ObjectMapper();
        }
        return mapper;
    }

}
