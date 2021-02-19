package org.jumpmind.metl.core.runtime.component;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jumpmind.metl.core.runtime.BinaryMessage;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.aws.IS3BucketOperations;
import org.jumpmind.properties.TypedProperties;

import software.amazon.awssdk.http.SdkHttpResponse;

public class S3FileUtil extends AbstractComponentRuntime {
    public static enum Action {
        Upload,
        Download,
        Delete
    };

    public final static String SETTING_ACTION = "action";

    /*
     * interpreted with respect to the "action" setting - can be either the
     * source of an Upload operation or the target of a Download operation if
     * using one of the S3 putObject/getObject java.io.File overloads
     */
    public final static String SETTING_FILE_NAME = "file.name";

    public final static String SETTING_GET_FILE_NAME_FROM_MESSAGE = "get.file.name.from.message";

    public final static String SETTING_OBJECT_KEY = "object.key";

    public final static String SETTING_GET_OBJECT_KEY_FROM_MESSAGE = "get.object.key.from.message";

    private Action action = Action.Upload;

    private String runWhen = PER_UNIT_OF_WORK;

    private String fileName;

    private boolean getFileNameFromMessage;

    private String objectKey;

    private boolean getObjectKeyFromMessage;

    private IS3BucketOperations bucketOps;

    @Override
    public void start() {
        TypedProperties typedProperties = getTypedProperties();

        action = Action.valueOf(typedProperties.get(SETTING_ACTION, Action.Upload.name()));
        runWhen = typedProperties.get(RUN_WHEN, PER_UNIT_OF_WORK);

        fileName = typedProperties.get(SETTING_FILE_NAME);
        getFileNameFromMessage = typedProperties.is(SETTING_GET_FILE_NAME_FROM_MESSAGE,
                getFileNameFromMessage);

        objectKey = typedProperties.get(SETTING_OBJECT_KEY);
        getObjectKeyFromMessage = typedProperties.is(SETTING_GET_OBJECT_KEY_FROM_MESSAGE,
                getObjectKeyFromMessage);

        bucketOps = getResourceReference();
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
        if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
                || (!PER_UNIT_OF_WORK.equals(runWhen)
                        && !(inputMessage instanceof ControlMessage))) {
            ArrayList<String> namesProcessed = new ArrayList<>();
            /* permits handling 1..N sources via CompletableFuture#allOf */
            CompletableFuture<Void> handled;
            switch (action) {
                case Upload:
                    if (inputMessage instanceof BinaryMessage) {
                        handled = uploadBytes((BinaryMessage) inputMessage, namesProcessed);
                    } else {
                        handled = uploadFiles(inputMessage, namesProcessed);
                    }
                    break;
                case Download:
                    handled = downloadObjects(inputMessage, namesProcessed);
                    break;
                case Delete:
                    handled = deleteObjects(inputMessage, namesProcessed);
                    break;
                default:
                    /* unreachable */
                    throw new IllegalStateException("invalid action");
            }
            /*
             * XXX: will throw CompletionException on any failure; could be
             * modified to convey partial success if desired
             */
            handled.join();
            callback.sendTextMessage(null, namesProcessed);
        } else {
            log.warn("ignoring {}; runWhen={}", inputMessage.getClass().getSimpleName(), runWhen);
        }
    }

    private CompletableFuture<Void> uploadBytes(final BinaryMessage message,
            final List<String> namesProcessed) {
        byte[] sourceBytes = message.getPayload();
        String targetKey = resolveParamsAndHeaders(objectKey, message);
        String targetPath = String.format("%s/%s", bucketOps.bucketName(), targetKey);
        String actionDescription = String.format("PutObject byte[%d] -> %s in %s",
                sourceBytes.length, targetPath, bucketOps.region());
        log.debug("attempting {}", actionDescription);
        return bucketOps.putObject(targetKey, sourceBytes)
                .whenComplete((response, throwable) -> onActionComplete(actionDescription,
                        response.sdkHttpResponse(), throwable))
                .thenAccept(response -> namesProcessed.add(targetPath));
    }

    private CompletableFuture<Void> uploadFiles(final Message message,
            final List<String> namesProcessed) {
        List<String> sourceFileNames = getSources(message, getFileNameFromMessage, fileName);
        if (sourceFileNames.size() == 0) {
            throw new IllegalArgumentException(
                    "unable to get source file name(s) from message or setting");
        }

        @SuppressWarnings("rawtypes")
        CompletableFuture[] futures = (CompletableFuture[]) sourceFileNames.stream()
                .map(fn -> doPutObject(fn, message, namesProcessed))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }

    private CompletableFuture<Void> doPutObject(final String sourceFileName, final Message message,
            final List<String> namesProcessed) {
        File sourceFile = new File(sourceFileName);
        String targetKey = objectKey != null ? resolveParamsAndHeaders(objectKey, message)
                : sourceFile.getName();
        String targetPath = String.format("%s/%s", bucketOps.bucketName(), targetKey);
        String actionDescription = String.format("PutObject %s -> %s in %s", sourceFile, targetPath,
                bucketOps.region());
        log.debug("attempting {}", actionDescription);
        return bucketOps.putObject(targetKey, sourceFile)
                .whenComplete((response, throwable) -> onActionComplete(actionDescription,
                        response.sdkHttpResponse(), throwable))
                .thenAccept(response -> {
                    namesProcessed.add(targetPath);

                    /* only delete successfully processed files */
                    try {
                        Files.delete(sourceFile.toPath());
                        log.info("deleted {}", sourceFile);
                    } catch (IOException ex) {
                        log.warn("failed to delete {}: {}", sourceFile, ex.toString());
                    }
                });
    }

    private CompletableFuture<Void> downloadObjects(final Message message,
            final List<String> namesProcessed) {
        List<String> sourceObjectKeys = getSources(message, getObjectKeyFromMessage, objectKey);
        if (sourceObjectKeys.size() == 0) {
            throw new IllegalArgumentException(
                    "unable to get source object key(s) from message or setting");
        }

        @SuppressWarnings("rawtypes")
        CompletableFuture[] futures = (CompletableFuture[]) sourceObjectKeys.stream()
                .map(k -> doGetObject(k, message, namesProcessed))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }

    private CompletableFuture<Void> doGetObject(final String sourceKey, final Message message,
            final List<String> namesProcessed) {
        String sourcePath = String.format("%s/%s", bucketOps.bucketName(), sourceKey);
        File targetFile;
        try {
            /* XXX: if fileName is static, it will be overwritten N times */
            targetFile = fileName != null ? new File(resolveParamsAndHeaders(fileName, message))
                    : Files.createTempFile(getClass().getSimpleName() + "-download-", null)
                            .toFile();
        } catch (IOException ex) {
            throw new UncheckedIOException("failed to reference target file", ex);
        }
        String actionDescription = String.format("GetObject %s in %s -> %s", sourcePath,
                bucketOps.region(), targetFile);
        log.debug("attempting {}", actionDescription);
        return bucketOps.getObject(sourceKey, targetFile)
                .whenComplete((response, throwable) -> onActionComplete(actionDescription,
                        response.sdkHttpResponse(), throwable))
                .thenAccept(response -> namesProcessed.add(targetFile.toString()));
    }

    private CompletableFuture<Void> deleteObjects(final Message message,
            final List<String> namesProcessed) {
        List<String> sourceObjectKeys = getSources(message, getObjectKeyFromMessage, objectKey);
        if (sourceObjectKeys.size() == 0) {
            throw new IllegalArgumentException(
                    "unable to get source file name(s) from message or setting");
        }

        @SuppressWarnings("rawtypes")
        CompletableFuture[] futures = (CompletableFuture[]) sourceObjectKeys.stream()
                .map(k -> doDeleteObject(k, message, namesProcessed))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }

    private CompletableFuture<Void> doDeleteObject(final String sourceKey, final Message message,
            final List<String> namesProcessed) {
        String sourcePath = String.format("%s/%s", bucketOps.bucketName(), sourceKey);
        String actionDescription = String.format("DeleteObject %s in %s", sourcePath,
                bucketOps.region());
        log.debug("attempting {}", actionDescription);
        return bucketOps.deleteObject(sourceKey)
                .whenComplete((response, throwable) -> onActionComplete(actionDescription,
                        response.sdkHttpResponse(), throwable))
                .thenAccept(response -> namesProcessed.add(sourcePath));
    }

    private List<String> getSources(final Message message, final boolean fromMessage,
            final String singleName) {
        if (fromMessage && message instanceof TextMessage) {
            return ((TextMessage) message).getPayload();
        } else if (singleName != null) {
            return Arrays.asList(singleName);
        } else {
            return Collections.emptyList();
        }
    }

    private void onActionComplete(final String actionDescription,
            final SdkHttpResponse sdkHttpResponse, final Throwable throwable) {
        if (sdkHttpResponse != null) {
            if (log.isInfoEnabled()) {
                log.info("{}: HTTP {} {}", actionDescription, sdkHttpResponse.statusCode(),
                        sdkHttpResponse.statusText().orElse("(reason not provided)"));
            }
        } else {
            log.error("{}: {}", actionDescription, throwable.toString());
        }
    }

    @Override
    public void stop() {
        action = Action.Upload;
        runWhen = PER_UNIT_OF_WORK;

        fileName = null;
        getFileNameFromMessage = false;

        objectKey = null;
        getObjectKeyFromMessage = false;

        bucketOps = null;

        super.stop();
    }
}
