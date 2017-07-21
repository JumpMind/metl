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
package org.jumpmind.metl.ui.views.design;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamedMap;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition.ResourceCategory;
import org.jumpmind.metl.core.plugin.XMLResourceDefinition;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.views.design.ChooseWsdlServiceOperationWindow.ServiceChosenListener;
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.reficio.ws.builder.SoapBuilder;
import org.reficio.ws.builder.SoapOperation;
import org.reficio.ws.builder.core.Wsdl;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import jlibs.xml.sax.XMLDocument;
import jlibs.xml.xsd.XSInstance;
import jlibs.xml.xsd.XSParser;

@SuppressWarnings("serial")
public class ImportXmlTemplateWindow extends ResizableWindow implements ValueChangeListener, ClickListener, Receiver, SucceededListener {

    private static final String OPTION_TEXT = "Text";

    private static final String OPTION_FILE = "File";

    private static final String OPTION_URL = "URL";

    private static final String OPTION_RESOURCE = "Resource";
    
    private static final String URL_SETTING = "url";

    VerticalLayout optionLayout;

    OptionGroup optionGroup;

    AceEditor editor;

    Upload upload;

    TextField urlTextField;
    
    ComboBox resourceComboBox;

    ByteArrayOutputStream uploadedData;

    ImportXmlListener listener;
    
    Component component;
    
    ApplicationContext context;

    public ImportXmlTemplateWindow(ImportXmlListener listener, Component component, ApplicationContext context) {
        this.listener = listener;
        this.component = component;
        this.context = context;
        setCaption("Import XML Template");
        setWidth(600.0f, Unit.PIXELS);
        setHeight(500.0f, Unit.PIXELS);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setSpacing(true);
        layout.setMargin(true);
        layout.addStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
        layout.addComponent(new Label("Import XML from either an XSD or WSDL source."));

        optionGroup = new OptionGroup("Select the location of the XSD or WSDL.");
        optionGroup.addItem(OPTION_TEXT);
        optionGroup.addItem(OPTION_FILE);
        optionGroup.addItem(OPTION_URL);
        optionGroup.addItem(OPTION_RESOURCE);
        optionGroup.setNullSelectionAllowed(false);
        optionGroup.setImmediate(true);
        optionGroup.select(OPTION_TEXT);
        optionGroup.addValueChangeListener(this);
        layout.addComponent(optionGroup);

        optionLayout = new VerticalLayout();
        optionLayout.setSizeFull();

        editor = new AceEditor();
        editor.setCaption("Enter the XML text:");
        editor.setMode(AceMode.xml);
        editor.setSizeFull();
        editor.setHighlightActiveLine(true);
        editor.setShowPrintMargin(false);

        Button importButton = new Button("Import");
        importButton.addClickListener(this);

        upload = new Upload(null, this);
        upload.addSucceededListener(this);
        upload.setButtonCaption(null);
        urlTextField = new TextField("Enter the URL:");
        urlTextField.setWidth(100.0f, Unit.PERCENTAGE);
        
        resourceComboBox = createResourceCB();
        
        layout.addComponent(optionLayout);
        layout.setExpandRatio(optionLayout, 1.0f);
        rebuildOptionLayout();

        addComponent(layout, 1);
        addComponent(buildButtonFooter(importButton, buildCloseButton()));

    }
    
    protected ComboBox createResourceCB() {
        ComboBox cb = new ComboBox("HTTP Resource");
        
        String projectVersionId = component.getProjectVersionId();
        IConfigurationService configurationService = context.getConfigurationService();
    
        Set<XMLResourceDefinition> types = context.getDefinitionFactory()
                .getResourceDefinitions(projectVersionId, ResourceCategory.HTTP);
        String[] typeStrings = new String[types.size()];
        int i = 0;
        for (XMLResourceDefinition type : types) {
            typeStrings[i++] = type.getId();
        }
        List<Resource> resources = new ArrayList<>(configurationService.findResourcesByTypes(projectVersionId, true, typeStrings));
        if (resources != null) {
            for (Resource resource : resources) {
                cb.addItem(resource);
            }
        }

        cb.setWidth(50.0f, Unit.PERCENTAGE);
        return cb;
    }

    protected void rebuildOptionLayout() {
        optionLayout.removeAllComponents();
        if (optionGroup.getValue().equals(OPTION_TEXT)) {
            optionLayout.addComponent(editor);
            editor.focus();
        } else if (optionGroup.getValue().equals(OPTION_FILE)) {
            optionLayout.addComponent(upload);
            upload.focus();
        } else if (optionGroup.getValue().equals(OPTION_URL)) {
            optionLayout.addComponent(urlTextField);
            urlTextField.focus();
        } else if (optionGroup.getValue().equals(OPTION_RESOURCE)) {
            optionLayout.addComponent(resourceComboBox);
            resourceComboBox.focus();
        }
    }

    @Override
    public void valueChange(ValueChangeEvent event) {
        rebuildOptionLayout();
    }

    @Override
    public void uploadSucceeded(SucceededEvent event) {
        importXml(new String(uploadedData.toByteArray()));
    }

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
        return uploadedData = new ByteArrayOutputStream();
    }

    @Override
    public void buttonClick(ClickEvent event) {
        if (optionGroup.getValue().equals(OPTION_TEXT)) {
            importXml(editor.getValue());
        } else if (optionGroup.getValue().equals(OPTION_FILE)) {
            upload.submitUpload();
        } else if (optionGroup.getValue().equals(OPTION_URL)) {
            InputStream in = null;
            String text = null;
            try {
                in = new URL(urlTextField.getValue()).openStream();
                text = IOUtils.toString(in);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                IOUtils.closeQuietly(in);
            }
            importXml(text);
        } else if (optionGroup.getValue().equals(OPTION_RESOURCE)) {
            InputStream in = null;
            String text = null;
            try {
                Resource resource = (Resource) resourceComboBox.getValue();
                String resourceUrl = resource.findSetting(URL_SETTING).getValue();
                in = new URL(resourceUrl).openStream();
                text = IOUtils.toString(in);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                IOUtils.closeQuietly(in);
            }
            importXml(text);
        }
    }

    protected void importXml(String text) {
        if (isNotBlank(text)) {
            SAXBuilder builder = new SAXBuilder();
            builder.setXMLReaderFactory(XMLReaders.NONVALIDATING);
            builder.setFeature("http://xml.org/sax/features/validation", false);
            try {
                Document document = builder.build(new StringReader(text));
                String rootName = document.getRootElement().getName();
                if (rootName.equals("definitions")) {
                    importFromWsdl(text);
                } else if (rootName.equals("schema")) {
                    importFromXsd(text);
                } else {
                    Notification note = new Notification("Unrecognized Content", "The XML file has a root element of " + rootName
                            + ", but expected \"definitions\" for WSDL or \"schema\" for XSD.");
                    note.show(Page.getCurrent());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void importFromXsd(String text) throws Exception {
        XSModel xsModel = new XSParser().parseString(text, "");

        XSInstance xsInstance = new XSInstance();
        xsInstance.minimumElementsGenerated = 1;
        xsInstance.maximumElementsGenerated = 1;
        xsInstance.generateOptionalElements = Boolean.TRUE;

        XSNamedMap map = xsModel.getComponents(XSConstants.ELEMENT_DECLARATION);

        QName rootElement = new QName(map.item(0).getNamespace(), map.item(0).getName(), XMLConstants.DEFAULT_NS_PREFIX);
        StringWriter writer = new StringWriter();
        XMLDocument sampleXml = new XMLDocument(new StreamResult(writer), true, 4, null);
        xsInstance.generate(xsModel, rootElement, sampleXml);

        String xml = writer.toString();
        listener.onImport(xml);
    }

    protected void importFromWsdl(String text) throws Exception {
        File wsdlFile = File.createTempFile("import", "wsdl");
        FileUtils.write(wsdlFile, text);
        final Wsdl wsdl = Wsdl.parse(wsdlFile.toURI().toURL());
        List<SoapOperation> allOperations = new ArrayList<>();
        List<QName> bindings = wsdl.getBindings();
        for (QName binding : bindings) {
            SoapBuilder builder = wsdl.getBuilder(binding);
            List<SoapOperation> operations = builder.getOperations();
            allOperations.addAll(operations);
        }

        if (allOperations.size() == 0) {
            Notification note = new Notification("No operations", "No operations found in the WSDL.");
            note.show(Page.getCurrent());
        } else if (allOperations.size() == 1) {
            importFromWsdl(wsdl, allOperations.get(0));
        } else {
            ChooseWsdlServiceOperationWindow dialog = new ChooseWsdlServiceOperationWindow(allOperations, new ServiceChosenListener() {
                public boolean onOk(SoapOperation operation) {
                    importFromWsdl(wsdl, operation);
                    return true;
                }
            });
            UI.getCurrent().addWindow(dialog);
        }
    }

    protected void importFromWsdl(Wsdl wsdl, SoapOperation operation) {
        try {
            SoapBuilder builder = wsdl.getBuilder(operation.getBindingName());
            String xml = builder.buildInputMessage(operation);
            listener.onImport(xml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static interface ImportXmlListener extends Serializable {
        public void onImport(String xml);
    }

}
