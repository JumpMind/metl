package org.jumpmind.metl.ui.views.admin;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jumpmind.metl.core.runtime.AgentManager;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.Table;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.util.AppUtils;

import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.VerticalLayout;

public class AboutPanel extends VerticalLayout implements IUiPanel {

    final Log log = LogFactory.getLog(getClass());

    private static final long serialVersionUID = 1L;

    Table table;

    public AboutPanel(ApplicationContext context, TabbedPanel tabbedPanel) {
        setSizeFull();
        setMargin(true);
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
        if (table != null) {
            removeComponent(table);
        }
        table = new Table();
        table.setSizeFull();
        table.addStyleName("noscroll");
        table.addContainerProperty("Name", String.class, null);
        table.setColumnWidth("Name", 200);
        table.addContainerProperty("Value", String.class, null);

        int itemId = 0;
        ServletContext context = VaadinServlet.getCurrent().getServletContext();
        Properties properties = new Properties();
        try {
            properties.load(context.getResourceAsStream("/version.properties"));

            table.addItem(new Object[] { "Application Version", properties.getProperty("appVersion") }, itemId++);

            table.addItem(new Object[] { "Build Time", properties.getProperty("buildTime") }, itemId++);

            table.addItem(new Object[] { "SCM Revision", properties.getProperty("scmVersion") }, itemId++);

            String branch = properties.getProperty("scmBranch");
            if (isNotBlank(branch) && branch.contains("/")) {
                branch = branch.substring(branch.indexOf("/") + 1);
            }

            table.addItem(new Object[] { "SCM Branch", branch }, itemId++);

        } catch (IOException e) {
            log.error("Could not load version.properties", e);
        }

        table.addItem(new Object[] { "Host Name", AppUtils.getHostName() }, itemId++);
        table.addItem(new Object[] { "IP Address", AppUtils.getIpAddress() }, itemId++);
        table.addItem(new Object[] { "Java Version", System.getProperty("java.version") }, itemId++);
        table.addItem(new Object[] { "System Time", FastDateFormat.getTimeInstance(FastDateFormat.MEDIUM).format(new Date()) }, itemId++);
        table.addItem(new Object[] { "Used Heap", Long.toString(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) },
                itemId++);
        table.addItem(new Object[] { "Heap Size", Long.toString(Runtime.getRuntime().maxMemory()) }, itemId++);
        table.addItem(
                new Object[] { "Last Restart", FastDateFormat.getTimeInstance(FastDateFormat.MEDIUM).format(AgentManager.lastRestartTime) },
                itemId++);

        addComponent(table);
        setExpandRatio(table, 1);

    }

}