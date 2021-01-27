package org.jumpmind.metl.core.runtime.component;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.jumpmind.metl.core.runtime.component.PgpConfiguration.ARMORED;
import static org.jumpmind.metl.core.runtime.component.PgpConfiguration.COMPRESSION_ALGORITHM;
import static org.jumpmind.metl.core.runtime.component.PgpConfiguration.COMPRESSION_LEVEL;
import static org.jumpmind.metl.core.runtime.component.PgpConfiguration.PUBLIC_KEY_LOCATION;
import static org.jumpmind.metl.core.runtime.component.PgpConfiguration.SYMMETRIC_KEY_ALGORITHM;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.BinaryMessage;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.component.PgpConfiguration.CompressionAlgorithm;
import org.jumpmind.metl.core.runtime.component.PgpConfiguration.CompressionLevel;
import org.jumpmind.metl.core.runtime.component.PgpConfiguration.SymmetricKeyAlgorithm;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.util.NameValue;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PgpEncryptTest extends MetlTestSupport {
    /*
     * MUST regenerate src/test/resources/ciphertext.bin and
     * src/test/resources/ciphertext.asc.txt if either this value or
     * src/test/resources/privkey.asc.txt is modified!
     */
    static final String TESTING_PLAINTEXT = "The quick brown fox jumps over the lazy dog.";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    PgpEncrypt createPgpEncryptWithTestKeyAndSettings(String... settingNameValuePairs) {
        Setting[] settings = createSettingsArrayFrom(settingNameValuePairs);
        Setting[] settingsWithTestKey = Arrays.copyOf(settings, settings.length + 1);
        settingsWithTestKey[settings.length] = new Setting(PUBLIC_KEY_LOCATION,
                getResourcePath("pubkey.asc.txt").toString());
        return createComponentRuntime(PgpEncrypt.class, "PGP Encrypt", settingsWithTestKey);
    }

    @After
    public void removeBouncyCastleProvider() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    }

    @Test
    public void start_failsIfKeyLocationNotConfigured() {
        PgpEncrypt runtime = createComponentRuntime(PgpEncrypt.class, "PGP Encrypt");

        thrown.expect(NullPointerException.class);
        thrown.expectMessage(PUBLIC_KEY_LOCATION);

        runtime.start();
    }

    @Test
    public void start_rejectsInvalidKeyAlgorithm() {
        PgpEncrypt runtime = createPgpEncryptWithTestKeyAndSettings(SYMMETRIC_KEY_ALGORITHM,
                "NOT_A_SYMMETRIC_KEY_ALGORITHM");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("NOT_A_SYMMETRIC_KEY_ALGORITHM");

        runtime.start();
    }

    @Test
    public void start_rejectsInvalidCompressionAlgorithm() {
        PgpEncrypt runtime = createPgpEncryptWithTestKeyAndSettings(COMPRESSION_ALGORITHM,
                "NOT_A_COMPRESSION_ALGORITHM");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("NOT_A_COMPRESSION_ALGORITHM");

        runtime.start();
    }

    @Test
    public void start_rejectsInvalidCompressionLevel() {
        PgpEncrypt runtime = createPgpEncryptWithTestKeyAndSettings(COMPRESSION_LEVEL,
                "NOT_A_COMPRESSION_LEVEL");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("NOT_A_COMPRESSION_LEVEL");

        runtime.start();
    }

    @Test
    public void start_failsWhenPublicKeyLocationNotFound() {
        PgpEncrypt runtime = createComponentRuntime(PgpEncrypt.class, "PGP Encrypt",
                PUBLIC_KEY_LOCATION, "not/a/public/key.location");

        thrown.expect(IoException.class);
        thrown.expectCause(instanceOf(FileNotFoundException.class));

        runtime.start();
    }

    @Test
    public void start_failsOnUnexpectedPGPObject() {
        PgpEncrypt runtime = createComponentRuntime(PgpEncrypt.class, "PGP Encrypt",
                PUBLIC_KEY_LOCATION,
                getResourcePath("privkey.asc.txt"/* no public key here */).toString());

        thrown.expect(IoException.class);
        thrown.expectCause(instanceOf(PGPException.class));

        runtime.start();
    }

    @Test
    public void start_failsOnUnknownPGPObject() {
        PgpEncrypt runtime = createComponentRuntime(PgpEncrypt.class, "PGP Encrypt",
                PUBLIC_KEY_LOCATION, getResourcePath(
                        "not-a-valid-pubkey.asc.txt"/* found but corrupted */).toString());

        thrown.expect(IoException.class);
        thrown.expectCause(instanceOf(IOException.class));

        runtime.start();
    }

    @Test
    public void start_failsWhenEncryptionOnlyKeyNotFound() {
        PgpEncrypt runtime = createComponentRuntime(PgpEncrypt.class, "PGP Encrypt",
                PUBLIC_KEY_LOCATION,
                getResourcePath("sig-pubkey.asc.txt"/* not for encryption */).toString());

        thrown.expect(IllegalArgumentException.class);

        runtime.start();
    }

    @Test
    public void start_failsWhenKeyLocationIsNotKeyRing() {
        PgpEncrypt runtime = createComponentRuntime(PgpEncrypt.class, "PGP Encrypt",
                PUBLIC_KEY_LOCATION, getResourcePath("this-is-not-a-key.txt").toString());

        thrown.expect(IllegalArgumentException.class);

        runtime.start();
    }

    @Test
    public void start_addsBouncyCastleProvider() {
        PgpEncrypt runtime = createPgpEncryptWithTestKeyAndSettings();

        assertNull(
                BouncyCastleProvider.PROVIDER_NAME
                        + " provider should NOT be registered (precondition)",
                Security.getProvider(BouncyCastleProvider.PROVIDER_NAME));

        runtime.start();

        assertNotNull(BouncyCastleProvider.PROVIDER_NAME + " provider SHOULD be registered",
                Security.getProvider(BouncyCastleProvider.PROVIDER_NAME));
    }

    @Test
    public void handle_refusesToNotEncrypt() {
        PgpEncrypt runtime = createPgpEncryptWithTestKeyAndSettings(SYMMETRIC_KEY_ALGORITHM,
                SymmetricKeyAlgorithm.NULL.toString());
        byte[] inputPayload = TESTING_PLAINTEXT.getBytes(UTF_8);
        BinaryMessage message = new BinaryMessage("test", inputPayload);

        /* actually comes from BC JcePGPDataEncryptorBuilder */
        thrown.expect(IllegalArgumentException.class);

        runtime.start();
        runtime.handle(message, null/* NPE if dereferenced */, false);
    }

    @Test
    public void handle_noCallbackOnControlMessage() {
        PgpEncrypt runtime = createPgpEncryptWithTestKeyAndSettings();
        Message message = new ControlMessage();

        runtime.start();
        runtime.handle(message, null/* NPE if dereferenced */, false);
    }

    @Test
    public void handle_noCallbackOnTextMessage() {
        PgpEncrypt runtime = createPgpEncryptWithTestKeyAndSettings();
        ArrayList<String> payload = new ArrayList<>();
        payload.add("test");
        Message message = new TextMessage("test", payload);

        runtime.start();
        runtime.handle(message, null/* NPE if dereferenced */, false);
    }

    @Test
    public void handle_noCallbackOnEntityDataMessage() {
        PgpEncrypt runtime = createPgpEncryptWithTestKeyAndSettings();
        NameValue nv = new NameValue("unit", "test");
        EntityData data = new EntityData(nv);
        ArrayList<EntityData> payload = new ArrayList<>();
        payload.add(data);
        Message message = new EntityDataMessage("test", payload);

        runtime.start();
        runtime.handle(message, null/* NPE if dereferenced */, false);
    }

    @Test
    public void handle_binaryMessageWithDefaultSettings() {
        PgpEncrypt runtime = createPgpEncryptWithTestKeyAndSettings();
        byte[] inputPayload = TESTING_PLAINTEXT.getBytes(UTF_8);
        BinaryMessage message = new BinaryMessage("test", inputPayload);
        message.getHeader().put("UnitTest", "test");
        ISendMessageCallback callback = new NoOpSendMessageCallback();

        runtime.start();
        runtime.handle(message, callback, false);

        NoOpSendMessageCallback callbackImpl = (NoOpSendMessageCallback) callback;
        assertEquals("sendBinaryMessage", callbackImpl.invokedMethodName);
        assertEquals(message.getHeader(), callbackImpl.messageHeaders);
        byte[] outputPayload = (byte[]) callbackImpl.payload;
        assertFalse("callback (output) payload should NOT equal input payload",
                Arrays.equals(inputPayload, outputPayload));
        assertFalse("callback (output) payload should NOT be ASCII-armored",
                new String(outputPayload, US_ASCII).startsWith("-----BEGIN PGP MESSAGE-----"));
        assertArrayEquals(new String[0], callbackImpl.targetStepIds);
    }

    @Test
    public void handle_binaryMessageWithNonDefaultSettings() {
        PgpEncrypt runtime = createPgpEncryptWithTestKeyAndSettings(SYMMETRIC_KEY_ALGORITHM,
                SymmetricKeyAlgorithm.TWOFISH.name(), COMPRESSION_ALGORITHM,
                CompressionAlgorithm.BZIP2.name(), COMPRESSION_LEVEL,
                CompressionLevel.BEST_SPEED.name(), ARMORED, "true");
        byte[] inputPayload = TESTING_PLAINTEXT.getBytes(UTF_8);
        BinaryMessage message = new BinaryMessage("test", inputPayload);
        message.getHeader().put("UnitTest", "test");
        ISendMessageCallback callback = new NoOpSendMessageCallback();

        runtime.start();
        runtime.handle(message, callback, false);

        NoOpSendMessageCallback callbackImpl = (NoOpSendMessageCallback) callback;
        assertEquals("sendBinaryMessage", callbackImpl.invokedMethodName);
        assertEquals(message.getHeader(), callbackImpl.messageHeaders);
        byte[] outputPayload = (byte[]) callbackImpl.payload;
        assertFalse("callback (output) payload should NOT equal input payload",
                Arrays.equals(inputPayload, outputPayload));
        assertTrue("callback (output) payload SHOULD be ASCII-armored",
                new String(outputPayload, US_ASCII).startsWith("-----BEGIN PGP MESSAGE-----"));
        assertArrayEquals(new String[0], callbackImpl.targetStepIds);
    }
}
