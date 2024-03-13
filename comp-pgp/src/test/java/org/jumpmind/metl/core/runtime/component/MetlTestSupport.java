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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition;
import org.jumpmind.metl.core.plugin.XMLDefinitions;

public abstract class MetlTestSupport {
    public static final String PLUGIN_XML = "plugin.xml";

    private static final JAXBContext PLUGIN_CONTEXT;

    static {
        try {
            PLUGIN_CONTEXT = JAXBContext.newInstance(
                    org.jumpmind.metl.core.plugin.ObjectFactory.class.getPackage().getName());
        } catch (JAXBException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    protected URL getResource(final String resourceFileName) {
        return Thread.currentThread().getContextClassLoader().getResource(resourceFileName);
    }

    protected Path getResourcePath(final String resourceFileName) {
        try {
            return Paths.get(getResource(resourceFileName).toURI());
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected byte[] readResourceData(final String resourceFileName) {
        return readResourceData(getResourcePath(resourceFileName));
    }

    protected byte[] readResourceData(final URL resourceUrl) {
        try {
            return readResourceData(Paths.get(resourceUrl.toURI()));
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected byte[] readResourceData(final Path resourcePath) {
        try {
            return Files.readAllBytes(resourcePath);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected String readResourceText(final String resourceFileName) {
        return readResourceText(getResourcePath(resourceFileName), UTF_8);
    }

    protected String readResourceText(final String resourceFileName,
            final String resourceCharsetName) {
        return readResourceText(getResourcePath(resourceFileName),
                Charset.forName(resourceCharsetName));
    }

    protected String readResourceText(final Path resourcePath, final String resourceCharsetName) {
        return readResourceText(resourcePath, Charset.forName(resourceCharsetName));
    }

    protected String readResourceText(final Path resourcePath, final Charset resourceCharset) {
        return new String(readResourceData(resourcePath), resourceCharset);
    }

    @SuppressWarnings("unchecked")
    protected XMLDefinitions unmarshalPluginXml() {
        URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(PLUGIN_XML);
        if (resourceUrl == null)
            return null;

        try (InputStream resourceStream = resourceUrl.openStream()) {
            JAXBElement<XMLDefinitions> element = (JAXBElement<XMLDefinitions>) PLUGIN_CONTEXT
                    .createUnmarshaller().unmarshal(resourceStream);
            return element.getValue();
        } catch (IOException | JAXBException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected XMLComponentDefinition findXMLComponentDefition(final String componentId) {
        XMLDefinitions definitions = unmarshalPluginXml();
        return definitions.getComponent().stream().filter(d -> d.getId().equals(componentId))
                .findFirst().orElse(null);
    }

    protected <T extends IComponentRuntime> T createComponentRuntime(final Class<T> type,
            final String componentId, final String... settingNameValuePairs) {
        return createComponentRuntime(type, findXMLComponentDefition(componentId),
                createSettingsArrayFrom(settingNameValuePairs));
    }

    protected <T extends IComponentRuntime> T createComponentRuntime(final Class<T> type,
            final String componentId, final Setting[] settings) {
        return createComponentRuntime(type, findXMLComponentDefition(componentId), settings);
    }

    protected <T extends IComponentRuntime> T createComponentRuntime(final Class<T> type,
            final XMLComponentDefinition componentDefinition,
            final String... settingNameValuePairs) {
        return createComponentRuntime(type, componentDefinition,
                createSettingsArrayFrom(settingNameValuePairs));
    }

    @SuppressWarnings("unchecked")
    protected <T extends IComponentRuntime> T createComponentRuntime(final Class<T> type,
            final XMLComponentDefinition componentDefinition, final Setting[] settings) {
        T componentRuntime;
        try {
            componentRuntime = type.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
            throw new RuntimeException(ex);
        }
        return (T) createComponentRuntime(componentRuntime, componentDefinition, settings);
    }

    protected IComponentRuntime createComponentRuntime(final IComponentRuntime componentRuntime,
            final XMLComponentDefinition componentDefinition,
            final String... settingNameValuePairs) {
        return createComponentRuntime(componentRuntime, componentDefinition,
                createSettingsArrayFrom(settingNameValuePairs));
    }

    protected IComponentRuntime createComponentRuntime(final IComponentRuntime componentRuntime,
            final XMLComponentDefinition componentDefinition, final Setting[] settings) {
        Component component = new Component(null, null, null, null, null, null, settings);
        FlowStep flowStep = new FlowStep(component);
        ComponentContext componentContext = new ComponentContext(null, flowStep, null, null, null,
                null, null, null);

        componentRuntime.create(componentDefinition, componentContext, -1);
        return componentRuntime;
    }

    protected Setting[] createSettingsArrayFrom(final String... nvPairs) {
        if (nvPairs.length % 2 != 0)
            throw new IllegalArgumentException(
                    "settingsArrayFrom vararg length must be even (i.e. name,value,... pairs)");

        return IntStream.range(0, nvPairs.length).filter(i -> i % 2 == 0)
                .mapToObj(i -> new Setting(nvPairs[i], nvPairs[i + 1])).toArray(Setting[]::new);
    }
}
