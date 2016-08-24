package org.jumpmind.metl.core.runtime.component;

public interface IHasSecurity {

    public SecurityType getSecurityType();
    public String getUsername();
    public String getPassword();
    
}
