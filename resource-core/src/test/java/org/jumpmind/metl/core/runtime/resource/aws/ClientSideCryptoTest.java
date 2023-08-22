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
package org.jumpmind.metl.core.runtime.resource.aws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

import com.amazonaws.encryptionsdk.AwsCrypto;

import software.amazon.awssdk.regions.Region;

/*
 * XXX: tests of the crypto operation methods is beyond difficult without a test
 * account; this would be a reasonable candidate for intrusive mocking
 */
public class ClientSideCryptoTest {
    /*
     * TODO: testCompile should be revamped to allow correct hamcrest & junit
     * resolution w/r/t PowerMock so that hamcrest matchers can be used reliably
     */
    // @Rule
    // public ExpectedException thrown = ExpectedException.none();

    /*
     * ------------------------------------------------------------------------
     * encryption-only restrict mode
     */

    @Test(expected = NullPointerException.class)
    public void forEncryptionOnlyRequiresNonNullCmkSpec() {
        ClientSideCrypto.forEncryptionOnly(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void forEncryptionOnlyRequiresNonEmptyCmkSpec() {
        ClientSideCrypto.forEncryptionOnly("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void forEncryptionOnlyRejectsPlainId() {
        /* plain Key ID requires default region */
        ClientSideCrypto.forEncryptionOnly("7e92cdbb-5281-43ce-a8ea-b30957777b3d");
    }

    @Test
    public void forEncryptionOnlyAcceptsIdArn() {
        ClientSideCrypto crypto = ClientSideCrypto.forEncryptionOnly(
                "arn:aws:kms:us-east-2:000000000000:key/7e92cdbb-5281-43ce-a8ea-b30957777b3d");

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.ENCRYPT_ONLY);
    }

    @Test
    public void forEncryptionOnlyAcceptsPlainIdWithDefaultRegion() {
        ClientSideCrypto crypto = ClientSideCrypto
                .forEncryptionOnly("7e92cdbb-5281-43ce-a8ea-b30957777b3d", Region.US_EAST_2.id());

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.ENCRYPT_ONLY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void forEncryptionOnlyRejectsPlainAlias() {
        /* plain Key Alias requires default region */
        ClientSideCrypto.forEncryptionOnly("alias/unit-test");
    }

    @Test
    public void forEncryptionOnlyAcceptsAliasArn() {
        ClientSideCrypto crypto = ClientSideCrypto
                .forEncryptionOnly("arn:aws:kms:us-east-2:000000000000:alias/unit-test");

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.ENCRYPT_ONLY);
    }

    @Test
    public void forEncryptionOnlyAcceptsPlainAliasWithDefaultRegion() {
        ClientSideCrypto crypto = ClientSideCrypto.forEncryptionOnly("alias/unit-test",
                Region.US_EAST_2.id());

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.ENCRYPT_ONLY);
    }

    /*
     * the generic forRestrictMode() factory just delegates to the named
     * factories, so there are only positive tests below
     */

    @Test
    public void settingsFactoryEncryptionOnlyAcceptsIdArn() {
        ClientSideCrypto crypto = ClientSideCrypto.forRestrictMode(
                ClientSideCrypto.RestrictMode.ENCRYPT_ONLY,
                "arn:aws:kms:us-east-2:000000000000:key/7e92cdbb-5281-43ce-a8ea-b30957777b3d",
                null);

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.ENCRYPT_ONLY);
    }

    @Test
    public void settingsFactoryEncryptionOnlyAcceptsPlainIdWithDefaultRegion() {
        ClientSideCrypto crypto = ClientSideCrypto.forRestrictMode(
                ClientSideCrypto.RestrictMode.ENCRYPT_ONLY, "7e92cdbb-5281-43ce-a8ea-b30957777b3d",
                Region.US_EAST_2.id());

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.ENCRYPT_ONLY);
    }

    @Test
    public void settingsFactoryEncryptionOnlyAcceptsAliasArn() {
        ClientSideCrypto crypto = ClientSideCrypto.forRestrictMode(
                ClientSideCrypto.RestrictMode.ENCRYPT_ONLY,
                "arn:aws:kms:us-east-2:000000000000:alias/unit-test", null);

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.ENCRYPT_ONLY);
    }

    @Test
    public void settingsFactoryEncryptionOnlyAcceptsPlainAliasWithDefaultRegion() {
        ClientSideCrypto crypto = ClientSideCrypto.forRestrictMode(
                ClientSideCrypto.RestrictMode.ENCRYPT_ONLY, "alias/unit-test",
                Region.US_EAST_2.id());

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.ENCRYPT_ONLY);
    }

    /*
     * ------------------------------------------------------------------------
     * decryption-only restrict mode
     */

    /* special case */
    @Test
    public void forDecryptionOnlyWithNullCmkSpecUsesDiscovery() {
        ClientSideCrypto crypto = ClientSideCrypto.forDecryptionOnly(null);

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.DECRYPT_ONLY_DISCOVERY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void forDecryptionOnlyRequiresNonEmptyCmkSpec() {
        ClientSideCrypto.forDecryptionOnly("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void forDecryptionOnlyRejectsPlainId() {
        /* plain Key ID requires default region */
        ClientSideCrypto.forDecryptionOnly("7e92cdbb-5281-43ce-a8ea-b30957777b3d");
    }

    @Test
    public void forDecryptionOnlyAcceptsIdArn() {
        ClientSideCrypto crypto = ClientSideCrypto.forDecryptionOnly(
                "arn:aws:kms:us-east-2:000000000000:key/7e92cdbb-5281-43ce-a8ea-b30957777b3d");

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.DECRYPT_ONLY);
    }

    @Test
    public void forDecryptionOnlyAcceptsPlainIdWithDefaultRegion() {
        ClientSideCrypto crypto = ClientSideCrypto
                .forDecryptionOnly("7e92cdbb-5281-43ce-a8ea-b30957777b3d", Region.US_EAST_2.id());

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.DECRYPT_ONLY);
    }

    /* Key Alias is never usable for decryption */
    @Test(expected = IllegalArgumentException.class)
    public void forEncryptionOnlyRejectsAliasArn() {
        ClientSideCrypto.forDecryptionOnly("arn:aws:kms:us-east-2:000000000000:alias/unit-test");
    }

    /* Key Alias is never usable for decryption */
    @Test(expected = IllegalArgumentException.class)
    public void forDecryptionOnlyRejectsPlainAliasWithDefaultRegion() {
        ClientSideCrypto.forDecryptionOnly("alias/unit-test", Region.US_EAST_2.id());
    }

    /*
     * the generic forRestrictMode() factory just delegates to the named
     * factories, so there are only positive tests below
     */

    @Test
    public void settingsFactoryDecryptionOnlyAcceptsIdArn() {
        ClientSideCrypto crypto = ClientSideCrypto.forRestrictMode(
                ClientSideCrypto.RestrictMode.DECRYPT_ONLY,
                "arn:aws:kms:us-east-2:000000000000:key/7e92cdbb-5281-43ce-a8ea-b30957777b3d",
                null);

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.DECRYPT_ONLY);
    }

    @Test
    public void settingsFactoryDecryptionOnlyAcceptsPlainIdWithDefaultRegion() {
        ClientSideCrypto crypto = ClientSideCrypto.forRestrictMode(
                ClientSideCrypto.RestrictMode.DECRYPT_ONLY, "7e92cdbb-5281-43ce-a8ea-b30957777b3d",
                Region.US_EAST_2.id());

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.DECRYPT_ONLY);
    }

    /*
     * ------------------------------------------------------------------------
     * "discovery" decryption-only restrict mode
     */

    @Test
    public void forDiscoveryDecryptionOnlyTakesNoArgs() {
        ClientSideCrypto crypto = ClientSideCrypto.forDiscoveryDecryptionOnly();

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.DECRYPT_ONLY_DISCOVERY);
    }

    /*
     * the generic forRestrictMode() factory just delegates to the named
     * factories, so there are only positive tests below
     */

    @Test
    public void settingsFactoryDiscoveryDecryptionOnlyAcceptsNullCmkSpec() {
        ClientSideCrypto crypto = ClientSideCrypto
                .forRestrictMode(ClientSideCrypto.RestrictMode.DECRYPT_ONLY_DISCOVERY, null, null);

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.DECRYPT_ONLY_DISCOVERY);
    }

    @Test
    public void settingsFactoryDiscoveryDecryptionOnlyIgnoresCmkSpecAndDefaultRegion() {
        ClientSideCrypto crypto = ClientSideCrypto.forRestrictMode(
                ClientSideCrypto.RestrictMode.DECRYPT_ONLY_DISCOVERY,
                "7e92cdbb-5281-43ce-a8ea-b30957777b3d", Region.US_EAST_2.id());

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.DECRYPT_ONLY_DISCOVERY);
    }

    /*
     * ------------------------------------------------------------------------
     * encryption + decryption restrict mode
     */

    @Test(expected = NullPointerException.class)
    public void forEncryptionDecryptionRequiresNonNullCmkSpec() {
        ClientSideCrypto.forEncryptionDecryption(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void forEncryptionDecryptionRequiresNonEmptyCmkSpec() {
        ClientSideCrypto.forEncryptionDecryption("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void forEncryptionDecryptionRejectsPlainId() {
        /* plain Key ID requires default region */
        ClientSideCrypto.forEncryptionDecryption("7e92cdbb-5281-43ce-a8ea-b30957777b3d");
    }

    @Test
    public void forEncryptionDecryptionAcceptsIdArn() {
        ClientSideCrypto crypto = ClientSideCrypto.forEncryptionDecryption(
                "arn:aws:kms:us-east-2:000000000000:key/7e92cdbb-5281-43ce-a8ea-b30957777b3d");

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.ENCRYPT_DECRYPT);
    }

    @Test
    public void forEncryptionDecryptionAcceptsPlainIdWithDefaultRegion() {
        ClientSideCrypto crypto = ClientSideCrypto.forEncryptionDecryption(
                "7e92cdbb-5281-43ce-a8ea-b30957777b3d", Region.US_EAST_2.id());

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.ENCRYPT_DECRYPT);
    }

    /* Key Alias is never usable for encryption+decryption */
    @Test(expected = IllegalArgumentException.class)
    public void forEncryptionDecryptionRejectsAliasArn() {
        ClientSideCrypto
                .forEncryptionDecryption("arn:aws:kms:us-east-2:000000000000:alias/unit-test");
    }

    /* Key Alias is never usable for encryption+decryption */
    @Test(expected = IllegalArgumentException.class)
    public void forEncryptionDecryptionRejectsPlainAliasWithDefaultRegion() {
        ClientSideCrypto.forEncryptionDecryption("alias/unit-test", Region.US_EAST_2.id());
    }

    /*
     * the generic forRestrictMode() factory just delegates to the named
     * factories, so there are only positive tests below
     */

    @Test
    public void settingsFactoryEncryptionDecryptionAcceptsIdArn() {
        ClientSideCrypto crypto = ClientSideCrypto.forRestrictMode(
                ClientSideCrypto.RestrictMode.ENCRYPT_DECRYPT,
                "arn:aws:kms:us-east-2:000000000000:key/7e92cdbb-5281-43ce-a8ea-b30957777b3d",
                null);

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.ENCRYPT_DECRYPT);
    }

    @Test
    public void settingsFactoryEncryptionDecryptionAcceptsPlainIdWithDefaultRegion() {
        ClientSideCrypto crypto = ClientSideCrypto.forRestrictMode(
                ClientSideCrypto.RestrictMode.ENCRYPT_DECRYPT,
                "7e92cdbb-5281-43ce-a8ea-b30957777b3d", Region.US_EAST_2.id());

        assertTrue(crypto.restrictMode().name(),
                crypto.restrictMode() == ClientSideCrypto.RestrictMode.ENCRYPT_DECRYPT);
    }

    /*
     * ------------------------------------------------------------------------
     * operations
     * 
     * NOTE: This test case CAN'T actually execute any operations that require
     * communication with AWS KMS, because the fixture data would need to
     * include _actual_ CMKs (and, of course, _actual_ credentials would need to
     * be provided, too); that's the domain of IT, not UT. As a result, the
     * remaining sections are limited primarily to negative tests.
     */

    /*
     * ------------------------------------------------------------------------
     * encryption and/or decryption common operations
     * 
     * NOTE: Junit4 support for true sub-tests is non-existent; "suggested"
     * approaches are grossly over-complicated and verbose IMO, so these common
     * operation tests use a simplified (albeit short-circuiting) approach -- UT
     * zealots may want to skip over this section ;)
     */

    @Test
    public void canAlwaysGetEncryptionAlgorithm() {
        ClientSideCrypto crypto;
        for (ClientSideCrypto.RestrictMode mode : ClientSideCrypto.RestrictMode.values()) {
            /* full Key Id ARN is usable (or safely ignorable) for ALL modes */
            crypto = ClientSideCrypto.forRestrictMode(mode,
                    "arn:aws:kms:us-east-2:000000000000:key/7e92cdbb-5281-43ce-a8ea-b30957777b3d",
                    null);

            /* null before an actual encryption/decryption operation */
            assertNull(mode.name(), crypto.getEncryptionAlgorithm());
        }
    }

    @Test
    public void canAlwaysGetEncryptionFrameSize() {
        ClientSideCrypto crypto;
        for (ClientSideCrypto.RestrictMode mode : ClientSideCrypto.RestrictMode.values()) {
            /* full Key Id ARN is usable (or safely ignorable) for ALL modes */
            crypto = ClientSideCrypto.forRestrictMode(mode,
                    "arn:aws:kms:us-east-2:000000000000:key/7e92cdbb-5281-43ce-a8ea-b30957777b3d",
                    null);

            /* default before an actual encryption/decryption operation */
            assertEquals(mode.name(), AwsCrypto.getDefaultFrameSize(),
                    crypto.getEncryptionFrameSize());
        }
    }

    /*
     * ------------------------------------------------------------------------
     * encryption-specific operations
     */

    @Test(expected = UnsupportedOperationException.class)
    public void discoveryDecryptionOnlyCannotEstimateCiphertextSize() {
        /* there's no CMK, and estimating requires knowing the key algorithm */
        ClientSideCrypto crypto = ClientSideCrypto.forDiscoveryDecryptionOnly();

        crypto.estimateCiphertextSize(0);
    }

    @Test /* negative "sub-tests" cannot use 'expected=' annotation argument */
    public void decryptionOnlyCannotEncryptData() {
        ClientSideCrypto.RestrictMode[] restrictedModes = new ClientSideCrypto.RestrictMode[] {
                ClientSideCrypto.RestrictMode.DECRYPT_ONLY,
                ClientSideCrypto.RestrictMode.DECRYPT_ONLY_DISCOVERY };

        ClientSideCrypto crypto;
        for (ClientSideCrypto.RestrictMode mode : restrictedModes) {
            crypto = ClientSideCrypto.forRestrictMode(mode,
                    "arn:aws:kms:us-east-2:000000000000:key/7e92cdbb-5281-43ce-a8ea-b30957777b3d",
                    null);

            try {
                crypto.encryptData(new byte[0]);
            } catch (UnsupportedOperationException expected) {
                /* pass */
            } catch (Exception unexpected) {
                throw new RuntimeException(mode.name(), unexpected);
            }
        }
    }

    @Test /* negative "sub-tests" cannot use 'expected=' annotation argument */
    public void decryptionOnlyCannotCreateEncryptingOutputStream() {
        ClientSideCrypto.RestrictMode[] restrictedModes = new ClientSideCrypto.RestrictMode[] {
                ClientSideCrypto.RestrictMode.DECRYPT_ONLY,
                ClientSideCrypto.RestrictMode.DECRYPT_ONLY_DISCOVERY };

        ClientSideCrypto crypto;
        for (ClientSideCrypto.RestrictMode mode : restrictedModes) {
            crypto = ClientSideCrypto.forRestrictMode(mode,
                    "arn:aws:kms:us-east-2:000000000000:key/7e92cdbb-5281-43ce-a8ea-b30957777b3d",
                    null);

            try {
                crypto.createEncryptingStream(new ByteArrayOutputStream());
            } catch (UnsupportedOperationException expected) {
                /* pass */
            } catch (Exception unexpected) {
                throw new RuntimeException(mode.name(), unexpected);
            }
        }
    }

    @Test /* negative "sub-tests" cannot use 'expected=' annotation argument */
    public void decryptionOnlyCannotCreateEncryptingInputStream() {
        ClientSideCrypto.RestrictMode[] restrictedModes = new ClientSideCrypto.RestrictMode[] {
                ClientSideCrypto.RestrictMode.DECRYPT_ONLY,
                ClientSideCrypto.RestrictMode.DECRYPT_ONLY_DISCOVERY };

        ClientSideCrypto crypto;
        for (ClientSideCrypto.RestrictMode mode : restrictedModes) {
            crypto = ClientSideCrypto.forRestrictMode(mode,
                    "arn:aws:kms:us-east-2:000000000000:key/7e92cdbb-5281-43ce-a8ea-b30957777b3d",
                    null);

            try {
                crypto.createEncryptingStream(new ByteArrayInputStream(new byte[0]));
            } catch (UnsupportedOperationException expected) {
                /* pass */
            } catch (Exception unexpected) {
                throw new RuntimeException(mode.name(), unexpected);
            }
        }
    }

    /*
     * ------------------------------------------------------------------------
     * decryption-specific operations
     */

    @Test(expected = UnsupportedOperationException.class)
    public void encryptionOnlyCannotDecryptData() {
        ClientSideCrypto crypto = ClientSideCrypto.forEncryptionOnly(
                "arn:aws:kms:us-east-2:000000000000:key/7e92cdbb-5281-43ce-a8ea-b30957777b3d");

        crypto.decryptData(new byte[0]);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void encryptionOnlyCannotCreateDecryptingOutputStream() {
        ClientSideCrypto crypto = ClientSideCrypto.forEncryptionOnly(
                "arn:aws:kms:us-east-2:000000000000:key/7e92cdbb-5281-43ce-a8ea-b30957777b3d");

        crypto.createDecryptingStream(new ByteArrayOutputStream());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void encryptionOnlyCannotCreateDecryptingInputStream() {
        ClientSideCrypto crypto = ClientSideCrypto.forEncryptionOnly(
                "arn:aws:kms:us-east-2:000000000000:key/7e92cdbb-5281-43ce-a8ea-b30957777b3d");

        crypto.createDecryptingStream(new ByteArrayInputStream(new byte[0]));
    }
}
