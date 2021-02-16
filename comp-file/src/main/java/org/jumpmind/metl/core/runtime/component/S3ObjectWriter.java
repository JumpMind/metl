package org.jumpmind.metl.core.runtime.component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.jumpmind.metl.core.runtime.ContentMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.metl.core.runtime.resource.aws.S3BucketOperations;

import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class S3ObjectWriter extends AbstractFileWriter {
    public static final String HEADER_SOURCE_FILE = "S3ObjectSourceFileName";

    private S3BucketOperations bucketOps;

    @Override
    public void start() {
        init();

        bucketOps = (S3BucketOperations) getResourceReference();
        if (bucketOps == null) {
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
        if (inputMessage instanceof ContentMessage<?>) {
            String sourceFileName = inputMessage.getHeader().getAsStrings().get(HEADER_SOURCE_FILE);
            if (sourceFileName != null) {
                File file = new File(sourceFileName);
                String objectKey = getFileName(inputMessage);

                /*
                 * if the PutObject operation itself fails, a
                 * CompletionException wrapping the cause is thrown; if the
                 * source file cannot be deleted on successful completion of
                 * PutObject, the reason is logged at WARN level
                 */
                bucketOps.putObject(objectKey, file).whenCompleteAsync((response,
                        throwable) -> onPutObjectComplete(file, objectKey, response, throwable))
                        .join();
            } else {
                log.warn("nothing to do; {} header is not defined or null", HEADER_SOURCE_FILE);
            }
        } else {
            log.debug("ignoring {}", inputMessage.getClass().getSimpleName());
        }
    }

    private void onPutObjectComplete(final File sourceFile, final String objectKey,
            final PutObjectResponse response, final Throwable throwable) {
        String operationFullDescription = String.format("PutObject %s -> [%s]%s/%s", sourceFile,
                bucketOps.region(), bucketOps.bucketName(), objectKey);
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
        bucketOps = null;
        super.stop();
    }
}
