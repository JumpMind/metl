package org.jumpmind.metl.ui.init;

import org.jsoup.nodes.Element;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

public class AppServiceInitListener implements VaadinServiceInitListener {
    private static final long serialVersionUID = 1L;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addBootstrapListener(response -> {
            final Element head = response.getDocument().head();
            head.appendElement("meta").attr("name", "viewport")
                    .attr("content", "width=device-width, initial-scale=1");
            head.appendElement("meta")
                    .attr("name", "apple-mobile-web-app-capable")
                    .attr("content", "yes");
            head.appendElement("meta")
                    .attr("name", "apple-mobile-web-app-status-bar-style")
                    .attr("content", "black");
        });
    }

}
