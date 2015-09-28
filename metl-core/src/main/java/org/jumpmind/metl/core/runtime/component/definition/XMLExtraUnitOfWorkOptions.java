package org.jumpmind.metl.core.runtime.component.definition;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "extraUnitOfWorkOptions", propOrder = {
    "extraUnitOfWorkOption"
})
public class XMLExtraUnitOfWorkOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement(name="extraUnitOfWorkOption", required = true)
    protected List<String> extraUnitOfWorkOption;
    
    public void setExtraUnitOfWorkOption(List<String> extraUnitOfWorkOption) {
        this.extraUnitOfWorkOption = extraUnitOfWorkOption;
    }
    
    public List<String> getExtraUnitOfWorkOption() {
        return extraUnitOfWorkOption;
    }
    
}
