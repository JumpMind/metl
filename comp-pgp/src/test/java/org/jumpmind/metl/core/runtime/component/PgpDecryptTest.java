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
import static org.jumpmind.metl.core.runtime.component.PgpConfiguration.PRIVATE_KEY_LOCATION;
import static org.jumpmind.metl.core.runtime.component.PgpConfiguration.PRIVATE_KEY_PASSPHRASE_LOCATION;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.BinaryMessage;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.util.NameValue;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PgpDecryptTest extends MetlTestSupport {
    /*
     * used for both src/test/resources/privkey.asc.txt and
     * src/test/resources/sig-privkey.asc.txt
     * 
     * if a new key pair is generated for testing, then -- in addition to
     * replacing the pub/priv files -- the src/test/resources/ciphertext.bin,
     * src/test/resources/ciphertext.asc.txt,
     * src/test/resources/ciphertext_uncompressed.bin and
     * src/test/resources/ciphertext_no-compression.bin files MUST be
     * regenerated (see PgpEncryptTest#TESTING_PLAINTEXT)
     */
    private static final String TESTING_PASSPHRASE_LOCATION = "privkey-pass-phrase.txt";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    PgpDecrypt createPgpDecryptWithTestKeyAndSettings(String... settingNameValuePairs) {
        Setting[] settings = createSettingsArrayFrom(settingNameValuePairs);
        Setting[] settingsWithTestKey = Arrays.copyOf(settings, settings.length + 2);
        settingsWithTestKey[settings.length] = new Setting(PRIVATE_KEY_LOCATION,
                getResourcePath("privkey.asc.txt").toString());
        settingsWithTestKey[settings.length + 1] = new Setting(PRIVATE_KEY_PASSPHRASE_LOCATION,
                getResourcePath(TESTING_PASSPHRASE_LOCATION).toString());
        return createComponentRuntime(PgpDecrypt.class, "PGP Decrypt", settingsWithTestKey);
    }

    @After
    public void removeBouncyCastleProvider() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    }

    @Test
    public void start_failsIfKeyLocationNotConfigured() {
        PgpDecrypt runtime = createComponentRuntime(PgpDecrypt.class, "PGP Decrypt",
                PRIVATE_KEY_PASSPHRASE_LOCATION, TESTING_PASSPHRASE_LOCATION);

        thrown.expect(NullPointerException.class);
        thrown.expectMessage(PRIVATE_KEY_LOCATION);

        runtime.start();
    }

    @Test
    public void start_failsIfKeyPassPhraseNotConfigured() {
        PgpDecrypt runtime = createComponentRuntime(PgpDecrypt.class, "PGP Decrypt",
                PRIVATE_KEY_LOCATION, getResourcePath("privkey.asc.txt").toString());

        thrown.expect(NullPointerException.class);
        thrown.expectMessage(PRIVATE_KEY_PASSPHRASE_LOCATION);

        runtime.start();
    }

    @Test
    public void start_addsBouncyCastleProvider() {
        PgpDecrypt runtime = createPgpDecryptWithTestKeyAndSettings();

        assertNull(
                BouncyCastleProvider.PROVIDER_NAME
                        + " provider should NOT be registered (precondition)",
                Security.getProvider(BouncyCastleProvider.PROVIDER_NAME));

        runtime.start();

        assertNotNull(BouncyCastleProvider.PROVIDER_NAME + " provider SHOULD be registered",
                Security.getProvider(BouncyCastleProvider.PROVIDER_NAME));
    }

    @Test
    public void handle_noCallbackOnControlMessage() {
        PgpDecrypt runtime = createPgpDecryptWithTestKeyAndSettings();
        Message message = new ControlMessage();

        runtime.start();
        runtime.handle(message, null/* NPE if dereferenced */, false);
    }

    @Test
    public void handle_noCallbackOnTextMessage() {
        PgpDecrypt runtime = createPgpDecryptWithTestKeyAndSettings();
        ArrayList<String> payload = new ArrayList<>();
        payload.add("test");
        Message message = new TextMessage("test", payload);

        runtime.start();
        runtime.handle(message, null/* NPE if dereferenced */, false);
    }

    @Test
    public void handle_noCallbackOnEntityDataMessage() {
        PgpDecrypt runtime = createPgpDecryptWithTestKeyAndSettings();
        NameValue nv = new NameValue("unit", "test");
        EntityData data = new EntityData(nv);
        ArrayList<EntityData> payload = new ArrayList<>();
        payload.add(data);
        Message message = new EntityDataMessage("test", payload);

        runtime.start();
        runtime.handle(message, null/* NPE if dereferenced */, false);
    }

    /* contrast with PgpEncryptTest#handle_binaryMessageWithDefaultSettings() */
    @Test
    public void handle_binaryMessageFromDefaultEncryptionSettings() {
        PgpDecrypt runtime = createPgpDecryptWithTestKeyAndSettings();
        byte[] inputPayload = readResourceData("ciphertext.bin");
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
        assertEquals(PgpEncryptTest.TESTING_PLAINTEXT, new String(outputPayload, UTF_8));
        assertArrayEquals(new String[0], callbackImpl.targetStepIds);
    }

    /*
     * contrast with PgpEncryptTest#handle_binaryMessageWithNonDefaultSettings()
     */
    @Test
    public void handle_binaryMessageFromNonDefaultEncryptionSettings() {
        PgpDecrypt runtime = createPgpDecryptWithTestKeyAndSettings();
        byte[] inputPayload = readResourceData("ciphertext.asc.txt");
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
        assertEquals(PgpEncryptTest.TESTING_PLAINTEXT, new String(outputPayload, UTF_8));
        assertArrayEquals(new String[0], callbackImpl.targetStepIds);
    }

    /*
     * contrast with
     * PgpEncryptTest#handle_encryptionWithCompressionAlgorithmUncompressed()
     */
    @Test
    public void handle_binaryMessageUncompressed() {
        PgpDecrypt runtime = createPgpDecryptWithTestKeyAndSettings();
        byte[] inputPayload = readResourceData("ciphertext_uncompressed.bin");
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
        assertEquals(PgpEncryptTest.TESTING_PLAINTEXT, new String(outputPayload, UTF_8));
        assertArrayEquals(new String[0], callbackImpl.targetStepIds);
    }

    /*
     * contrast with
     * PgpEncryptTest#handle_encryptionWithCompressionLevelNoCompression()
     */
    @Test
    public void handle_binaryMessageNoCompression() {
        PgpDecrypt runtime = createPgpDecryptWithTestKeyAndSettings();
        byte[] inputPayload = readResourceData("ciphertext_no-compression.bin");
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
        assertEquals(PgpEncryptTest.TESTING_PLAINTEXT, new String(outputPayload, UTF_8));
        assertArrayEquals(new String[0], callbackImpl.targetStepIds);
    }

    /*
     * contrast with
     * PgpEncryptTest#handle_encryptionWithUncompressedNoCompression()
     */
    @Test
    public void handle_binaryMessageUncompressedNoCompression() {
        PgpDecrypt runtime = createPgpDecryptWithTestKeyAndSettings();
        byte[] inputPayload = readResourceData("ciphertext_uncompressed_no-compression.bin");
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
        assertEquals(PgpEncryptTest.TESTING_PLAINTEXT, new String(outputPayload, UTF_8));
        assertArrayEquals(new String[0], callbackImpl.targetStepIds);
    }
}
