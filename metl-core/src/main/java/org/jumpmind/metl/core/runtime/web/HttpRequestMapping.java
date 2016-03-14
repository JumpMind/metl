package org.jumpmind.metl.core.runtime.web;

import java.io.Serializable;

import org.jumpmind.metl.core.model.AgentDeployment;

public class HttpRequestMapping implements Serializable, Comparable<HttpRequestMapping> {

    private static final long serialVersionUID = 1L;

    String path;
    
    HttpMethod method;
    
    String contentType;
    
    AgentDeployment deployment;
    
    int priority;

    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
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
    
    public void setDeployment(AgentDeployment deployment) {
        this.deployment = deployment;
    }
    
    public AgentDeployment getDeployment() {
        return deployment;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    @Override
    public int compareTo(HttpRequestMapping o) {
        return new Integer(priority).compareTo(new Integer(o.getPriority()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((contentType == null) ? 0 : contentType.hashCode());
        result = prime * result + ((deployment == null) ? 0 : deployment.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HttpRequestMapping other = (HttpRequestMapping) obj;
        if (contentType == null) {
            if (other.contentType != null)
                return false;
        } else if (!contentType.equals(other.contentType))
            return false;
        if (deployment == null) {
            if (other.deployment != null)
                return false;
        } else if (!deployment.equals(other.deployment))
            return false;
        if (method != other.method)
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
    }
    
    

}
