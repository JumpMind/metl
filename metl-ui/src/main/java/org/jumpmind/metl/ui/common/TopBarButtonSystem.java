package org.jumpmind.metl.ui.common;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.GlobalSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.vaadin.server.FontAwesome;

@Component
@Scope(value="ui")
@Order(value=10)
public class TopBarButtonSystem extends TopBarButton {

    private static final long serialVersionUID = 1L;

    @Autowired
    protected ApplicationContext context;

    public TopBarButtonSystem() {
        super(FontAwesome.WARNING);
    }
    
    @PostConstruct
    public void init() {
        String caption = "";
        GlobalSetting setting = context.getOperationsService().findGlobalSetting(GlobalSetting.SYSTEM_TEXT);
        if (setting != null) {
            caption = setting.getValue();
        }

        if (StringUtils.isNotBlank(caption)) {
            setCaption(caption);
            setHtmlContentAllowed(true);
        } else {
            setVisible(false);
        }
    }

}
