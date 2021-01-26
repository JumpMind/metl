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
import static org.jumpmind.metl.core.runtime.component.PgpConfiguration.PRIVATE_KEY_LOCATION;
import static org.jumpmind.metl.core.runtime.component.PgpConfiguration.PRIVATE_KEY_PASSPHRASE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Iterator;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPRuntimeOperationException;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.util.io.Streams;
import org.jumpmind.metl.core.runtime.BinaryMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

public class PgpDecrypt extends AbstractComponentRuntime {
    private URL privateKeyUrl;

    private char[] privateKeyPassPhrase;

    @Override
    public void start() {
        super.start();

        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
            Security.addProvider(new BouncyCastleProvider());

        TypedProperties properties = getTypedProperties();

        String privateKeyLocation = requireNonNull(properties.get(PRIVATE_KEY_LOCATION),
                PRIVATE_KEY_LOCATION);
        try {
            privateKeyUrl = new File(privateKeyLocation).toURI().toURL();
        } catch (MalformedURLException ex) {
            /* unreachable (in theory) */
            log.error("{} is not valid: {}", PRIVATE_KEY_LOCATION, ex.toString());
            throw new UncheckedIOException(ex);
        }

        /* TODO: this needs to be handled in a better way */
        privateKeyPassPhrase = requireNonNull(properties.get(PRIVATE_KEY_PASSPHRASE),
                PRIVATE_KEY_PASSPHRASE).toCharArray();
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
                outputPayload = decrypt(inputPayload);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            } catch (PGPException | NoSuchProviderException ex) {
                throw new PGPRuntimeOperationException("decryption failed", ex);
            }
            callback.sendBinaryMessage(inputMessage.getHeader(), outputPayload);
        }
    }

    @SuppressWarnings("rawtypes")
    private byte[] decrypt(byte[] sourceData)
            throws IOException, PGPException, NoSuchProviderException {
        InputStream dataDecoderStream = PGPUtil
                .getDecoderStream(new ByteArrayInputStream(sourceData));
        PGPEncryptedDataList encryptedDataList = findEncryptedDataList(dataDecoderStream);
        if (encryptedDataList == null)
            throw new IllegalArgumentException("input did not contain encrypted data");

        /*
         * obtain the private key and the encrypted data; the encrypted data
         * itself encapsulates the ID of the key that is needed such that if a
         * matching key is not found, then _this_ message cannot be decrypted
         * using _this_ private
         */
        PGPPrivateKey privateKey = null;
        PGPPublicKeyEncryptedData encryptedData = null;
        Iterator encryptedDataIter = encryptedDataList.iterator();
        try (InputStream privateKeyStream = privateKeyUrl.openStream();
                InputStream privateKeyDecoderStream = PGPUtil.getDecoderStream(privateKeyStream);) {
            PGPSecretKeyRingCollection secretKeyRings = new PGPSecretKeyRingCollection(
                    privateKeyDecoderStream, new JcaKeyFingerprintCalculator());
            while (privateKey == null && encryptedDataIter.hasNext()) {
                encryptedData = (PGPPublicKeyEncryptedData) encryptedDataIter.next();
                privateKey = extractPrivateKeyById(secretKeyRings, encryptedData.getKeyID());
            }
        }
        if (privateKey == null)
            throw new PGPException("message can not be decrypted using configured private key");

        PublicKeyDataDecryptorFactory pkDecryptorFactory = new JcePublicKeyDataDecryptorFactoryBuilder()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(privateKey);

        InputStream encryptedDataStream = encryptedData.getDataStream(pkDecryptorFactory);
        Object message = new JcaPGPObjectFactory(encryptedDataStream).nextObject();
        if (message instanceof PGPCompressedData) {
            InputStream compressedDataStream = ((PGPCompressedData) message).getDataStream();
            message = new JcaPGPObjectFactory(compressedDataStream).nextObject();
        }

        ByteArrayOutputStream sink = null;
        if (message instanceof PGPLiteralData) {
            sink = new ByteArrayOutputStream();
            try (InputStream literalDataStream = ((PGPLiteralData) message).getInputStream()) {
                Streams.pipeAll(literalDataStream, sink);
            }
        } else {
            throw new PGPException(String.format(
                    "expected to find PGPLiteralData in the encrypted data stream, but found {} instead",
                    message.getClass().getSimpleName()));
        }

        /* the HMAC can only be verified once all the data has been decrypted */
        if (encryptedData.isIntegrityProtected() && encryptedData.verify()) {
            return sink.toByteArray();
        } else {
            sink = null;
            throw new PGPException("message failed integrity check");
        }
    }

    @SuppressWarnings("rawtypes")
    private PGPEncryptedDataList findEncryptedDataList(final InputStream inStream) {
        Object obj = null;
        Iterator objIter = new JcaPGPObjectFactory(inStream).iterator();
        while (objIter.hasNext()) {
            obj = objIter.next();
            if (obj instanceof PGPEncryptedDataList)
                return (PGPEncryptedDataList) obj;
        }

        /* not found */
        return null;
    }

    private PGPPrivateKey extractPrivateKeyById(final PGPSecretKeyRingCollection skRingCollection,
            final long keyId) throws PGPException, NoSuchProviderException {
        PGPSecretKey sKey = skRingCollection.getSecretKey(keyId);
        if (sKey == null)
            return null;

        return sKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(privateKeyPassPhrase));
    }

    @Override
    public void stop() {
        privateKeyUrl = null;
        privateKeyPassPhrase = null;

        super.stop();
    }
}
