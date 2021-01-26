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

    /* TODO: this needs to be handled in a better way */
    /**
     * The runtime property key identifying the OpenPGP private key pass phrase.
     *
     * <p>
     * This property is only relevant for {@link PgpDecrypt}.
     * </p>
     */
    String PRIVATE_KEY_PASSPHRASE = "pgp.private.key.passphrase";

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
     * (BouncyCastle) <em>supports</em>. The {@link PgpEncrypt} component, or a
     * subclass thereof, is free to further restrict the <em>acceptable</em>
     * algorithms when the component is initialized.
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
        /*
         * explicitly disallowed by PgpOperation; has no practical usefulness
         * outside of unit and/or integration testing but MUST be defined here
         * for those usages
         */
        NULL(SymmetricKeyAlgorithmTags.NULL),
        /* 128-bit block sizes (recommended): */
        AES_128(SymmetricKeyAlgorithmTags.AES_128), /* default */
        AES_192(SymmetricKeyAlgorithmTags.AES_192),
        AES_256(SymmetricKeyAlgorithmTags.AES_256),
        CAMELLIA_128(SymmetricKeyAlgorithmTags.CAMELLIA_128),
        CAMELLIA_192(SymmetricKeyAlgorithmTags.CAMELLIA_192),
        CAMELLIA_256(SymmetricKeyAlgorithmTags.CAMELLIA_256),
        TWOFISH(SymmetricKeyAlgorithmTags.TWOFISH),
        /* 64-bit block sizes: */
        BLOWFISH(SymmetricKeyAlgorithmTags.BLOWFISH), /* prefer Twofish */
        CAST5(SymmetricKeyAlgorithmTags.CAST5),
        SAFER(SymmetricKeyAlgorithmTags.SAFER),
        IDEA(SymmetricKeyAlgorithmTags.IDEA), /* PGP legacy interop only */
        /**
         * <p>
         * <strong>SHOULD NOT USE!</strong>
         * </p>
         * <p>
         * TripleDES/3DES/TDEA encryption is deprecated and should not be used
         * for new applications. <em>Decryption-only</em> is permitted as
         * "legacy use." TripleDES will be disallowed altogether after 2023.
         * </p>
         */
        @Deprecated
        TRIPLE_DES(SymmetricKeyAlgorithmTags.TRIPLE_DES), /* SHOULD NOT USE */
        /**
         * <p>
         * <strong>DO NOT USE!</strong>
         * </p>
         * <p>
         * DES has been officially withdrawn (NIST 2005). Keys can be
         * brute-forced in seconds (best case) or <24 hours (worst case).
         * </p>
         *
         * @see <a href="https://crack.sh/">Crack.sh guarantees that it will
         *      100% produce a working key for jobs submitted.</a>
         */
        @Deprecated
        DES(SymmetricKeyAlgorithmTags.DES); /* DO NOT USE */

        private final int id;

        private SymmetricKeyAlgorithm(final int id) {
            this.id = id;
        }

        public int intValue() {
            return id;
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
