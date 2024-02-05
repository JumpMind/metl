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
package org.jumpmind.metl.ui.views.admin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.metl.ui.common.IBackgroundRefreshable;
import org.jumpmind.metl.ui.init.BackgroundRefresherService;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.Scroller.ScrollDirection;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;

@SuppressWarnings("serial")
@UiComponent
@Scope(value = "ui")
@Order(1300)
@AdminMenuLink(name = "Logging", id = "Logging", icon = VaadinIcon.FILE_TEXT_O)
public class LoggingPanel extends AbstractAdminPanel implements IBackgroundRefreshable<Object> {

    BackgroundRefresherService backgroundRefresherService;

    TextField bufferSize;

    TextField filter;

    Checkbox autoRefreshOn;

    Pre logView;

    Scroller logPanel;

    File logFile;

    public LoggingPanel() {
    }
    
    @PostConstruct
    @Override
    public void init() {
        this.backgroundRefresherService = context.getBackgroundRefresherService();
        if (LogUtils.isFileEnabled()) {
            logFile = new File(LogUtils.getLogFilePath());
        }
        setSizeFull();
        setSpacing(true);
        setMargin(true);

        HorizontalLayout topPanelLayout = new HorizontalLayout();
        topPanelLayout.setWidthFull();
        topPanelLayout.setSpacing(true);

        Button refreshButton = new Button("Refresh");
        refreshButton.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
            public void onComponentEvent(ClickEvent<Button> event) {
                refresh();
            }
        });
        topPanelLayout.add(refreshButton);
        topPanelLayout.setVerticalComponentAlignment(Alignment.END, refreshButton);

        bufferSize = new TextField();
        bufferSize.setWidth("5em");
        bufferSize.setValue("1000");
        bufferSize.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<String>>() {
            public void valueChanged(ValueChangeEvent<String> event) {
                refresh();
            }
        });
        topPanelLayout.add(bufferSize);

        filter = new TextField();
        filter.setPlaceholder("Filter");
        filter.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        filter.setValueChangeMode(ValueChangeMode.LAZY);
        filter.setValueChangeTimeout(200);
        filter.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<String>>() {
            public void valueChanged(ValueChangeEvent<String> event) {
                refresh();
            }
        });
        topPanelLayout.add(filter);
        topPanelLayout.setVerticalComponentAlignment(Alignment.END, filter);

        autoRefreshOn = new Checkbox("Auto Refresh");
        autoRefreshOn.setValue(true);
        topPanelLayout.add(autoRefreshOn);
        topPanelLayout.setVerticalComponentAlignment(Alignment.END, autoRefreshOn);

        Span spacer = new Span();
        topPanelLayout.addAndExpand(spacer);

        if (logFile != null && logFile.exists()) {
            Button downloadButton = new Button("Download log file");
            downloadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);

            Anchor fileDownloader = new Anchor(getLogFileResource(), null);
            fileDownloader.getElement().setAttribute("download", true);
            fileDownloader.add(downloadButton);
            topPanelLayout.add(fileDownloader);
            topPanelLayout.setVerticalComponentAlignment(Alignment.END, fileDownloader);
        }

        add(topPanelLayout);

        logPanel = new Scroller(ScrollDirection.VERTICAL);
        add(new H3("Log Output"));
        logPanel.setSizeFull();
        logView = new Pre("");
        logView.setSizeUndefined();
        logPanel.setContent(logView);
        addAndExpand(logPanel);
        refresh();
        backgroundRefresherService.register(this);
    }

    private StreamResource getLogFileResource() {
        InputStreamFactory factory = new InputStreamFactory() {
            private static final long serialVersionUID = 1L;

            public InputStream createInputStream() {
                try {
                    return new BufferedInputStream(new FileInputStream(logFile));
                } catch (FileNotFoundException e) {
                    CommonUiUtils.notify("Could not find " + logFile.getName() + " to download");
                    return null;
                }
            }
        };
        StreamResource sr = new StreamResource(logFile.getName(), factory);
        return sr;
    }

    protected void refresh() {
        onBackgroundUIRefresh(onBackgroundDataRefresh());
    }

    @Override
    public boolean closing() {
        backgroundRefresherService.unregister(this);
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
    }

    @Override
    public Object onBackgroundDataRefresh() {
        StringBuilder builder = null;
        if (logFile != null && logFile.exists() && autoRefreshOn.getValue()) {
            try {
                builder = new StringBuilder();
                Pattern pattern = Pattern.compile("^\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d,\\d\\d\\d .*");
                String filterValue = filter.getValue();
                boolean isFiltering = !StringUtils.isBlank(filterValue);
                Pattern filter = Pattern.compile("(.*)(" + filterValue + ")(.*)");
                ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile);
                try {
                    int lines = Integer.parseInt(bufferSize.getValue());
                    int counter = 0;
                    String line = null;
                    do {
                        if (!isFiltering) {
                            line = StringEscapeUtils.escapeHtml(reader.readLine());
                        } else {
                            StringBuilder multiLine = new StringBuilder();
                            while ((line = StringEscapeUtils.escapeHtml(reader.readLine())) != null) {
                                if (pattern.matcher(line).matches()) {
                                    multiLine.insert(0, line);
                                    line = multiLine.toString();
                                    break;
                                } else {
                                    multiLine.insert(0, line + "<br/>");
                                    counter++;
                                }
                            }
                        }
                        
                        if (line != null) {
                            boolean showLine = !isFiltering;
                            if (isFiltering) {
                                Matcher matcher = filter.matcher(line);
                                if (showLine = matcher.matches()) {
                                    line = matcher.replaceAll("$1<font color='red'>$2</font>$3");
                                }
                            }
                            if (showLine) {
                                builder.insert(0, line + "<br/>");
                                counter++;
                            }
                        }
                    } while (line != null && counter < lines);
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return builder;
    }

    @Override
    public void onBackgroundUIRefresh(Object backgroundData) {
        if (backgroundData != null) {
            StringBuilder builder = (StringBuilder) backgroundData;
            logView.setText(builder.toString());
            UI.getCurrent().getPage().executeJs("$0.scrollTop = $0.scrollHeight;", logPanel.getElement());
        }
    }
    
    @Override
    public void onUIError(Throwable ex) {
        CommonUiUtils.notifyError();
    }
}
