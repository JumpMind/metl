package org.jumpmind.metl.core.runtime.resource.aws;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.bouncycastle.util.io.Streams;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.runtime.resource.FileInfo;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.encryptionsdk.CryptoInputStream;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class S3Directory implements S3BucketOperations {
    /* XXX: configurable? */
    private static final int SINK_BUFFER_SIZE = 8192;

    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();

    private static final Map<String, String> EMPTY_MAP = Collections.emptyMap();

    protected final Logger log = LoggerFactory.getLogger(getClass().getName());

    private final Region region;

    private final String bucketName;

    private final ClientSideCrypto crypto;

    private final S3AsyncClient s3;

    /**
     * Creates an immutable "bucket view" (i.e. {@link IDirectory}) for an S3
     * resource runtime.
     * 
     * @param resource
     *            TODO: ???
     * @param region
     *            the S3 region (may be <code>null</code>)
     * @param bucketName
     *            the S3 bucket name
     * @param crypto
     *            the CSE-KMS helper (if crypto is enabled)
     * @param credentialsProviders
     *            how the S3 client acquires IAM authentication credentials
     */
    public S3Directory(final Resource resource, final Region region, final String bucketName,
            final ClientSideCrypto crypto, final AwsCredentialsProvider credentialsProvider) {
        this.region = region;
        this.bucketName = bucketName;
        this.crypto = crypto;
        s3 = S3AsyncClient.builder().region(this.region).credentialsProvider(credentialsProvider)
                .build();
    }

    @Override
    public void connect() {
        log.debug("{} is immutable; {} has already been created", getClass().getSimpleName(),
                s3.getClass().getName());
    }

    @Override
    public void close() {
        close(true);
    }

    @Override
    public void close(final boolean success) {
        if (!success) {
            log.warn("closing S3 client following unsuccessful operation");
        }
        s3.close();
    }

    @Override
    public boolean requiresContentLength() {
        return false;
    }

    @Override
    public void setContentLength(final int length) {
        /* no-op (calculated automatically by S3Client) */
    }

    @Override
    public boolean supportsInputStream() {
        return true;
    }

    /*
     * XXX: what is a use case for mustExist? how could an input stream be
     * returned for a backing file/resource/object that does NOT exist? (it may
     * just be that this is irrelevant for S3, as going through the steps to
     * issue a HeadObject or request before issuing a GetObject request would be
     * a redundant/unnecessary HTTP round-trip since -- if the object doesn't
     * exist, then AWS throws NoSuchKeyException with a 404)
     */
    @Override
    public InputStream getInputStream(final String objectKey, final boolean mustExist) {
        return getInputStream(objectKey, mustExist, false);
    }

    /*
     * XXX: make sure I interpret the meaning of "closeClient" correctly - I
     * understand it to mean that *this* resource is intended to perform one
     * singular operation (GetObject) and that's it?
     */
    @Override
    public InputStream getInputStream(final String objectKey, final boolean mustExist,
            final boolean closeClient) {
        return getInputStream(objectKey, mustExist, closeClient, EMPTY_MAP, EMPTY_MAP);
    }

    @Override
    public InputStream getInputStream(final String objectKey, final boolean mustExist,
            final boolean closeClient, final Map<String, String> headers,
            final Map<String, String> parameters) {

        boolean success = false;
        try {
            InputStream stream = getObject(objectKey);

            success = true;
            return stream;
        } finally {
            if (closeClient) {
                close(success);
            }
        }
    }

    @Override
    public CompletableFuture<ResponseBytes<GetObjectResponse>> getObject(final String key) {
        return getObject(key, EMPTY_MAP);
    }

    @Override
    public CompletableFuture<ResponseBytes<GetObjectResponse>> getObject(final String key,
            final Map<String, String> suppliedAad) {
        /* defensive copy of AAD (crypto only) */
        Map<String, String> aad = new HashMap<>(requireNonNull(suppliedAad, "suppliedAad"));

        CompletableFuture<ResponseBytes<GetObjectResponse>> responseFuture = s3.getObject(
                GetObjectRequest.builder().bucket("my-bucket").key("my-object-key").build(),
                AsyncResponseTransformer.toBytes());
        return (crypto == null) ? responseFuture
                : responseFuture.thenCompose(this::decryptGetObjectResponse);
    }

    /* make sure this is only composed AFTER an explicit crypto check */
    private CompletableFuture<ResponseBytes<GetObjectResponse>> decryptGetObjectResponse(
            final ResponseBytes<GetObjectResponse> response) {
        if (response.response().sdkHttpResponse().isSuccessful()) {
            return CompletableFuture.supplyAsync(() -> {
                byte[] ct = response.asByteArrayUnsafe();
                CryptoResult<byte[], KmsMasterKey> cryptoResult = crypto.decryptData(ct);
                log.debug("decrypted object having AAD {} using {}",
                        cryptoResult.getEncryptionContext(), cryptoResult.getCryptoAlgorithm());
                return ResponseBytes.fromByteArrayUnsafe(response.response(),
                        cryptoResult.getResult());
            });
        } else {
            return CompletableFuture.completedFuture(response);
        }
    }

    @Override
    public boolean supportsOutputStream() {
        return true;
    }

    @Override
    public OutputStream getOutputStream(final String objectKey, final boolean mustExist) {
        return getOutputStream(objectKey, mustExist, false, false);
    }

    @Override
    public OutputStream getOutputStream(final String objectKey, final boolean mustExist,
            final boolean closeClient, final boolean append) {
        return getOutputStream(objectKey, mustExist, closeClient, append, EMPTY_MAP, EMPTY_MAP);
    }

    /*
     * for large objects, prefer to use #putObject(String, InputStream) directly
     * to avoid the
     */
    @Override
    public OutputStream getOutputStream(final String objectKey, final boolean mustExist,
            final boolean closeClient, final boolean append, final Map<String, String> headers,
            final Map<String, String> parameters) {
        if (mustExist || append) {
            throw new UnsupportedOperationException(
                    "cannot write or append to existing S3 object; can only create/overwrite");
        }

        S3Directory s3d = this;
        return new FilterOutputStream(new ByteArrayOutputStream(SINK_BUFFER_SIZE)) {
            @Override
            public void close() throws IOException {
                super.close();// defaultPutObjectCompletionAction

                /*
                 * nasty business, but it gets the job done without having two
                 * copies of the byte[] lying around
                 */
                byte[] buf = ((ByteArrayOutputStream) out).toByteArray();
                out = null;

                InputStream objectSource = new ByteArrayInputStream(buf);
                putObject(objectKey, objectSource, s3d::defaultPutObjectCompletionAction).join();
            }
        };
    }

    @Override
    public CompletableFuture<PutObjectResponse> putObject(final String key,
            final InputStream objectSource) {
        return putObject(key, objectSource, EMPTY_MAP);
    }

    @Override
    public CompletableFuture<PutObjectResponse> putObject(final String key,
            final InputStream objectSource, final Map<String, String> suppliedAad) {
        /* defensive copy of AAD (crypto only) */
        Map<String, String> aad = new HashMap<>(requireNonNull(suppliedAad, "suppliedAad"));

        /*
         * object will be either encrypted + digested (if crypto enabled) OR
         * just digested; either way we need a MessageDigest and a sink for the
         * processed object bytes
         */
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalArgumentException(ex);
        }
        ByteArrayOutputStream objectSink = new ByteArrayOutputStream(SINK_BUFFER_SIZE);

        try {
            if (crypto == null) {
                digestPipe(objectSource, md, objectSink);
            } else {
                /*
                 * XXX: would be ideal to be able to convey the AWS-added AAD
                 * keys back to caller, but haven't found a simple way yet
                 */
                CryptoInputStream<KmsMasterKey> cryptoSource = crypto
                        .createEncryptingStream(objectSource, aad);
                digestPipe(cryptoSource, md, objectSink);
                CryptoResult<CryptoInputStream<KmsMasterKey>, KmsMasterKey> cryptoResult = cryptoSource
                        .getCryptoResult();
                log.debug("encrypted object using {} with AAD {}",
                        cryptoResult.getCryptoAlgorithm(), cryptoResult.getEncryptionContext());
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        /*
         * whichever stream was passed to digestPipe() is now closed, and the
         * MD5 digest is ready
         */
        String objectDigest = B64_ENCODER.encodeToString(md.digest());

        PutObjectRequest objectRequest = PutObjectRequest.builder().bucket(bucketName).key(key)
                .contentMD5(objectDigest).build();
        return s3.putObject(objectRequest, AsyncRequestBody.fromBytes(objectSink.toByteArray()));
    }

    @Override
    public CompletableFuture<PutObjectResponse> putObject(String key, InputStream objectSource,
            BiConsumer<? super PutObjectResponse, ? super Throwable> whenCompleteAction) {
        return putObject(key, objectSource, whenCompleteAction, EMPTY_MAP);
    }

    @Override
    public CompletableFuture<PutObjectResponse> putObject(String key, InputStream objectSource,
            BiConsumer<? super PutObjectResponse, ? super Throwable> whenCompleteAction,
            final Map<String, String> suppliedAad) {
        return putObject(key, objectSource, suppliedAad).whenComplete(whenCompleteAction);
    }

    private static void digestPipe(final InputStream source, final MessageDigest md,
            final OutputStream sink) throws IOException {
        try (InputStream digester = new DigestInputStream(source, md)) {
            /*
             * BC is a direct dependency of AWS Encryption SDK, so may as well
             * use what we've got
             */
            Streams.pipeAll(digester, sink);
        }
    }

    private void defaultPutObjectCompletionAction(final PutObjectResponse response,
            final Throwable throwable) {
        if (response != null) {
            if (log.isInfoEnabled()) {
                log.info("S3 PutObject request ID {} -> HTTP {} {}",
                        response.responseMetadata().requestId(),
                        response.sdkHttpResponse().statusCode(),
                        response.sdkHttpResponse().statusText().orElse("(no reason provided)"));
            }
        } else {
            if (throwable instanceof SdkServiceException) {
                SdkServiceException svcEx = (SdkServiceException) throwable;
                log.error("S3 PutObject request ID {} -> status {}", svcEx.requestId(),
                        svcEx.statusCode());
            }

            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            } else {
                throw new RuntimeException(throwable);
            }
        }
    }

    @Override
    public boolean supportsDelete() {
        return true;
    }

    @Override
    public boolean delete(final String objectKey) {
        return delete(objectKey, false);
    }

    @Override
    public boolean delete(String objectKey, boolean closeClient) {
        boolean success = false;
        try {
            WaiterResponse<HeadObjectResponse> waiter = deleteObject(objectKey);
            ResponseOrException<HeadObjectResponse> rx = waiter.matched();

            Throwable t = rx.exception().orElse(null);
            if (t instanceof NoSuchKeyException) {
                success = true;
                return success;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Exception) {
                throw new RuntimeException(t);
            } else if (t != null) {
                throw new RuntimeException("unknown S3 failure: " + t);
            }

            /* t is null */
            HeadObjectResponse response = rx.response().orElse(null);
            if (response != null) {
                int statusCode = response.sdkHttpResponse().statusCode();
                if (statusCode == 404) {
                    success = true;
                    return success;
                } else {
                    throw new RuntimeException(String.format("S3 DeleteObject -> %s %s", statusCode,
                            response.sdkHttpResponse().statusText()
                                    .orElse("(no reason provided)")));
                }
            } else {
                throw new RuntimeException("unknown S3 failure");
            }
        } finally {
            if (closeClient) {
                close(success);
            }
        }
    }

    @Override
    public WaiterResponse<HeadObjectResponse> deleteObject(final String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName)
                .key(key).build();

        s3.deleteObject(deleteObjectRequest);

        return s3.waiter().waitUntilObjectNotExists(b -> b.bucket(bucketName).key(key));
    }

    @Override
    public List<FileInfo> listFiles(String... relativePaths) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<FileInfo> listFiles(boolean closeSession, String... relativePaths) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FileInfo listFile(String relativePath) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FileInfo listFile(String relativePath, boolean closeSession) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void copyFile(String fromFilePath, String toFilePath) {
        // TODO Auto-generated method stub
    }

    @Override
    public void copyFile(String fromFilePath, String toFilePath, boolean closeSession) {
        // TODO Auto-generated method stub
    }

    @Override
    public void moveFile(String fromFilePath, String toFilePath) {
        // TODO Auto-generated method stub
    }

    @Override
    public void moveFile(String fromFilePath, String toFilePath, boolean closeSession) {
        // TODO Auto-generated method stub
    }

    @Override
    public void copyToDir(String fromFilePath, String toDirPath) {
        // TODO Auto-generated method stub
    }

    @Override
    public void copyToDir(String fromFilePath, String toDirPath, boolean closeSession) {
        // TODO Auto-generated method stub
    }

    @Override
    public void moveToDir(String fromFilePath, String toDirPath) {
        // TODO Auto-generated method stub
    }

    @Override
    public void moveToDir(String fromFilePath, String toDirPath, boolean closeSession) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean renameFile(String fromFilePath, String toFilePath) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean renameFile(String fromFilePath, String toFilePath, boolean closeSession) {
        // TODO Auto-generated method stub
        return false;
    }
}
