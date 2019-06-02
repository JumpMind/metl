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
package org.jumpmind.metl.core.runtime.web;

import java.io.Serializable;

import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.runtime.component.SecurityScheme;

public class HttpRequestMapping implements Serializable, Comparable<HttpRequestMapping> {

    private static final long serialVersionUID = 1L;

    String path;

    HttpMethod method;

    AgentDeploy deployment;

    int priority;
    
    SecurityScheme securityScheme;
    
    String securityUsername;
    
    String securityPassword;
    
    String requestDescription;
    
    String responseDescription;
    
    String flowDescription;
    
    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setDeployment(AgentDeploy deployment) {
        this.deployment = deployment;
    }

    public AgentDeploy getDeployment() {
        return deployment;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public void setSecurityScheme(SecurityScheme securityScheme) {
        this.securityScheme = securityScheme;
    }
    
    public SecurityScheme getSecurityScheme() {
        return securityScheme;
    }
    
    public void setSecurityUsername(String securityUsername) {
        this.securityUsername = securityUsername;
    }
    
    public String getSecurityUsername() {
        return securityUsername;
    }
    
    public void setSecurityPassword(String securityPassword) {
        this.securityPassword = securityPassword;
    }
    
    public String getSecurityPassword() {
        return securityPassword;
    }
    
    public void setRequestDescription(String description) {
        this.requestDescription = description;
    }
    
    public String getRequestDescription() {
        return requestDescription;
    }
    
    public void setResponseDescription(String responseDescription) {
        this.responseDescription = responseDescription;
    }
    
    public String getResponseDescription() {
        return responseDescription;
    }
    
    public void setFlowDescription(String flowDescription) {
        this.flowDescription = flowDescription;
    }
    
    public String getFlowDescription() {
        return flowDescription;
    }

    @Override
    public int compareTo(HttpRequestMapping o) {
        int compare = new Integer(priority).compareTo(new Integer(o.getPriority()));
        if (compare == 0) {
            compare = path.compareTo(o.path);
        }
        return compare;
    }

    @Override
    public int hashCode() {
        return deployment.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HttpRequestMapping) {
            return deployment.equals(((HttpRequestMapping) obj).deployment);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("{ \"requestmapping\":[\"path\":").append(path).append("\",\"deployment\":\"").append(deployment.getName())
                .append("\"]}").toString();
    }

}
