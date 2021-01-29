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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.Deflater;

import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;

/**
 * Constants related to the {@link PgpEncrypt} and {@link PgpDecrypt} plug-in
 * configuration settings.
 */
public interface PgpConfiguration {
    /**
     * The runtime property key specifying the location of the OpenPGP public
     * key file.
     *
     * <p>
     * This property is only relevant for {@link PgpEncrypt} and is mutually
     * exclusive with respect to {@link #PRIVATE_KEY_LOCATION}.
     * </p>
     */
    String PUBLIC_KEY_LOCATION = "pgp.public.key.location";

    /**
     * The runtime property key specifying the location of the OpenPGP private
     * key file.
     *
     * <p>
     * This property is only relevant for {@link PgpDecrypt} and is mutually
     * exclusive with respect to {@link #PUBLIC_KEY_LOCATION}.
     * </p>
     */
    String PRIVATE_KEY_LOCATION = "pgp.private.key.location";

    /**
     * The runtime property key identifying the location of the OpenPGP private
     * key pass phrase.
     *
     * <p>
     * This property is only relevant for {@link PgpDecrypt}.
     * </p>
     */
    String PRIVATE_KEY_PASSPHRASE_LOCATION = "pgp.private.key.passphrase.location";

    /**
     * The runtime property key identifying the OpenPGP
     * <strong>symmetric</strong> key algorithm to use for encryption and
     * decryption.
     *
     * <p>
     * This property is only relevant for {@link PgpEncrypt}.
     * </p>
     * <p>
     * <p>
     * The {@link PgpDecrypt} component (or any external OpenPGP-compatible
     * recipient) will automatically detect the algorithm for the symmetric
     * encryption key.
     * </p>
     *
     * @see SymmetricKeyAlgorithm
     */
    String SYMMETRIC_KEY_ALGORITHM = "pgp.key.algorithm";

    /**
     * The runtime property key identifying the OpenPGP compression algorithm to
     * use prior to encryption.
     *
     * <p>
     * This property is only relevant for {@link PgpEncrypt}.
     * </p>
     * <p>
     * The encrypted PGP message carries the necessary information for
     * decompression, which will be handled automatically by {@link PgpDecrypt}
     * (or any external OpenPGP-compatible recipient).
     * </p>
     *
     * @see CompressionAlgorithm
     * @see #COMPRESSION_ENABLED
     */
    String COMPRESSION_ALGORITHM = "pgp.compression.algorithm";

    /**
     * The runtime property key identifying the OpenPGP compression level to use
     * with {@link #COMPRESSION_ALGORITHM}.
     *
     * <p>
     * This property is only relevant for {@link PgpEncrypt}.
     * </p>
     * <p>
     * The encrypted PGP message carries the necessary information for
     * decompression, which will be handled automatically by {@link PgpDecrypt}
     * (or any external OpenPGP-compatible recipient).
     * </p>
     *
     * @see CompressionLevel
     * @see #COMPRESSION_ENABLED
     */
    String COMPRESSION_LEVEL = "pgp.compression.level";

    /**
     * The runtime property key controlling whether or not the encrypted message
     * is ASCII-encoded for safe transmission with any message format.
     *
     * <p>
     * This property is only relevant for {@link PgpEncrypt}.
     * </p>
     * <p>
     * The {@link PgpDecrypt} component (or any external OpenPGP-compatible
     * recipient) will automatically detect an ASCII-armored PGP message and
     * handle it appropriately.
     * </p>
     */
    String ARMORED = "pgp.armored";

    /**
     * Supported OpenPGP symmetric key algorithms.
     *
     * <p>
     * These algorithms identify those that the underlying crypto implementor
     * (BouncyCastle) <em>supports</em>. The UI, {@link PgpEncrypt} component
     * runtime, or a subclass thereof, is free to further restrict the
     * <em>acceptable</em> algorithms.
     * </p>
     *
     * @see org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
     * @see <a href="https://tools.ietf.org/html/rfc4880#section-9.2">RFC 4880
     *      OpenPGP Message Format Section 9.2 Symmetric-Key Algorithms</a>
     * @see <a href=
     *      "https://csrc.nist.gov/publications/detail/sp/800-57-part-1/rev-5/final">NIST
     *      Recommendation for Key Management (SP 800-57 Part 1 Rev. 5)</a>.
     */
    enum SymmetricKeyAlgorithm {
        /** No practical usefulness beyond testing. */
        NULL(SymmetricKeyAlgorithmTags.NULL, 0, 0, false),
        /* 128-bit or higher key and block sizes (recommended): */
        /** The current default. */
        AES_128(SymmetricKeyAlgorithmTags.AES_128, 128, 128, true),
        AES_192(SymmetricKeyAlgorithmTags.AES_192, 192, 128, true),
        AES_256(SymmetricKeyAlgorithmTags.AES_256, 256, 128, true),
        CAMELLIA_128(SymmetricKeyAlgorithmTags.CAMELLIA_128, 128, 128, true),
        CAMELLIA_192(SymmetricKeyAlgorithmTags.CAMELLIA_192, 192, 128, true),
        CAMELLIA_256(SymmetricKeyAlgorithmTags.CAMELLIA_256, 256, 128, true),
        TWOFISH(SymmetricKeyAlgorithmTags.TWOFISH, 256, 128, true),
        /* less than 128-bit key or block sizes (not recommended): */
        BLOWFISH(SymmetricKeyAlgorithmTags.BLOWFISH, 128, 64, false),
        CAST5(SymmetricKeyAlgorithmTags.CAST5, 128, 64, false),
        SAFER(SymmetricKeyAlgorithmTags.SAFER, 128, 64, false),
        /** For PGP legacy interoperability only. */
        IDEA(SymmetricKeyAlgorithmTags.IDEA, 128, 64, false),
        /**
         * SHOULD NOT USE!
         *
         * <p>
         * TripleDES/3DES/DESede/TDEA encryption is deprecated (NIST 2018) and
         * should not be used for new applications. <em>Decryption-only</em> is
         * permitted as "legacy use" through 2023.
         * </p>
         * <p>
         * TripleDES will be withdrawn by NIST after 2023.
         * </p>
         * 
         * @see <a href=
         *      "https://csrc.nist.gov/publications/detail/sp/800-131a/rev-2/final">Transitioning
         *      the Use of Cryptographic Algorithms and Key Lengths</a>
         */
        @Deprecated
        TRIPLE_DES(SymmetricKeyAlgorithmTags.TRIPLE_DES, 168, 64, false),
        /**
         * <strong>DO NOT USE!</strong>
         * 
         * <p>
         * DES has been officially withdrawn (NIST 2005).
         * </p>
         *
         * @see <a href=
         *      "https://www.nist.gov/news-events/news/2005/06/nist-withdraws-outdated-data-encryption-standard">NIST
         *      Withdraws Outdated Data Encryption Standard</a>
         */
        @Deprecated
        DES(SymmetricKeyAlgorithmTags.DES, 64, 64, false);

        public static Set<SymmetricKeyAlgorithm> allApproved() {
            return ALL_APPROVED;
        }

        private static final Set<SymmetricKeyAlgorithm> ALL_APPROVED = Collections
                .unmodifiableSet(Arrays.asList(values()).stream().filter(a -> a.isApproved())
                        .collect(Collectors.toSet()));

        private final int id;

        private final int keySize;

        private final int blockSize;

        /*
         * this is an admittedly nebulous concept (approved by whom? NIST?
         * BouncyCastle? JumpMind? John Doe?); for our purposes here, we
         * generally say that any algorithm that has been officially
         * deprecated/withdrawn by a recognized authority (e.g. NIST, NESSIE,
         * CRYPTREC) -or- that has a key size or block size less than 128 is
         * _not_ approved
         */
        private final boolean approved;

        private SymmetricKeyAlgorithm(final int id, final int keySize, final int blockSize,
                final boolean approved) {
            this.id = id;
            this.keySize = keySize;
            this.blockSize = blockSize;
            this.approved = approved;
        }

        public int intValue() {
            return id;
        }

        public int keySize() {
            return keySize;
        }

        public int blockSize() {
            return blockSize;
        }

        public boolean isApproved() {
            return approved;
        }
    };

    /**
     * Supported OpenPGP compression algorithms.
     *
     * @see org.bouncycastle.bcpg.CompressionAlgorithmTags
     * @see <a href="https://tools.ietf.org/html/rfc4880#section-9.3">RFC 4880
     *      OpenPGP Message Format Section 9.3 Compression Algorithms</a>
     */
    enum CompressionAlgorithm {
        UNCOMPRESSED(CompressionAlgorithmTags.UNCOMPRESSED),
        ZIP(CompressionAlgorithmTags.ZIP),
        ZLIB(CompressionAlgorithmTags.ZLIB),
        BZIP2(CompressionAlgorithmTags.BZIP2);

        private final int id;

        private CompressionAlgorithm(final int id) {
            this.id = id;
        }

        public int intValue() {
            return id;
        }
    };

    /**
     * Compression levels for OpenPGP compression.
     *
     * @see java.util.zip.Deflater
     */
    enum CompressionLevel {
        DEFAULT_COMPRESSION(Deflater.DEFAULT_COMPRESSION),
        NO_COMPRESSION(Deflater.NO_COMPRESSION),
        BEST_SPEED(Deflater.BEST_SPEED),
        BEST_COMPRESSION(Deflater.BEST_COMPRESSION);

        private final int id;

        private CompressionLevel(final int id) {
            this.id = id;
        }

        public int intValue() {
            return id;
        }
    };
}
