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

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoAlgorithm;
import com.amazonaws.encryptionsdk.CryptoInputStream;
import com.amazonaws.encryptionsdk.CryptoOutputStream;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.ParsedCiphertext;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;

/**
 * A <em>CSE-KMS</em> (client-side encryption using AWS Key Management Service
 * customer master keys) helper utilty.
 * 
 * <p>
 * <code>ClientSideCrypto</code> instances are immutable and closed with respect
 * to the underlying {@link AwsCrypto} and {@link KmsMasterKeyProvider}
 * instances. All boilerplate AWS Encryption SDK authentication and key
 * management tasks are encapsulated, resulting in a simplified and less
 * error-prone interface.
 * </p>
 * <p>
 * <code>ClientSideCrypto</code> avoids several areas of trouble by performing
 * more rigorous examination of CMK identifiers with respect to intended crypto
 * operations. Refer to the {@code for*()} factory methods.
 * </p>
 * 
 * @see <a href=
 *      "https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/introduction.html">What
 *      is the AWS Encryption SDK?</a>
 * @see <a href=
 *      "https://docs.aws.amazon.com/general/latest/gr/aws_sdk_cryptography.html">AWS
 *      SDK support for Amazon S3 client-side encryption</a>
 * @see <a href=
 *      "https://docs.amazonaws.cn/en_us/kms/latest/developerguide/rotate-keys.html">Rotating
 *      customer master keys</a>
 * @see <a href=
 *      "https://www.oreilly.com/library/view/aws-certified-solutions/9781789130669/45ae5662-a39f-4d33-a797-480d2ae42e0e.xhtml">Client-side
 *      encryption with KMS managed keys (CSE-KMS)</a>
 */
public final class ClientSideCrypto {
    public static final class Settings {
        public static final String ENABLED = "aws.crypto.cse.enabled";

        public static final String KMS_CMK_SPEC = "aws.crypto.cse.kms.cmk.spec";

        public static final String DEFAULT_REGION = "aws.crypto.cse.default.region";

        public static final String RESTRICT_MODE = "aws.crypto.cse.restrict.mode";

        Settings() {
            throw new UnsupportedOperationException("do not instantiate");
        }
    }

    /**
     * Explicitly restrict the usable crypto operations based on the CMK type
     * and default region.
     * 
     * <p>
     * For some AWS Encryption SDK API calls, we can know deterministically that
     * they will fail given a certain CMK ARN type (ID vs. Alias); the restrict
     * mode allows us to avoid misconfiguration up front and to fail as early as
     * possible otherwise.
     * </p>
     * 
     * @see <a href=
     *      "https://github.com/aws/aws-encryption-sdk-java/issues/27">Support
     *      decryption using KmsMasterKeys built using alias ARNs #27</a>
     */
    public static enum RestrictMode {
        /**
         * Allow ALL encryption- and decryption-related operation using a
         * specified CMK.
         */
        ENCRYPT_DECRYPT,
        /** Allow only encryption-related operations using a specified CMK. */
        ENCRYPT_ONLY,
        /** Allow only decryption-related operations using a specified CMK. */
        DECRYPT_ONLY,
        /** Allow only decryption-related operations with ANY accessible CMK. */
        DECRYPT_ONLY_DISCOVERY,
    }

    /**
     * Convenience factory to drive mode-restricted creation patterns from
     * configuration settings.
     * 
     * <p>
     * Refer to the corresponding {@code for*(String, String)} factories.
     * </p>
     * 
     * @param restrictMode
     *            the mode restriction
     * @param kmsCmkSpec
     *            specifies the AWS KMS customer master
     * @return a fully constructed and initialized CSE-KMS helper
     */
    public static ClientSideCrypto forRestrictMode(final RestrictMode restrictMode,
            final String kmsCmkSpec, final String defaultRegion) {
        switch (restrictMode) {
            case ENCRYPT_DECRYPT:
                return forEncryptionDecryption(kmsCmkSpec, defaultRegion);
            case ENCRYPT_ONLY:
                return forEncryptionOnly(kmsCmkSpec, defaultRegion);
            case DECRYPT_ONLY:
                return forDecryptionOnly(kmsCmkSpec, defaultRegion);
            case DECRYPT_ONLY_DISCOVERY:
                if (kmsCmkSpec != null || defaultRegion != null) {
                    LOG.warn("ignoring non-null kmsCmkSpec and/or defaultRegion "
                            + "for restrict mode DECRYPT_ONLY_DISCOVERY");
                }
                return forDiscoveryDecryptionOnly();
            default:
                /* unreachable */
                throw new UnsupportedOperationException(restrictMode.name());
        }
    }

    /**
     * Creates a CSE-KMS helper for encryption and/or decryption using the
     * specified CMK (customer master key).
     * 
     * <p>
     * For encryption+decryption mode, the <em>kmsCmkArn</em> MUST be a full Key
     * ID ARN ("arn:aws:kms:${region}:${accountId}:key/${uuid}").
     * </p>
     * 
     * @param kmsCmkArn
     *            specifies the AWS KMS customer master key as a full Key ID ARN
     * @return a fully constructed and initialized CSE-KMS helper
     */
    public static ClientSideCrypto forEncryptionDecryption(final String kmsCmkArn) {
        return forEncryptionDecryption(kmsCmkArn, null);
    }

    /**
     * Creates a CSE-KMS helper for encryption and/or decryption using the
     * specified CMK (customer master key) and default region.
     * 
     * <p>
     * If <em>defaultRegion</em> is <code>null</code>, then <em>kmsCmkSpec</em>
     * MUST be a full Key ID ARN
     * ("arn:aws:kms:${region}:${accountId}:key/${uuid}").
     * </p>
     * <p>
     * If <em>defaultRegion</em> is non-null, then <em>kmsCmkSpec</em> MAY be
     * expressed as a plain Key ID ("${uuid}").
     * </p>
     * 
     * @param kmsCmkSpec
     *            specifies the AWS KMS customer master key as either a full Key
     *            ID ARN or a plain Key ID (UUID)
     * @param defaultRegion
     *            the region used to resolve a plain Key ID <em>kmsCmkSpec</em>
     * @return a fully constructed and initialized CSE-KMS helper
     */
    public static ClientSideCrypto forEncryptionDecryption(final String kmsCmkSpec,
            final String defaultRegion) {
        if (!isEncryptionDecryptionCmkSpecUsable(requireNonNull(kmsCmkSpec, "KMS CMK spec"),
                defaultRegion)) {
            throw new IllegalArgumentException(
                    "KMS CMK spec is not usable for encryption+decryption");
        }

        return new ClientSideCrypto(kmsCmkSpec, DefaultAWSCredentialsProviderChain.getInstance(),
                defaultRegion, RestrictMode.ENCRYPT_DECRYPT);
    }

    /**
     * Creates a CSE-KMS helper for encryption <strong>only</strong> using the
     * specified CMK (customer master key).
     * 
     * <p>
     * For encryption-only mode, the <em>kmsCmkArn</em> may represent either a
     * full Key ID ARN ("arn:aws:kms:${region}:${accountId}:key/${uuid}") or a
     * full Key Alias ARN ( "arn:aws:kms:${region}:${accountId}:alias/${name}").
     * </p>
     * 
     * @param kmsCmkArn
     *            specifies the AWS KMS customer master key as a full ARN
     * @return a fully constructed and initialized CSE-KMS helper
     */
    public static ClientSideCrypto forEncryptionOnly(final String kmsCmkArn) {
        return forEncryptionOnly(kmsCmkArn, null);
    }

    /**
     * Creates a CSE-KMS helper for encryption <strong>only</strong> using the
     * specified CMK (customer master key) and default region.
     * 
     * <p>
     * If <em>defaultRegion</em> is <code>null</code>, then <em>kmsCmkSpec</em>
     * MUST be either a full Key ID ARN
     * ("arn:aws:kms:${region}:${accountId}:key/${uuid}") or a full Key Alias
     * ARN ("arn:aws:kms:${region}:${accountId}:alias/${name}").
     * </p>
     * <p>
     * If <em>defaultRegion</em> is non-null, then <em>kmsCmkSpec</em> MAY be
     * expressed as either a plain Key ID ("${uuid}") or a plain Key Alias
     * ("${name}").
     * </p>
     * 
     * @param kmsCmkSpec
     *            specifies the AWS KMS customer master key as either a full ARN
     *            or a plain Key ID/Alias
     * @param defaultRegion
     *            the region used to resolve a plain Key ID/Alias
     * @return a fully constructed and initialized CSE-KMS helper
     */
    public static ClientSideCrypto forEncryptionOnly(final String kmsCmkSpec,
            final String defaultRegion) {
        if (!isEncryptionOnlyCmkSpecUsable(requireNonNull(kmsCmkSpec, "KMS CMK spec"),
                defaultRegion)) {
            throw new IllegalArgumentException("KMS CMK spec is not usable for encryption-only");
        }

        return new ClientSideCrypto(requireNonNull(kmsCmkSpec, "KMS CMK spec"),
                DefaultAWSCredentialsProviderChain.getInstance(), defaultRegion,
                RestrictMode.ENCRYPT_ONLY);
    }

    /**
     * Creates a CSE-KMS helper for decryption <strong>only</strong> in AWS
     * "discovery" mode.
     * 
     * <p>
     * In discovery mode, any message that was encrypted using a CMK accessible
     * to the authenticated IAM account can be decrypted (i.e. this is a
     * "multi-region capable" decryption mode).
     * </p>
     * 
     * @return a fully constructed and initialized CSE-KMS helper
     */
    public static ClientSideCrypto forDiscoveryDecryptionOnly() {
        return new ClientSideCrypto(null, DefaultAWSCredentialsProviderChain.getInstance(), null,
                RestrictMode.DECRYPT_ONLY_DISCOVERY);
    }

    /**
     * Creates a CSE-KMS helper for decryption <strong>only</strong> using the
     * specified CMK (customer master key).
     * 
     * <p>
     * For decryption-only mode, the <em>kmsCmkArn</em> MUST be either a full
     * Key ID ARN ("arn:aws:kms:${region}:${accountId}:key/${uuid}") or
     * <code>null</code>.
     * </p>
     * 
     * @param kmsCmkArn
     *            specifies the AWS KMS customer master key as a full Key ID ARN
     *            (may be <code>null</code> to use AWS "discovery" mode)
     * @return a fully constructed and initialized CSE-KMS helper
     */
    public static ClientSideCrypto forDecryptionOnly(final String kmsCmkArn) {
        return forDecryptionOnly(kmsCmkArn, null);
    }

    /**
     * Creates a CSE-KMS helper for decryption <strong>only</strong> using the
     * specified CMK (customer master key) and default region.
     * 
     * <p>
     * If <em>defaultRegion</em> is <code>null</code>, then <em>kmsCmkSpec</em>
     * MUST be a full Key ID ARN
     * ("arn:aws:kms:${region}:${accountId}:key/${uuid}").
     * </p>
     * <p>
     * If <em>defaultRegion</em> is non-null, then <em>kmsCmkSpec</em> MAY be
     * expressed as a plain Key ID ("${uuid}").
     * </p>
     * 
     * @param kmsCmkSpec
     *            specifies the AWS KMS customer master key as either a full Key
     *            ID ARN or a plain Key ID
     * @param defaultRegion
     *            the region used to resolve a plain Key ID
     * @return a fully constructed and initialized CSE-KMS helper
     */
    public static ClientSideCrypto forDecryptionOnly(final String kmsCmkSpec,
            final String defaultRegion) {
        if (kmsCmkSpec == null) {
            LOG.warn("kmsCmkSpec is null; ignoring defaultRegion and "
                    + "returning \"discovery\"-mode decryption-only client instead");
            return forDiscoveryDecryptionOnly();
        } else if (!isDecryptionOnlyCmkSpecUsable(kmsCmkSpec, defaultRegion)) {
            throw new IllegalArgumentException("KMS CMK spec is not usable for decryption-only");
        }

        return new ClientSideCrypto(kmsCmkSpec, DefaultAWSCredentialsProviderChain.getInstance(),
                defaultRegion, RestrictMode.DECRYPT_ONLY);
    }

    /* can use ID/Alias ARN or plain ID/Alias */
    private static boolean isEncryptionOnlyCmkSpecUsable(final String kmsCmkSpec,
            final String defaultRegion) {
        boolean isFullArn = kmsCmkSpec.startsWith(CMK_SPEC_ARN_PREFIX)
                && (kmsCmkSpec.contains(CMK_SPEC_ARN_ID_MARKER)
                        || kmsCmkSpec.contains(CMK_SPEC_ARN_ALIAS_MARKER));
        return isFullArn
                || /* plain Key ID or Key Alias with a default region */(defaultRegion != null
                        && (kmsCmkSpec.startsWith(CMK_SPEC_ALIAS_PREFIX)
                                || CMK_SPEC_ID_PATTERN.matcher(kmsCmkSpec).matches()));
    }

    /* can't use Alias */
    private static boolean isDecryptionOnlyCmkSpecUsable(final String kmsCmkSpec,
            final String defaultRegion) {
        return /* full Key ID ARN */(kmsCmkSpec.startsWith(CMK_SPEC_ARN_PREFIX)
                && kmsCmkSpec.contains(CMK_SPEC_ARN_ID_MARKER))
                || /* plain Key ID with a default region */(defaultRegion != null
                        && CMK_SPEC_ID_PATTERN.matcher(kmsCmkSpec).matches());
    }

    /* same as decryption-only */
    private static boolean isEncryptionDecryptionCmkSpecUsable(final String kmsCmkSpec,
            final String defaultRegion) {
        return isDecryptionOnlyCmkSpecUsable(kmsCmkSpec, defaultRegion);
    }

    /*
     * when using a plain Key Alias with a default region, the CMK spec must be
     * of the form "alias/${name}"
     */
    private static final String CMK_SPEC_ALIAS_PREFIX = "alias/";

    private static final String CMK_SPEC_ARN_PREFIX = "arn:aws:kms:";

    private static final String CMK_SPEC_ARN_ID_MARKER = ":key/";

    private static final String CMK_SPEC_ARN_ALIAS_MARKER = ":" + CMK_SPEC_ALIAS_PREFIX;

    /*
     * match only a subset of the canonical UUID character set (AWS disallows
     * upper case)
     */
    private static final Pattern CMK_SPEC_ID_PATTERN = Pattern
            .compile("[0-9a-f]{8}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{12}");

    private static final Map<String, String> EMPTY_MAP = Collections.emptyMap();

    /* crypto operations are delegated to this backing instance */
    private static final AwsCrypto AWS_CRYPTO = AwsCrypto
            .standard();/* implies key commitment */

    private static final Logger LOG = LoggerFactory.getLogger(ClientSideCrypto.class.getName());

    private final KmsMasterKeyProvider masterKeyProvider;

    private final RestrictMode restrictMode;

    /* use one of the factory methods to obtain an instance */
    private ClientSideCrypto(final String kmsCmkSpec,
            final AWSCredentialsProvider credentialsProvider, final String defaultRegion,
            final RestrictMode restrictMode) {
        KmsMasterKeyProvider.Builder mkpBuilder = KmsMasterKeyProvider.builder()
                .withDefaultRegion(defaultRegion).withCredentials(credentialsProvider);
        masterKeyProvider = (kmsCmkSpec != null) ? mkpBuilder.buildStrict(kmsCmkSpec)
                : mkpBuilder.buildDiscovery();

        this.restrictMode = restrictMode;
    }

    public RestrictMode restrictMode() {
        return restrictMode;
    }

    public CryptoAlgorithm getEncryptionAlgorithm() {
        return AWS_CRYPTO.getEncryptionAlgorithm();
    }

    public int getEncryptionFrameSize() {
        return AWS_CRYPTO.getEncryptionFrameSize();
    }

    public long estimateCiphertextSize(final int plaintextSize) {
        return estimateCiphertextSize(plaintextSize, EMPTY_MAP);
    }

    public long estimateCiphertextSize(final int plaintextSize,
            final Map<String, String> encryptionContext) {
        switch (restrictMode) {
            case DECRYPT_ONLY_DISCOVERY:/* no CMK */
                throw new UnsupportedOperationException(restrictMode.name());
            default:
                return AWS_CRYPTO.estimateCiphertextSize(masterKeyProvider, plaintextSize,
                        encryptionContext);
        }
    }

    public CryptoResult<byte[], KmsMasterKey> encryptData(final byte[] plaintext) {
        return encryptData(plaintext, EMPTY_MAP);
    }

    public CryptoResult<byte[], KmsMasterKey> encryptData(final byte[] plaintext,
            final Map<String, String> encryptionContext) {
        switch (restrictMode) {
            case DECRYPT_ONLY:
            case DECRYPT_ONLY_DISCOVERY:
                throw new UnsupportedOperationException(restrictMode.name());
            default:
                return AWS_CRYPTO.encryptData(masterKeyProvider, plaintext, encryptionContext);
        }
    }

    public CryptoResult<byte[], KmsMasterKey> decryptData(final byte[] ciphertext) {
        /*
         * XXX: could make a defensive copy here --
         * AwsCrypto#decryptData(byte[]) does not, and ParsedCiphertext includes
         * a warning to that effect
         */
        switch (restrictMode) {
            case ENCRYPT_ONLY:
                throw new UnsupportedOperationException(restrictMode.name());
            default:
                return AWS_CRYPTO.decryptData(masterKeyProvider, ciphertext);
        }
    }

    public CryptoResult<byte[], KmsMasterKey> decryptData(final ParsedCiphertext ciphertext) {
        switch (restrictMode) {
            case ENCRYPT_ONLY:
                throw new UnsupportedOperationException(restrictMode.name());
            default:
                return AWS_CRYPTO.decryptData(masterKeyProvider, ciphertext);
        }
    }

    public CryptoOutputStream<KmsMasterKey> createEncryptingStream(final OutputStream os) {
        return createEncryptingStream(os, EMPTY_MAP);
    }

    public CryptoOutputStream<KmsMasterKey> createEncryptingStream(final OutputStream os,
            final Map<String, String> encryptionContext) {
        switch (restrictMode) {
            case DECRYPT_ONLY:
            case DECRYPT_ONLY_DISCOVERY:
                throw new UnsupportedOperationException(restrictMode.name());
            default:
                return AWS_CRYPTO.createEncryptingStream(masterKeyProvider, os, encryptionContext);
        }
    }

    public CryptoInputStream<KmsMasterKey> createEncryptingStream(final InputStream is) {
        return createEncryptingStream(is, EMPTY_MAP);
    }

    public CryptoInputStream<KmsMasterKey> createEncryptingStream(final InputStream is,
            final Map<String, String> encryptionContext) {
        switch (restrictMode) {
            case DECRYPT_ONLY:
            case DECRYPT_ONLY_DISCOVERY:
                throw new UnsupportedOperationException(restrictMode.name());
            default:
                return AWS_CRYPTO.createEncryptingStream(masterKeyProvider, is, encryptionContext);
        }
    }

    public CryptoOutputStream<KmsMasterKey> createDecryptingStream(final OutputStream os) {
        switch (restrictMode) {
            case ENCRYPT_ONLY:
                throw new UnsupportedOperationException(restrictMode.name());
            default:
                return AWS_CRYPTO.createDecryptingStream(masterKeyProvider, os);
        }
    }

    public CryptoInputStream<KmsMasterKey> createDecryptingStream(final InputStream is) {
        switch (restrictMode) {
            case ENCRYPT_ONLY:
                throw new UnsupportedOperationException(restrictMode.name());
            default:
                return AWS_CRYPTO.createDecryptingStream(masterKeyProvider, is);
        }
    }
}
