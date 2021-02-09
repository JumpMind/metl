package org.jumpmind.metl.core.runtime.resource.aws;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.runtime.resource.FileInfo;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3Directory implements S3BucketOperations {
    public static final class ListFilesPartialResult extends RuntimeException {
        private static final long serialVersionUID = 1143701240959684134L;

        final List<FileInfo> files;

        ListFilesPartialResult(final List<FileInfo> files) {
            this(files, null);
        }

        ListFilesPartialResult(final List<FileInfo> files, Throwable cause) {
            super("S3 ListObjectsV2Request (continuation) failed", cause);
            this.files = Collections.unmodifiableList(files);
        }

        List<FileInfo> files() {
            return files;
        }
    }

    private static final Map<String, String> EMPTY_MAP = Collections.emptyMap();

    /*
     * TODO: Although this conventionally makes S3 ListObjects behave like a
     * file system directory listing, it assumes that the delimiter will ALWAYS
     * be "/" (it might not be, and arguably should be configurable).
     */
    private static final String LIST_FILES_DELIMITER = "/";

    protected final Logger log = LoggerFactory.getLogger(getClass().getName());

    private final Region region;

    private final String bucketName;

    @SuppressWarnings("unused")
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

        if (crypto != null) {
            throw new UnsupportedOperationException(
                    "client-side encryption support is not yet integrated");
        }
        this.crypto = crypto;

        s3 = S3AsyncClient.builder().region(this.region).credentialsProvider(credentialsProvider)
                .build();
    }

    @Override
    public void connect() {
        log.debug("{} is immutable; S3 client has already been created",
                getClass().getSimpleName());
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
        log.debug("ignoring Content-Length (calculated automatically by S3 client)");
    }

    /* S3 PutObject */

    @Override
    public boolean supportsOutputStream() {
        /* TODO: adapt OutputStream->Publisher */
        return false;
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

    @Override
    public OutputStream getOutputStream(final String objectKey, final boolean mustExist,
            final boolean closeClient, final boolean append, final Map<String, String> headers,
            final Map<String, String> parameters) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public CompletableFuture<PutObjectResponse> putObject(final String objectKey,
            final File objectSource) {
        PutObjectRequest request = buildPutObjectRequest(objectKey);
        return s3.putObject(request, AsyncRequestBody.fromFile(objectSource));
    }

    private PutObjectRequest buildPutObjectRequest(final String objectKey) {
        return PutObjectRequest.builder().bucket(bucketName).key(objectKey).build();
    }

    /* S3 GetObject */

    @Override
    public boolean supportsInputStream() {
        /* TODO: adapt InputStream->Subscriber */
        return false;
    }

    @Override
    public InputStream getInputStream(final String objectKey, final boolean mustExist) {
        return getInputStream(objectKey, mustExist, false);
    }

    @Override
    public InputStream getInputStream(final String objectKey, final boolean mustExist,
            final boolean closeClient) {
        return getInputStream(objectKey, mustExist, closeClient, EMPTY_MAP, EMPTY_MAP);
    }

    @Override
    public InputStream getInputStream(final String objectKey, final boolean mustExist,
            final boolean closeClient, final Map<String, String> headers,
            final Map<String, String> parameters) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public CompletableFuture<ResponseBytes<GetObjectResponse>> getObject(final String objectKey) {
        GetObjectRequest request = buildGetObjectRequest(objectKey);
        return s3.getObject(request, AsyncResponseTransformer.toBytes());
    }

    @Override
    public CompletableFuture<GetObjectResponse> getObject(final String objectKey,
            final File objectSink) {
        GetObjectRequest request = buildGetObjectRequest(objectKey);
        return s3.getObject(request, AsyncResponseTransformer.toFile(objectSink));
    }

    private GetObjectRequest buildGetObjectRequest(final String objectKey) {
        return GetObjectRequest.builder().bucket(bucketName).key(objectKey).build();
    }

    /* S3 DeleteObject */

    @Override
    public boolean supportsDelete() {
        return true;
    }

    @Override
    public boolean delete(final String objectKey) {
        return delete(objectKey, false);
    }

    @Override
    public boolean delete(final String objectKey, boolean closeClient) {
        boolean completedWithoutError = false;
        try {
            return completedWithoutError;
        } finally {
            if (closeClient) {
                close(completedWithoutError);
            }
        }
    }

    @Override
    public CompletableFuture<DeleteObjectResponse> deleteObject(final String objectKey) {
        DeleteObjectRequest request = buildDeleteObjectRequest(objectKey);
        return s3.deleteObject(request);
    }

    private DeleteObjectRequest buildDeleteObjectRequest(final String objectKey) {
        return DeleteObjectRequest.builder().bucket(bucketName).key(objectKey).build();
    }

    /* S3 ListObjectsV2 */

    @Override
    public List<FileInfo> listFiles(final String... relativePaths) {
        return listFiles(false, relativePaths);
    }

    @Override
    public List<FileInfo> listFiles(final boolean closeSession, final String... relativePaths) {
        List<FileInfo> files = new ArrayList<>();

        boolean completedWithoutError = false;
        String delimiter = LIST_FILES_DELIMITER;
        String prefix = null;
        try {
            if (relativePaths.length == 0) {
                collateWithContinuations(files, listObjects(delimiter, prefix));
                completedWithoutError = true;
            } else {
                /*
                 * XXX: This _could_ be made to be "concurrent" but at the
                 * expense of a great deal of additional complexity for a use
                 * case that is likely (?) to be very low frequency.
                 */
                for (String relativePath : relativePaths) {
                    prefix = relativePath.endsWith("*")
                            ? relativePath.substring(0, relativePath.length() - 1)
                            : relativePath;
                    if (prefix.isEmpty()) {
                        prefix = null;
                    }
                    collateWithContinuations(files, listObjects(delimiter, prefix));
                }
                completedWithoutError = true;
            }
        } finally {
            if (closeSession) {
                close(completedWithoutError);
            }
        }

        return files;
    }

    private void collateWithContinuations(final List<FileInfo> files,
            final CompletableFuture<ListObjectsV2Response> futureResponse) {
        ListObjectsV2Response response = futureResponse.exceptionally(t -> {
            log.error("S3 ListObjectsV2Request failed: {}", t.toString());
            return null;
        }).join();
        if (response == null) {
            throw new RuntimeException("S3 ListObjectsV2Request failed");
        }

        s3_continuation: while (response != null) {
            files.addAll(response.commonPrefixes().stream().map(this::commonPrefixToFileInfo)
                    .collect(Collectors.toList()));
            files.addAll(response.contents().stream().map(this::s3ObjectToFileInfo)
                    .collect(Collectors.toList()));
            if (response.isTruncated()) {
                response = listObjects(response.delimiter(), response.prefix(), null,
                        response.nextContinuationToken()).exceptionally(t -> {
                            log.warn("S3 ListObjectsV2Request (continuation) failed: {}",
                                    t.toString());
                            return null;
                        }).join();
                if (response == null) {
                    if (files.size() > 0) {
                        throw new ListFilesPartialResult(files);
                    } else {
                        throw new RuntimeException("S3 ListObjectsV2Request (continuation) failed");
                    }
                }
            } else {
                break s3_continuation;
            }
        }
    }

    private FileInfo commonPrefixToFileInfo(final CommonPrefix commonPrefix) {
        return new FileInfo(commonPrefix.prefix(), true, -1, -1);
    }

    private FileInfo s3ObjectToFileInfo(final S3Object object) {
        return new FileInfo(object.key(), false,
                object.lastModified() != null ? object.lastModified().toEpochMilli() : -1,
                object.size() != null ? object.size() : -1);
    }

    @Override
    public CompletableFuture<ListObjectsV2Response> listObjects() {
        return listObjects(null, null);
    }

    @Override
    public CompletableFuture<ListObjectsV2Response> listObjects(final String delimiter,
            final String prefix) {
        return listObjects(delimiter, prefix, null);
    }

    @Override
    public CompletableFuture<ListObjectsV2Response> listObjects(final String delimiter,
            final String prefix, final Integer maxKeys) {
        return listObjects(delimiter, prefix, maxKeys, null);
    }

    @Override
    public CompletableFuture<ListObjectsV2Response> listObjects(final String delimiter,
            final String prefix, final Integer maxKeys, final String continuationToken) {
        ListObjectsV2Request request = buildListObjectsV2Request(delimiter, prefix, maxKeys,
                continuationToken);
        return s3.listObjectsV2(request);
    }

    private ListObjectsV2Request buildListObjectsV2Request(final String delimiter,
            final String prefix, final Integer maxKeys, final String continuationToken) {
        return ListObjectsV2Request.builder().bucket(bucketName).delimiter(delimiter).prefix(prefix)
                .maxKeys(maxKeys).continuationToken(continuationToken).build();
    }

    @Override
    public FileInfo listFile(final String relativePath) {
        return listFile(relativePath, false);
    }

    @Override
    public FileInfo listFile(final String relativePath, final boolean closeSession) {
        throw new UnsupportedOperationException();
    }

    /* S3 CopyObject */

    @Override
    public void copyFile(String fromFilePath, String toFilePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyFile(String fromFilePath, String toFilePath, boolean closeSession) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyToDir(String fromFilePath, String toDirPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyToDir(String fromFilePath, String toDirPath, boolean closeSession) {
        throw new UnsupportedOperationException();
    }

    /* S3 CopyObject, DeleteObject */

    @Override
    public void moveFile(String fromFilePath, String toFilePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveFile(String fromFilePath, String toFilePath, boolean closeSession) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveToDir(String fromFilePath, String toDirPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveToDir(String fromFilePath, String toDirPath, boolean closeSession) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean renameFile(String fromFilePath, String toFilePath) {
        return false;
    }

    @Override
    public boolean renameFile(String fromFilePath, String toFilePath, boolean closeSession) {
        return false;
    }
}
