package org.jumpmind.metl.core.runtime.component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.metl.core.runtime.resource.aws.S3BucketOperations;

import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class S3ObjectWriter extends AbstractFileWriter {
    public static final String HEADER_SOURCE_FILE = "S3ObjectSourceFileName";

    private S3BucketOperations bucket;

    @Override
    public void start() {
        init();

        bucket = (S3BucketOperations) getResourceReference();
        if (bucket == null) {
            throw new IllegalStateException("S3 resource has not been configured.");
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(final Message inputMessage, final ISendMessageCallback callback,
            final boolean unitOfWorkBoundaryReached) {
        String sourceFileName = inputMessage.getHeader().getAsStrings().get(HEADER_SOURCE_FILE);
        if (sourceFileName != null) {
            File file = new File(sourceFileName);
            String objectKey = getFileName(inputMessage);

            /*
             * if the PutObject operation itself fails, a CompletionException
             * wrapping the cause is thrown; if the source file cannot be
             * deleted on successful completion of PutObject, the reason is
             * logged at WARN level
             */
            bucket.putObject(objectKey, file).whenCompleteAsync((response,
                    throwable) -> onPutObjectComplete(file, objectKey, response, throwable)).join();
        } else {
            log.debug("ignoring {}; {} header not defined or null", inputMessage,
                    HEADER_SOURCE_FILE);
        }
    }

    private void onPutObjectComplete(final File sourceFile, final String objectKey,
            final PutObjectResponse response, final Throwable throwable) {
        String operationFullDescription = String.format("PutObject %s -> [%s]%s/%s", sourceFile,
                bucket.region(), bucket.bucketName(), objectKey);
        if (response != null) {
            if (log.isInfoEnabled()) {
                log.info("{}: HTTP {} {}", operationFullDescription,
                        response.sdkHttpResponse().statusCode(),
                        response.sdkHttpResponse().statusText().orElse("(reason not provided)"));
            }

            try {
                Files.delete(sourceFile.toPath());
            } catch (IOException ex) {
                log.warn("unable to delete source file: {}", ex.toString());
            }
        } else {
            log.error("{}: {}", operationFullDescription, throwable.toString());
        }
    }

    @Override
    protected IDirectory initStream(String fileName) {
        /*
         * XXX: this deletes the file name (object key) by default; unnecessary
         * (and undesirable) for S3
         */
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop() {
        /* TODO: is this correct w/r/t lifecycle management? */
        bucket = null;
        getResourceRuntime().stop();
        super.stop();
    }
}
