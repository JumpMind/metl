package org.jumpmind.metl.ui.common;

import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;

@Component
@Scope(value="ui")
@Order(value=20)
public class TopBarButtonHelp extends TopBarButton {

    private static final long serialVersionUID = 1L;

    public TopBarButtonHelp() {
        super("Help", FontAwesome.QUESTION_CIRCLE);
        addClickListener(event -> openHelp(event));
    }

    protected void openHelp(ClickEvent event) {
        String docUrl = Page.getCurrent().getLocation().toString();
        docUrl = docUrl.substring(0, docUrl.lastIndexOf("/"));
        Page.getCurrent().open(docUrl + "/doc/html/user-guide.html", "doc");
    }

}
