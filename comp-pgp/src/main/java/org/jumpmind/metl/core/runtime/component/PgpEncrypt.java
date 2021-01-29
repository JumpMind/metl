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

import static java.util.Objects.requireNonNull;
import static org.jumpmind.metl.core.runtime.component.PgpConfiguration.ARMORED;
import static org.jumpmind.metl.core.runtime.component.PgpConfiguration.COMPRESSION_ALGORITHM;
import static org.jumpmind.metl.core.runtime.component.PgpConfiguration.COMPRESSION_LEVEL;
import static org.jumpmind.metl.core.runtime.component.PgpConfiguration.PUBLIC_KEY_LOCATION;
import static org.jumpmind.metl.core.runtime.component.PgpConfiguration.SYMMETRIC_KEY_ALGORITHM;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.security.Security;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPRuntimeOperationException;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.BinaryMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.PgpConfiguration.CompressionAlgorithm;
import org.jumpmind.metl.core.runtime.component.PgpConfiguration.CompressionLevel;
import org.jumpmind.metl.core.runtime.component.PgpConfiguration.SymmetricKeyAlgorithm;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

public class PgpEncrypt extends AbstractComponentRuntime {
    private String publicKeyLocation;

    private SymmetricKeyAlgorithm symmetricKeyAlgorithm;

    private CompressionAlgorithm compressionAlgorithm;

    private CompressionLevel compressionLevel;

    private boolean armored;

    private PGPPublicKey publicKey;

    @Override
    public void start() {
        super.start();

        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
            Security.addProvider(new BouncyCastleProvider());

        TypedProperties properties = getTypedProperties();

        publicKeyLocation = requireNonNull(properties.get(PUBLIC_KEY_LOCATION),
                PUBLIC_KEY_LOCATION);
        symmetricKeyAlgorithm = SymmetricKeyAlgorithm
                .valueOf(properties.get(SYMMETRIC_KEY_ALGORITHM));
        compressionAlgorithm = CompressionAlgorithm.valueOf(properties.get(COMPRESSION_ALGORITHM));
        compressionLevel = CompressionLevel.valueOf(properties.get(COMPRESSION_LEVEL));
        armored = properties.is(ARMORED, false);
        try {
            publicKey = readPublicKey();
        } catch (IOException | PGPException ex) {
            log.error("Unable to read public key from {}: {}", PUBLIC_KEY_LOCATION, ex.toString());
            throw new IoException(ex);
        }

        log.info("selected public key {}", publicKey.getKeyID());
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(final Message inputMessage, final ISendMessageCallback callback,
            final boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof BinaryMessage) {
            byte[] inputPayload = ((BinaryMessage) inputMessage).getPayload();
            byte[] outputPayload;
            try {
                byte[] compressedPayload = compress(inputPayload);
                outputPayload = encrypt(compressedPayload);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            } catch (PGPException ex) {
                throw new PGPRuntimeOperationException("encryption failed", ex);
            }
            callback.sendBinaryMessage(inputMessage.getHeader(), outputPayload);
        } else {
            log.debug("ignoring {}", inputMessage.getClass());
        }
    }

    @SuppressWarnings("rawtypes")
    protected PGPPublicKey readPublicKey() throws IOException, PGPException {
        URL publicKeyUrl = new File(publicKeyLocation).toURI().toURL();

        try (InputStream keyIn = publicKeyUrl.openStream()) {
            PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(
                    PGPUtil.getDecoderStream(keyIn), new JcaKeyFingerprintCalculator());

            Iterator keyRingIter = pgpPub.getKeyRings();
            while (keyRingIter.hasNext()) {
                PGPPublicKeyRing keyRing = (PGPPublicKeyRing) keyRingIter.next();

                Iterator keyIter = keyRing.getPublicKeys();
                while (keyIter.hasNext()) {
                    PGPPublicKey key = (PGPPublicKey) keyIter.next();

                    /* a "valid" key is one that... */
                    if (key.isEncryptionKey()/* is used for encryption */
                            && !key.isMasterKey()/* is not used for signing */
                            && !isExpired(key)/* has not expired yet */
                            && !isRevoked(key)/* has not been revoked */) {
                        return key;
                    }
                }
            }
        }

        throw new IllegalArgumentException("Unable to find valid key in the key file.");
    }

    protected boolean isExpired(final PGPPublicKey pubKey) {
        long validSeconds = pubKey.getValidSeconds();
        if (validSeconds != 0/* never expires */) {
            Date creationTime = pubKey.getCreationTime();
            Instant expires = creationTime.toInstant().plusSeconds(validSeconds);
            if (!Instant.now().isAfter(expires)) {
                log.warn("public key {} expired {}", pubKey.getKeyID(), expires);
                /* XXX: a strict implementation should return true here */
            }
        }
        return false;
    }

    protected boolean isRevoked(final PGPPublicKey pubKey) {
        if (pubKey.hasRevocation()) {
            log.warn("public key {} has been revoked", pubKey.getKeyID());
            /*
             * XXX: a strict implementation should verify the revocation
             * certificate here and return true or false
             */
        }
        return false;
    }

    private byte[] compress(final byte[] sourceData) throws IOException {
        PGPCompressedDataGenerator compressor = new PGPCompressedDataGenerator(
                compressionAlgorithm.intValue(), compressionLevel.intValue());
        PGPLiteralDataGenerator encoder = new PGPLiteralDataGenerator();

        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try (OutputStream compressionFilter = compressor.open(sink);
                OutputStream literalFilter = encoder.open(compressionFilter, PGPLiteralData.BINARY,
                        PGPLiteralData.CONSOLE, sourceData.length, new Date());) {
            literalFilter.write(sourceData);
        }
        return sink.toByteArray();
    }

    private byte[] encrypt(final byte[] sourceData) throws IOException, PGPException {
        PGPEncryptedDataGenerator encryptor = new PGPEncryptedDataGenerator(
                new JcePGPDataEncryptorBuilder(symmetricKeyAlgorithm.intValue())
                        .setWithIntegrityPacket(true)
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME));
        encryptor.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(publicKey)
                .setProvider(BouncyCastleProvider.PROVIDER_NAME));

        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try (OutputStream armorFilter = armored ? new ArmoredOutputStream(sink) : sink;
                OutputStream encryptionFilter = encryptor.open(armorFilter, sourceData.length);) {
            encryptionFilter.write(sourceData);
        }
        return sink.toByteArray();
    }

    @Override
    public void stop() {
        publicKeyLocation = null;
        publicKey = null;

        super.stop();
    }
}
