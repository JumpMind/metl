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
package org.jumpmind.metl.core.plugin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "component", propOrder = {})
public class XMLComponentDefinition extends XMLAbstractDefinition {

    private static final long serialVersionUID = 1L;
    
    @XmlType
    @XmlEnum(String.class)
    public enum MessageType {
        @XmlEnumValue("none")
        NONE(null), @XmlEnumValue("entity")
        ENTITY("E"), @XmlEnumValue("text")
        TEXT("T"), @XmlEnumValue("binary")
        BINARY("B"), @XmlEnumValue("any")
        ANY("*");

        private String letter;

        private MessageType(String letter) {
            this.letter = letter;
        }

        public String getLetter() {
            return letter;
        }
    }

    @XmlType
    @XmlEnum
    public enum ResourceCategory {
        @XmlEnumValue("datasource")
        DATASOURCE, 
        @XmlEnumValue("streamable")
        STREAMABLE,
        @XmlEnumValue("mailsession")
        MAIL_SESSION,
        @XmlEnumValue("http")
        HTTP,
        @XmlEnumValue("subscribe")
        SUBSCRIBE,         
        @XmlEnumValue("none")
        NONE, 
        @XmlEnumValue("any")
        ANY
    }

    @XmlElement(required = true)
    protected String keywords = "";
    
    @XmlElement(required = false)
    protected String deploymentListenerClassName;
    
    @XmlElement(required = true)
    protected String flowManipulatorClassName;    

    @XmlAttribute(required = true)
    protected String category;

    @XmlAttribute(required = false)
    protected boolean shareable;
    
    @XmlAttribute(required = false)
    protected boolean autoSendControlMessages = true;
    
    @XmlAttribute(required = false)
    protected boolean supportsMultipleThreads;

    @XmlAttribute(required = false)
    protected boolean inputOutputModelsMatch;

    @XmlAttribute(required = false)
    protected boolean showInputModel = false;

    @XmlAttribute(required = false)
    protected boolean showOutputModel = false;
    
    @XmlAttribute(required = false)
    protected MessageType inputMessageType;

    @XmlAttribute(required = false)
    protected MessageType outputMessageType;

    @XmlAttribute(required = false)
    protected ResourceCategory resourceCategory;
    
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isShareable() {
        return shareable;
    }

    public void setShareable(boolean shareable) {
        this.shareable = shareable;
    }

    public boolean isInputOutputModelsMatch() {
        return inputOutputModelsMatch;
    }

    public void setInputOutputModelsMatch(boolean inputOutputModelsMatch) {
        this.inputOutputModelsMatch = inputOutputModelsMatch;
    }

    public MessageType getInputMessageType() {
        return inputMessageType;
    }

    public void setInputMessageType(MessageType inputMessageType) {
        this.inputMessageType = inputMessageType;
    }

    public MessageType getOutputMessageType() {
        return outputMessageType;
    }

    public void setOutputMessageType(MessageType outputMessageType) {
        this.outputMessageType = outputMessageType;
    }
    
    public void setAutoSendControlMessages(boolean autoSendControlMessages) {
        this.autoSendControlMessages = autoSendControlMessages;
    }
    
    public boolean isAutoSendControlMessages() {
        return autoSendControlMessages;
    }

    public ResourceCategory getResourceCategory() {
        return resourceCategory;
    }

    public void setResourceCategory(ResourceCategory resourceCategory) {
        this.resourceCategory = resourceCategory;
    }

    public boolean isSupportsMultipleThreads() {
        return supportsMultipleThreads;
    }
    
    public void setSupportsMultipleThreads(boolean supportsMultipleThreads) {
        this.supportsMultipleThreads = supportsMultipleThreads;
    }

    public void setFlowManipulatorClassName(String flowManipulatorClassName) {
        this.flowManipulatorClassName = flowManipulatorClassName;
    }
    
    public String getFlowManipulatorClassName() {
        return flowManipulatorClassName;
    }
    
    public void getDeploymentListenerClassName(String deploymentListenerClassname) {
        this.deploymentListenerClassName = deploymentListenerClassname;
    }
    
    public String getDeploymentListenerClassName() {
        return deploymentListenerClassName;
    }
    
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    
    public String getKeywords() {
        return keywords;
    }
    
    public boolean isShowInputModel() {
        return showInputModel;
    }
    
    public void setShowInputModel(boolean showInputModel) {
        this.showInputModel = showInputModel;
    }
    
    public boolean isShowOutputModel() {
        return showOutputModel;
    }
    
    public void setShowOutputModel(boolean showOutputModel) {
        this.showOutputModel = showOutputModel;
    }
    
}
