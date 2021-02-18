package org.jumpmind.metl.core.runtime.component;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition;
import org.jumpmind.metl.core.plugin.XMLDefinitions;
import org.jumpmind.metl.core.runtime.BinaryMessage;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.aws.S3;
import org.jumpmind.metl.core.runtime.resource.aws.S3ITestSupport;
import org.jumpmind.properties.TypedProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class S3FileUtilITest extends S3ITestSupport {
    private static final Map<String, String> EMPTY_MAP = Collections.emptyMap();

    private static final S3 S3_RESOURCE_RUNTIME = new S3();

    private static final Map<String, IResourceRuntime> DEPLOYED_RESOURCES = Collections
            .singletonMap("s3", S3_RESOURCE_RUNTIME);

    private static final XMLComponentDefinition XML_COMPONENT_DEFINITION;

    static {
        URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource("plugin.xml");
        try (InputStream resourceStream = resourceUrl.openStream()) {
            JAXBContext jaxbContext = JAXBContext.newInstance(
                    org.jumpmind.metl.core.plugin.ObjectFactory.class.getPackage().getName());
            @SuppressWarnings("unchecked")
            JAXBElement<XMLDefinitions> element = (JAXBElement<XMLDefinitions>) jaxbContext
                    .createUnmarshaller().unmarshal(resourceStream);
            XMLDefinitions definitions = element.getValue();
            XML_COMPONENT_DEFINITION = definitions.getComponent().stream()
                    .filter(d -> d.getId().equals("S3 File Util")).findFirst().orElse(null);
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @BeforeClass
    public static void prepareRuntimeRequirements() {
        S3_RESOURCE_RUNTIME.start(null, s3ResourceRuntimeSettings());
    }

    @AfterClass
    public static void disposeRuntimeRequirements() {
        S3_RESOURCE_RUNTIME.stop();
    }

    private static TypedProperties s3ResourceRuntimeSettings() {
        TypedProperties properties = new TypedProperties();
        properties.setProperty(S3.Settings.BUCKET_NAME, TEST_BUCKET_NAME);
        return properties;
    }

    private static S3FileUtil s3FileUtil(final Setting... settings) {
        Resource resource = new Resource("s3");
        Component component = new Component(resource, null, null, null, null, null, settings);
        FlowStep flowStep = new FlowStep(component);
        ComponentContext componentContext = new ComponentContext(null, flowStep, null, null,
                DEPLOYED_RESOURCES, EMPTY_MAP, EMPTY_MAP, EMPTY_MAP);

        S3FileUtil util = new S3FileUtil();
        util.setComponentDefinition(XML_COMPONENT_DEFINITION);
        util.setContext(componentContext);

        return util;
    }

    private static Setting[] settingsArray(final String... nvPairs) {
        if (nvPairs.length % 2 != 0)
            throw new IllegalArgumentException(
                    "settingsArray vararg length must be even (i.e. name,value,... pairs)");

        return IntStream.range(0, nvPairs.length).filter(i -> i % 2 == 0)
                .mapToObj(i -> new Setting(nvPairs[i], nvPairs[i + 1])).toArray(Setting[]::new);
    }

    private static File makeCopyOf(final File sourceFile) throws IOException {
        Path tempFile = Files
                .createTempFile(S3FileUtilITest.class.getSimpleName().toLowerCase() + "-", null);
        Files.copy(sourceFile.toPath(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        return tempFile.toFile();
    }

    @Test
    public void runWhenPerUowIgnoresNonControlMessage() {
        assumeTrue("AWS credentials are not present", awsCredentialsArePresent());

        IComponentRuntime runtime = s3FileUtil();

        runtime.start();
        runtime.handle(new TextMessage(null), /* NPE if handled -> */null, false);
        runtime.stop();
    }

    @Test
    public void notRunWhenPerUowIgnoresControlMessage() {
        assumeTrue("AWS credentials are not present", awsCredentialsArePresent());

        Setting[] settings = settingsArray(AbstractComponentRuntime.RUN_WHEN,
                AbstractComponentRuntime.PER_MESSAGE);
        IComponentRuntime runtime = s3FileUtil(settings);

        runtime.start();
        runtime.handle(new ControlMessage(), /* NPE if handled -> */null, false);
        runtime.stop();
    }

    @Test
    public void canUploadFromBinaryMessageWithKeyFromSetting() {
        assumeTrue("AWS credentials are not present", awsCredentialsArePresent());

        // AWS max key length (64):
        // ---------------****************************************************************
        String testKey = "upload-binary-message-key-setting/test";
        List<String> expectedNamesProcessed = Collections
                .singletonList(String.format("%s/%s", TEST_BUCKET_NAME, testKey));
        Setting[] settings = settingsArray(AbstractComponentRuntime.RUN_WHEN,
                AbstractComponentRuntime.PER_MESSAGE, S3FileUtil.SETTING_OBJECT_KEY, testKey);
        IComponentRuntime runtime = s3FileUtil(settings);

        byte[] payload = "The quick brown fox jumps over the lazy dog.".getBytes(UTF_8);
        BinaryMessage message = new BinaryMessage("test", payload);

        ISendMessageCallback callback = new NoOpSendMessageCallback();

        runtime.start();
        runtime.handle(message, callback, false);
        runtime.stop();

        NoOpSendMessageCallback callbackImpl = (NoOpSendMessageCallback) callback;
        assertEquals("sendTextMessage", callbackImpl.invokedMethodName);
        assertEquals(expectedNamesProcessed, callbackImpl.payload);
    }

    @Test
    public void canUploadFromControlMessageWithFileNameAndKeyFromSettings() throws IOException {
        assumeTrue("AWS credentials are not present", awsCredentialsArePresent());

        // AWS max key length (64):
        // ---------------****************************************************************
        String testKey = "upload-control-message-filename-key-settings/test";
        File sourceFile = makeCopyOf(testFile());
        Setting[] settings = settingsArray(S3FileUtil.SETTING_FILE_NAME,
                sourceFile.getAbsolutePath(), S3FileUtil.SETTING_OBJECT_KEY, testKey);
        IComponentRuntime runtime = s3FileUtil(settings);

        ISendMessageCallback callback = new NoOpSendMessageCallback();

        runtime.start();
        runtime.handle(new ControlMessage(), callback, false);
        runtime.stop();

        List<String> expectedNamesProcessed = Collections
                .singletonList(String.format("%s/%s", TEST_BUCKET_NAME, testKey));
        NoOpSendMessageCallback callbackImpl = (NoOpSendMessageCallback) callback;
        assertEquals("sendTextMessage", callbackImpl.invokedMethodName);
        assertEquals(expectedNamesProcessed, callbackImpl.payload);

        /* source file should be deleted on success */
        assertFalse(sourceFile + " was not deleted after upload", sourceFile.exists());
    }

    @Test
    public void canUploadFromTextMessageMultipleFilesUsingImpliedKeys() throws IOException {
        assumeTrue("AWS credentials are not present", awsCredentialsArePresent());

        @SuppressWarnings("unused")
        // AWS max key length (64):
        // ---------------****************************************************************
        String testKey = "upload-txtmsg-nfiles/s3fileutilitest-9223372036854775807.tmp";

        File sourceFile1 = makeCopyOf(testFile());
        File sourceFile2 = makeCopyOf(testFile());
        String expectedNameProcessed1 = String.format("%s/%s", TEST_BUCKET_NAME,
                sourceFile1.getName());
        String expectedNameProcessed2 = String.format("%s/%s", TEST_BUCKET_NAME,
                sourceFile2.getName());
        Setting[] settings = settingsArray(AbstractComponentRuntime.RUN_WHEN,
                AbstractComponentRuntime.PER_MESSAGE, S3FileUtil.SETTING_GET_FILE_NAME_FROM_MESSAGE,
                "true");
        IComponentRuntime runtime = s3FileUtil(settings);

        ArrayList<String> payload = new ArrayList<>();
        payload.add(sourceFile1.getAbsolutePath());
        payload.add(sourceFile2.getAbsolutePath());
        TextMessage message = new TextMessage("test", payload);

        ISendMessageCallback callback = new NoOpSendMessageCallback();

        runtime.start();
        runtime.handle(message, callback, false);
        runtime.stop();

        NoOpSendMessageCallback callbackImpl = (NoOpSendMessageCallback) callback;
        assertEquals("sendTextMessage", callbackImpl.invokedMethodName);
        /* can't do a List#equals because the uploads are concurrent */
        @SuppressWarnings("unchecked")
        List<String> actualPayload = (List<String>) callbackImpl.payload;
        assertEquals(2, actualPayload.size());
        assertTrue(expectedNameProcessed1 + " was not in the callback payload",
                actualPayload.contains(expectedNameProcessed1));
        assertTrue(expectedNameProcessed2 + " was not in the callback payload",
                actualPayload.contains(expectedNameProcessed2));

        /* source files should be deleted on success */
        assertFalse(sourceFile1 + " was not deleted after upload", sourceFile1.exists());
        assertFalse(sourceFile2 + " was not deleted after upload", sourceFile2.exists());
    }

    @Test
    public void canUploadFromTextMessageMultipleFilesUsingKeyFormat() throws IOException {
        assumeTrue("AWS credentials are not present", awsCredentialsArePresent());

        // AWS max key length (64):
        // ---------------****************************************************************
        String testKey = "upload-text-message-multifile-key-format/test-$(_sequence)";
        // see AbstractComponentRuntime#resolveParamsAndHeaders(String,Message)
        String expectedNameProcessed1 = String.format("%s/%s", TEST_BUCKET_NAME,
                testKey.replace("$(_sequence)", "0"));
        String expectedNameProcessed2 = String.format("%s/%s", TEST_BUCKET_NAME,
                testKey.replace("$(_sequence)", "1"));
        File sourceFile1 = makeCopyOf(testFile());
        File sourceFile2 = makeCopyOf(testFile());
        Setting[] settings = settingsArray(AbstractComponentRuntime.RUN_WHEN,
                AbstractComponentRuntime.PER_MESSAGE, S3FileUtil.SETTING_GET_FILE_NAME_FROM_MESSAGE,
                "true", S3FileUtil.SETTING_OBJECT_KEY, testKey);
        IComponentRuntime runtime = s3FileUtil(settings);

        ArrayList<String> payload = new ArrayList<>();
        payload.add(sourceFile1.getAbsolutePath());
        payload.add(sourceFile2.getAbsolutePath());
        TextMessage message = new TextMessage("test", payload);

        ISendMessageCallback callback = new NoOpSendMessageCallback();

        runtime.start();
        runtime.handle(message, callback, false);
        runtime.stop();

        NoOpSendMessageCallback callbackImpl = (NoOpSendMessageCallback) callback;
        assertEquals("sendTextMessage", callbackImpl.invokedMethodName);
        /* can't do a List#equals because the uploads are concurrent */
        @SuppressWarnings("unchecked")
        List<String> actualPayload = (List<String>) callbackImpl.payload;
        assertEquals(2, actualPayload.size());
        assertTrue(expectedNameProcessed1 + " was not in the callback payload",
                actualPayload.contains(expectedNameProcessed1));
        assertTrue(expectedNameProcessed2 + " was not in the callback payload",
                actualPayload.contains(expectedNameProcessed2));

        /* source files should be deleted on success */
        assertFalse(sourceFile1 + " was not deleted after upload", sourceFile1.exists());
        assertFalse(sourceFile2 + " was not deleted after upload", sourceFile2.exists());
    }
}
