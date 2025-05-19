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
package org.jumpmind.metl.core.runtime.component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.Http;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.metl.core.runtime.resource.IOutputStreamWithResponse;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.util.FormatUtils;

public class WebXmlPagedReader extends AbstractComponentRuntime {

    public static final String TYPE = "Paged Web Reader";

    public static final String DEFAULT_CHARSET = "UTF-8";

    public static final String RELATIVE_PATH = "relative.path";
    
    public static final String BODY_FROM = "body.from";
    
    public static final String BODY_TEXT = "body.text";
    
    public static final String REQUEST_XPATH = "request.xpath";
    
    public static final String RESULT_XPATH = "result.xpath";
    
    public static final String PARAMETER_REPLACEMENT = "parameter.replacement";
    
    String runWhen = PER_UNIT_OF_WORK;

    String relativePath;
    
    String bodyFrom;
    
    String bodyText;
    
    String requestXpath;
    
    String resultXpath;
    
    IDirectory streamable;
    
    boolean parameterReplacement;
    
    @Override
    public void start() {
        IResourceRuntime httpResource = getResourceRuntime();
        if (httpResource == null) {
        	throw new IllegalStateException(String.format(
                    "A msgTarget resource of type %s must be chosen.  No resource was chosen.",
                    Http.TYPE));
        }
        streamable = (IDirectory) getResourceReference();
        /*
        if (!(httpResource instanceof Http)) {
            throw new IllegalStateException(String.format(
                    "A msgTarget resource of type %s must be chosen.  The specified resource is:" + httpResource.getResourceRuntimeSettings(),
                    Http.TYPE));
        }
		*/
        Component component = getComponent();
        relativePath = component.get(RELATIVE_PATH);
        bodyFrom = component.get(BODY_FROM, "Message");
        bodyText = component.get(BODY_TEXT);
        requestXpath = component.get(REQUEST_XPATH,"//previousPageLastRecordPredicate");
        resultXpath = component.get(RESULT_XPATH,"//lastRecordPredicate");
        parameterReplacement = component.getBoolean(PARAMETER_REPLACEMENT, false);
        runWhen = getComponent().get(RUN_WHEN, PER_UNIT_OF_WORK);
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
                || (!PER_UNIT_OF_WORK.equals(runWhen) && !(inputMessage instanceof ControlMessage))) {

            ArrayList<String> inputPayload = new ArrayList<String>();
            if (bodyFrom.equals("Message") && inputMessage instanceof TextMessage) {
                inputPayload = ((TextMessage)inputMessage).getPayload();
            } else {
                inputPayload.add(bodyText);
            }

            if (inputPayload != null) {
                try {
                    for (String requestContent : inputPayload) {

                        ArrayList<String> outputPayload = new ArrayList<String>();
                        String pageValue = null;
                        do {
                            getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                            if (parameterReplacement) {
                                requestContent = FormatUtils.replaceTokens(requestContent,
                                        context.getFlowParameters(), true);
                            }
                            if (pageValue != null && StringUtils.isNotBlank(requestXpath)) {
                                requestContent = documentFindReplace(requestContent, requestXpath, pageValue);
                            }
                            info("relativePath: " + relativePath);
                            OutputStream rawStream = streamable.getOutputStream(relativePath, false);
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(rawStream, DEFAULT_CHARSET));
                            try {
                                writer.write(requestContent);
                            } finally {
                                writer.close();
                                if (rawStream instanceof IOutputStreamWithResponse is) {
	                                String response = is.getResponse();
	                                if (response != null) {
	                                    outputPayload.add(response);
	                                    if (StringUtils.isNotBlank(resultXpath)) {
	                                        pageValue = getDocumentValue(response, resultXpath);
	                                    }
	                                }
                                }
                            }
                            if (outputPayload.size() > 0) {
                                callback.sendTextMessage(null, outputPayload);
                                outputPayload.clear();
                            }
                        } while(pageValue != null);
                    }

                } catch (IOException e) {
                    throw new IoException(String.format("Error writing to %s ", streamable), e);
                }
            }
        }
    }

    private String documentFindReplace(String content, String searchXpath, String replaceValue) {
        SAXBuilder builder = new SAXBuilder();
        builder.setXMLReaderFactory(XMLReaders.NONVALIDATING);
        builder.setFeature("http://xml.org/sax/features/validation", false);
        Document document = null;
        try {
            document = builder.build(new StringReader(content));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        XPathFactory xFactory = XPathFactory.instance();
        XPathExpression<Element> expr = xFactory.compile(searchXpath, Filters.element());
        
        List<Element> matches = expr.evaluate(document.getRootElement());
        if (matches.size() == 0) {
            log(LogLevel.WARN, "XPath expression '" + searchXpath + "' did not find any matches");
        } else {
            Element element = matches.get(0);
            if (replaceValue != null) {
                element.setText(replaceValue.toString());
            }
        }
        XMLOutputter xmlOutputter = new XMLOutputter();
        content = xmlOutputter.outputString(document);
        return content;
    }
    
    private String getDocumentValue(String content, String xpath) {
        String value = null;
        SAXBuilder builder = new SAXBuilder();
        builder.setXMLReaderFactory(XMLReaders.NONVALIDATING);
        builder.setFeature("http://xml.org/sax/features/validation", false);
        Document document = null;
        try {
            document = builder.build(new StringReader(content));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        XPathFactory xFactory = XPathFactory.instance();
        XPathExpression<Element> expr = xFactory.compile(xpath, Filters.element());
        
        List<Element> matches = expr.evaluate(document.getRootElement());
        if (matches.size() == 0) {
            log(LogLevel.WARN, "XPath expression '" + xpath + "' did not find any matches");
        } else {
            Element element = matches.get(0);
            value = element.getText();
        }
        return value;
    }

}
