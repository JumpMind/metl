package org.jumpmind.symmetric.is.ui.views.admin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang3.StringUtils;
import org.jumpmind.symmetric.is.core.util.EnvConstants;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.IBackgroundRefreshable;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class LoggingPanel extends NamedPanel implements IBackgroundRefreshable {

    ApplicationContext context;

    TabbedPanel tabbedPanel;

    TextField bufferSize;

    TextField filter;

    CheckBox autoRefreshOn;

    Label logView;

    Panel logPanel;

    File logFile;

    public LoggingPanel(ApplicationContext context, TabbedPanel tabbedPanel, String caption,
            Resource icon) {
        super(caption, icon);
        this.context = context;
        this.tabbedPanel = tabbedPanel;
        boolean fileEnabled = Boolean.parseBoolean(context.getEnvironment().getProperty(
                EnvConstants.LOG_TO_FILE_ENABLED, "true"));
        if (fileEnabled) {
            logFile = new File(context.getEnvironment().getProperty(EnvConstants.LOG_FILE,
                    "logs/application.log"));
        }
        setSizeFull();
        setSpacing(true);
        setMargin(true);

        HorizontalLayout topPanelLayout = new HorizontalLayout();
        topPanelLayout.setWidth(100, Unit.PERCENTAGE);
        topPanelLayout.setSpacing(true);

        Button refreshButton = new Button("Refresh");
        refreshButton.addClickListener(new ClickListener() {
            public void buttonClick(ClickEvent event) {
                refresh();
            }
        });
        topPanelLayout.addComponent(refreshButton);
        topPanelLayout.setComponentAlignment(refreshButton, Alignment.BOTTOM_LEFT);

        bufferSize = new TextField();
        bufferSize.setImmediate(true);
        bufferSize.setWidth(5, Unit.EM);
        bufferSize.setValue("1000");
        bufferSize.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                refresh();
            }
        });
        topPanelLayout.addComponent(bufferSize);

        filter = new TextField();
        filter.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        filter.setInputPrompt("Filter");
        filter.setIcon(FontAwesome.SEARCH);
        filter.setNullRepresentation("");
        filter.setImmediate(true);
        filter.setTextChangeEventMode(TextChangeEventMode.LAZY);
        filter.setTextChangeTimeout(200);
        filter.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                refresh();
            }
        });
        topPanelLayout.addComponent(filter);
        topPanelLayout.setComponentAlignment(filter, Alignment.BOTTOM_LEFT);

        autoRefreshOn = new CheckBox("Auto Refresh");
        autoRefreshOn.setValue(true);
        autoRefreshOn.setImmediate(true);
        topPanelLayout.addComponent(autoRefreshOn);
        topPanelLayout.setComponentAlignment(autoRefreshOn, Alignment.BOTTOM_LEFT);

        Label spacer = new Label();
        topPanelLayout.addComponent(spacer);
        topPanelLayout.setExpandRatio(spacer, 1);

        if (logFile != null && logFile.exists()) {
            Button downloadButton = new Button("Download log file");
            downloadButton.addStyleName(ValoTheme.BUTTON_LINK);
            downloadButton.addStyleName(ValoTheme.BUTTON_SMALL);

            FileDownloader fileDownloader = new FileDownloader(getLogFileResource());
            fileDownloader.extend(downloadButton);
            topPanelLayout.addComponent(downloadButton);
            topPanelLayout.setComponentAlignment(downloadButton, Alignment.BOTTOM_RIGHT);
        }

        addComponent(topPanelLayout);

        logPanel = new Panel("Log Output");
        logPanel.setSizeFull();
        logView = new Label("", ContentMode.PREFORMATTED);
        logView.setSizeUndefined();
        logPanel.setContent(logView);
        addComponent(logPanel);
        setExpandRatio(logPanel, 1);
        refresh();
    }

    private StreamResource getLogFileResource() {
        StreamSource ss = new StreamSource() {
            public InputStream getStream() {
                try {
                    return new BufferedInputStream(new FileInputStream(logFile));
                } catch (FileNotFoundException e) {
                    Notification note = new Notification("File Not Found", "Could not find "
                            + logFile.getName() + " to download");
                    note.show(Page.getCurrent());
                    return null;
                }
            }
        };
        return new StreamResource(ss, logFile.getName());
    }

    protected void refresh() {
        if (logFile != null && logFile.exists()) {
            try {
                StringBuilder builder = new StringBuilder();
                ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile);
                String filterValue = filter.getValue();
                try {
                    int lines = Integer.parseInt(bufferSize.getValue());
                    int counter = 0;
                    String line = null;
                    do {
                        line = reader.readLine();
                        if (line != null) {
                            if (StringUtils.isBlank(filterValue) || line.contains(filterValue)) {
                                builder.insert(0, line + "\n");
                                counter++;
                            }
                        }
                    } while (line != null && counter < lines);
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }
                logView.setValue(builder.toString());
                logPanel.setScrollTop(1000000);
                logPanel.markAsDirty();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object onBackgroundDataRefresh() {
        return null;
    }

    @Override
    public void onBackgroundUIRefresh(Object backgroundData) {

    }

}
