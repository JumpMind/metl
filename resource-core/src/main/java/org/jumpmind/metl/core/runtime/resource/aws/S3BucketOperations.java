package org.jumpmind.metl.core.runtime.resource.aws;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.jumpmind.metl.core.runtime.resource.IDirectory;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public interface S3BucketOperations extends IDirectory {
    /* S3 PutObject */

    /* use the future returned here to wait for the response to be ready */
    CompletableFuture<PutObjectResponse> putObject(String key, InputStream objectSource);

    /* use the future returned here to wait for the response to be ready */
    CompletableFuture<PutObjectResponse> putObject(String key, InputStream objectSource,
            Map<String, String> suppliedAad);

    /*
     * use the future returned here to process the ready response (either
     * success or failure)
     */
    CompletableFuture<PutObjectResponse> putObject(String key, InputStream objectSource,
            BiConsumer<? super PutObjectResponse, ? super Throwable> whenCompleteAction);

    /*
     * use the future returned here to process the ready response (either
     * success or failure)
     */
    CompletableFuture<PutObjectResponse> putObject(String key, InputStream objectSource,
            BiConsumer<? super PutObjectResponse, ? super Throwable> whenCompleteAction,
            Map<String, String> suppliedAad);

    /* S3 GetObject */

    /* use the future returned here to wait for the response to be ready */
    CompletableFuture<ResponseBytes<GetObjectResponse>> getObject(String key);

    /* use the future returned here to wait for the response to be ready */
    CompletableFuture<ResponseBytes<GetObjectResponse>> getObject(String key,
            Map<String, String> suppliedAad);

    /*
     * use the future returned here to process the ready response (either
     * success or failure)
     */
    CompletableFuture<GetObjectResponse> getObject(String key,
            BiConsumer<? super GetObjectResponse, ? super Throwable> whenCompleteAction);

    /*
     * use the future returned here to process the ready response (either
     * success or failure)
     */
    CompletableFuture<GetObjectResponse> getObject(String key,
            BiConsumer<? super GetObjectResponse, ? super Throwable> whenCompleteAction,
            Map<String, String> suppliedAad);
}
