package org.jumpmind.metl.core.runtime.resource.aws;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.runtime.resource.FileInfo;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3Directory implements IS3BucketOperations {
    public static final class Settings {
        public static final String LIST_FILES_DELIMITER = "aws.s3.list.files.delimiter";

        public static final String DEFAULT_LIST_FILES_DELIMITER = "/";

        public static final String TRANSFER_WINDOW_SIZE = "aws.s3.transfer.window.size";

        public static final int DEFAULT_TRANSFER_WINDOW_SIZE = 16384;

        Settings() {
            throw new UnsupportedOperationException("do not instantiate");
        }
    }

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

    protected final Logger log = LoggerFactory.getLogger(getClass().getName());

    private final Region region;

    private final String bucketName;

    private final ClientSideCrypto crypto;

    private final S3AsyncClient s3;

    private final String listFilesDelimiter;

    private final int transferWindowSize;

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
            final S3AsyncClient s3, final ClientSideCrypto crypto, final String listFilesDelimiter,
            final int transferWindowSize) {
        this.region = region;
        this.bucketName = bucketName;

        if (crypto != null) {
            throw new UnsupportedOperationException(
                    "client-side encryption support is not yet integrated");
        }
        this.crypto = crypto;

        this.s3 = s3;

        this.listFilesDelimiter = listFilesDelimiter;
        this.transferWindowSize = transferWindowSize;
    }

    @Override
    public Region region() {
        return region;
    }

    @Override
    public String bucketName() {
        return bucketName;
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

    /* S3 HeadObject */

    @Override
    public CompletableFuture<HeadObjectResponse> headObject(final String objectKey) {
        HeadObjectRequest request = buildHeadObjectRequest(objectKey);
        return s3.headObject(request);
    }

    private HeadObjectRequest buildHeadObjectRequest(final String objectKey) {
        return HeadObjectRequest.builder().bucket(bucketName).key(objectKey).build();
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
            deleteObject(objectKey).join();
            completedWithoutError = true;
            return true;
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
    public List<FileInfo> listFiles(final String... prefixes) {
        return listFiles(false, prefixes);
    }

    @Override
    public List<FileInfo> listFiles(final boolean closeSession, final String... prefixes) {
        List<FileInfo> backingFiles = new ArrayList<>();
        /* need a synchronized view for collation */
        List<FileInfo> files = Collections.synchronizedList(backingFiles);

        /*
         * either no prefixes or any normalized-null prefix means "all objects"
         * 
         * potential object matches for any NON-null effective prefix are
         * naturally a subset of "all objects"
         * 
         * therefore, if ANY prefix is normalized to null then we'll have fewer
         * "effective" prefixes, meaning we can make one single ListObjectsV2
         * request for all objects to avoid unnecessary calls (and, more
         * importantly, a cartesian result)
         */
        List<String> effectivePrefixes = Arrays.stream(prefixes).map(this::normalizePrefix)
                .filter(Objects::nonNull).collect(Collectors.toList());
        boolean listAll = prefixes.length == 0 || effectivePrefixes.size() < prefixes.length;

        @SuppressWarnings("rawtypes")
        CompletableFuture[] batch;
        if (listAll) {
            batch = new CompletableFuture[] { listObjects(listFilesDelimiter, null)
                    .thenAccept(response -> collateWithContinuations(files, response)) };

            if (effectivePrefixes.size() > 0) {
                log.warn(
                        "IGNORING prefix(es) {} and returning ALL matches "
                                + "(at least 1 prefix normalizes to null - i.e. \"all objects\")",
                        effectivePrefixes);
            }
        } else {
            batch = effectivePrefixes.stream()
                    .map(prefix -> listObjects(listFilesDelimiter, prefix)
                            .thenAccept(response -> collateWithContinuations(files, response)))
                    .toArray(CompletableFuture[]::new);
        }

        boolean completedWithoutError = false;
        try {
            CompletableFuture.allOf(batch).join();

            completedWithoutError = true;
            /* caller receives non-synchronized List */
            return backingFiles;
        } catch (CompletionException ex) {
            if (files.size() > 0) {
                throw new ListFilesPartialResult(backingFiles, ex);
            } else {
                throw ex;
            }
        } finally {
            if (closeSession) {
                close(completedWithoutError);
            }
        }
    }

    private String normalizePrefix(final String rawPrefix) {
        String prefix = rawPrefix.trim();
        if (prefix.endsWith("*")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return !prefix.isEmpty() ? prefix : null;
    }

    private void collateWithContinuations(final List<FileInfo> files,
            final ListObjectsV2Response response) {
        files.addAll(response.commonPrefixes().stream().map(this::commonPrefixToFileInfo)
                .collect(Collectors.toList()));
        files.addAll(response.contents().stream().map(this::s3ObjectToFileInfo)
                .collect(Collectors.toList()));

        if (response.isTruncated()) {
            listObjects(response.delimiter(), response.prefix(), null,
                    response.nextContinuationToken()).thenAccept(
                            continuedResponse -> collateWithContinuations(files, continuedResponse))
                            .join();
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
    public FileInfo listFile(final String objectKey) {
        return listFile(objectKey, false);
    }

    /*
     * XXX: this could be S3 HeadObject instead, but for the sake of consistency
     * w/r/t SftpDirectory this impl maintains the possibility of returning a
     * "directory" FileInfo
     */
    @Override
    public FileInfo listFile(final String objectKey, final boolean closeSession) {
        List<FileInfo> files = listFiles(closeSession, objectKey);
        /*
         * if there's an exact match, return it; otherwise return the last
         * FileInfo matched
         */
        return files.stream().filter(fi -> fi.getName().equals(objectKey)).findFirst()
                .orElse(files.size() != 0 ? files.get(files.size() - 1) : null);
    }

    /* S3 CopyObject? */

    @Override
    public void copyFile(final String fromFilePath, final String toFilePath) {
        copyFile(fromFilePath, toFilePath, false);
    }

    @Override
    public void copyFile(final String fromFilePath, final String toFilePath,
            final boolean closeSession) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void copyToDir(final String fromFilePath, final String toDirPath) {
        copyToDir(fromFilePath, toDirPath, false);
    }

    @Override
    public void copyToDir(final String fromFilePath, final String toDirPath,
            final boolean closeSession) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /* S3 CopyObject,DeleteObject? */

    @Override
    public void moveFile(final String fromFilePath, final String toFilePath) {
        moveFile(fromFilePath, toFilePath, false);
    }

    @Override
    public void moveFile(final String fromFilePath, final String toFilePath,
            final boolean closeSession) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void moveToDir(final String fromFilePath, final String toDirPath) {
        moveToDir(fromFilePath, toDirPath, false);
    }

    @Override
    public void moveToDir(final String fromFilePath, final String toDirPath,
            final boolean closeSession) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public boolean renameFile(final String fromFilePath, final String toFilePath) {
        return renameFile(fromFilePath, toFilePath, false);
    }

    @Override
    public boolean renameFile(final String fromFilePath, final String toFilePath,
            final boolean closeSession) {
        boolean completedWithoutError = false;
        try {
            moveFile(fromFilePath, toFilePath);
            completedWithoutError = true;
            return true;
        } catch (Exception ex) {
            return false;
        } finally {
            if (closeSession) {
                close(completedWithoutError);
            }
        }
    }
}
