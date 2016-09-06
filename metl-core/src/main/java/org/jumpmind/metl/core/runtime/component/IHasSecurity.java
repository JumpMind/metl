package org.jumpmind.metl.core.runtime.component;

public interface IHasSecurity {

    public SecurityScheme getSecurityType();
    public String getUsername();
    public String getPassword();
    
}
