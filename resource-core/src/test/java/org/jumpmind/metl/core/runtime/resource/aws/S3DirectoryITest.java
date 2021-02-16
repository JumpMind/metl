package org.jumpmind.metl.core.runtime.resource.aws;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class S3DirectoryITest {
    private static final String BUCKET_NAME = S3DirectoryITest.class.getSimpleName().toLowerCase()
            + "-" + UUID.randomUUID();

    @BeforeClass
    public static void createTestBucket() {
        /*
         * assumeTrue does not work here (this is not a test); but we can't
         * continue if there are no credentials (otherwise it would be pointless
         * to use assumeTrue elsewhere)
         */
        if (!awsCredentialsArePresent()) {
            throw new IllegalStateException("AWS credentials are not present");
        }

        S3Client s3 = S3Client.builder().credentialsProvider(awsCredentialsProvider()).build();
        CreateBucketRequest request = CreateBucketRequest.builder().bucket(BUCKET_NAME).build();
        CreateBucketResponse response = s3.createBucket(request);

        if (!response.sdkHttpResponse().isSuccessful()) {
            throw new IllegalStateException(String.format(
                    "test bucket '%s' not created: HTTP %d %s", BUCKET_NAME,
                    response.sdkHttpResponse().statusCode(),
                    response.sdkHttpResponse().statusText().orElse("(reason not provided)")));
        }
    }

    @AfterClass
    public static void deleteTestBucket() {
        /*
         * assumeTrue does not work here (this is not a test); but we can't
         * continue if there are no credentials (otherwise it would be pointless
         * to use assumeTrue elsewhere)
         */
        if (!awsCredentialsArePresent()) {
            throw new IllegalStateException("AWS credentials are not present");
        }

        S3Client s3 = S3Client.builder().credentialsProvider(awsCredentialsProvider()).build();

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(BUCKET_NAME)
                .build();
        ListObjectsV2Response listResponse = s3.listObjectsV2(listRequest);

        delete_all_objects: while (true) {
            listResponse.contents().stream().forEach(o -> {
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(BUCKET_NAME).key(o.key()).build();
                s3.deleteObject(deleteObjectRequest);
            });

            if (listResponse.isTruncated()) {
                listRequest = ListObjectsV2Request.builder().bucket(BUCKET_NAME)
                        .continuationToken(listResponse.nextContinuationToken()).build();
                listResponse = s3.listObjectsV2(listRequest);
            } else {
                break delete_all_objects;
            }
        }

        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(BUCKET_NAME)
                .build();
        DeleteBucketResponse deleteBucketResponse = s3.deleteBucket(deleteBucketRequest);
        if (!deleteBucketResponse.sdkHttpResponse().isSuccessful()) {
            System.err.printf("test bucket '%s' not deleted: HTTP %d %s", BUCKET_NAME,
                    deleteBucketResponse.sdkHttpResponse().statusCode(), deleteBucketResponse
                            .sdkHttpResponse().statusText().orElse("(reason not provided)"));
        }
    }

    private static boolean awsCredentialsArePresent() {
        return System.getenv(SdkSystemSetting.AWS_ACCESS_KEY_ID.environmentVariable()) != null
                || System.getProperty(SdkSystemSetting.AWS_ACCESS_KEY_ID.property()) != null
                || Files.isReadable(Paths.get(System.getProperty("user.home"), ".aws/credentials"));
    }

    private static AwsCredentialsProvider awsCredentialsProvider() {
        return AwsCredentialsProviderChain.of(DefaultCredentialsProvider.builder().build());
    }

    private static File testFile() {
        try {
            return new File(Thread.currentThread().getContextClassLoader()
                    .getResource("lorem-ipsum.txt").toURI());
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void assertSdkHttpSuccess(final SdkHttpResponse response) {
        assertTrue(
                String.format("%d %s", response.statusCode(),
                        response.statusText().orElse("(reason not provided)")),
                response.isSuccessful());
    }

    private S3Directory s3Directory() {
        return new S3Directory(null, null, BUCKET_NAME,
                S3AsyncClient.builder().credentialsProvider(awsCredentialsProvider()).build(), null,
                S3Directory.Settings.DEFAULT_LIST_FILES_DELIMITER,
                S3Directory.Settings.DEFAULT_TRANSFER_WINDOW_SIZE);
    }

    /* S3BucketOperations tests */

    @Test
    public void canPutObjectFromFile()
            throws InterruptedException, ExecutionException, TimeoutException {
        assumeTrue("AWS credentials are not present", awsCredentialsArePresent());

        String key = "can-put-object-from-file/lorem-ipsum";
        try (S3BucketOperations bucketOps = s3Directory()) {
            PutObjectResponse response = bucketOps.putObject(key, testFile()).get(3,
                    TimeUnit.SECONDS);

            assertSdkHttpSuccess(response.sdkHttpResponse());
        }
    }

    @Test
    public void canHeadObject() throws InterruptedException, ExecutionException, TimeoutException {
        assumeTrue("AWS credentials are not present", awsCredentialsArePresent());

        String key = "can-head-object/lorem-ipsum";
        try (S3BucketOperations bucketOps = s3Directory()) {
            PutObjectResponse r = bucketOps.putObject(key, testFile()).get(3, TimeUnit.SECONDS);
            assertSdkHttpSuccess(r.sdkHttpResponse());

            HeadObjectResponse response = bucketOps.headObject(key).get(3, TimeUnit.SECONDS);

            assertSdkHttpSuccess(response.sdkHttpResponse());
        }
    }

    @Test
    public void canGetObjectBytes()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        assumeTrue("AWS credentials are not present", awsCredentialsArePresent());

        String key = "can-get-object-bytes/lorem-ipsum";
        try (S3BucketOperations bucketOps = s3Directory()) {
            PutObjectResponse r = bucketOps.putObject(key, testFile()).get(3, TimeUnit.SECONDS);
            assertSdkHttpSuccess(r.sdkHttpResponse());

            ResponseBytes<GetObjectResponse> response = bucketOps.getObject(key).get(3,
                    TimeUnit.SECONDS);

            assertSdkHttpSuccess(response.response().sdkHttpResponse());
            assertArrayEquals(Files.readAllBytes(testFile().toPath()),
                    response.asByteArrayUnsafe());
        }
    }

    @Test
    public void canGetObjectToFile()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        assumeTrue("AWS credentials are not present", awsCredentialsArePresent());

        String key = "can-get-object-to-file/lorem-ipsum";
        try (S3BucketOperations bucketOps = s3Directory()) {
            PutObjectResponse r = bucketOps.putObject(key, testFile()).get(3, TimeUnit.SECONDS);
            assertSdkHttpSuccess(r.sdkHttpResponse());

            Path tempDir = Files.createTempDirectory(getClass().getSimpleName() + "-");
            Path tempFile = Files.createTempFile(tempDir, "canGetObjectToFile-", null);
            /*
             * borrow the name, but the file itself must be deleted (S3 will NOT
             * overwrite and will instead fail); in this case we allow the
             * IOException to be thrown since if the file doesn't get deleted
             * here the S3 response WILL fail
             */
            Files.delete(tempFile);

            try {
                GetObjectResponse response = bucketOps.getObject(key, tempFile.toFile()).get(3,
                        TimeUnit.SECONDS);

                assertSdkHttpSuccess(response.sdkHttpResponse());
                assertArrayEquals(Files.readAllBytes(testFile().toPath()),
                        Files.readAllBytes(tempFile));
            } finally {
                try {
                    Files.delete(tempFile);
                    Files.delete(tempDir);
                } catch (IOException ex) {
                    System.err.printf(
                            "WARNING: could not programmatically delete tempFile '%s' and/or tempDir '%s': %s\n",
                            tempFile, tempDir, ex);
                }
            }
        }
    }

    @Test
    public void canDeleteObject()
            throws InterruptedException, ExecutionException, TimeoutException {
        assumeTrue("AWS credentials are not present", awsCredentialsArePresent());

        String key = "can-delete-object/lorem-ipsum";
        try (S3BucketOperations bucketOps = s3Directory()) {
            PutObjectResponse r = bucketOps.putObject(key, testFile()).get(3, TimeUnit.SECONDS);
            assertSdkHttpSuccess(r.sdkHttpResponse());

            DeleteObjectResponse response = bucketOps.deleteObject(key).get(3, TimeUnit.SECONDS);

            assertSdkHttpSuccess(response.sdkHttpResponse());
        }
    }

    @Test
    public void canListObjects() throws InterruptedException, ExecutionException, TimeoutException {
        assumeTrue("AWS credentials are not present", awsCredentialsArePresent());

        String key1 = "can-list-objects/lorem-ipsum-1";
        String key2 = "can-list-objects/lorem-ipsum-2";
        try (S3BucketOperations bucketOps = s3Directory()) {
            PutObjectResponse r = bucketOps.putObject(key1, testFile()).get(3, TimeUnit.SECONDS);
            assertSdkHttpSuccess(r.sdkHttpResponse());
            r = bucketOps.putObject(key2, testFile()).get(3, TimeUnit.SECONDS);
            assertSdkHttpSuccess(r.sdkHttpResponse());

            ListObjectsV2Response response = bucketOps.listObjects().get(3, TimeUnit.SECONDS);

            assertSdkHttpSuccess(response.sdkHttpResponse());
            /* test order is not guaranteed; may have other objects */
            assertTrue("expected at least key1 and key2 in contents",
                    response.contents().size() >= 2);
            assertNotNull("missing key1: " + key1, response.contents().stream()
                    .filter(o -> o.key().equals(key1)).findFirst().orElse(null));
            assertNotNull("missing key2: " + key2, response.contents().stream()
                    .filter(o -> o.key().equals(key2)).findFirst().orElse(null));
        }
    }

    @Test
    public void canListObjectsWithContinuation()
            throws InterruptedException, ExecutionException, TimeoutException {
        assumeTrue("AWS credentials are not present", awsCredentialsArePresent());

        String key1 = "can-list-objects-with-continuation/lorem-ipsum-1";
        String key2 = "can-list-objects-with-continuation/lorem-ipsum-2";
        try (S3BucketOperations bucketOps = s3Directory()) {
            PutObjectResponse r = bucketOps.putObject(key1, testFile()).get(3, TimeUnit.SECONDS);
            assertSdkHttpSuccess(r.sdkHttpResponse());
            r = bucketOps.putObject(key2, testFile()).get(3, TimeUnit.SECONDS);
            assertSdkHttpSuccess(r.sdkHttpResponse());

            ListObjectsV2Response response = bucketOps
                    .listObjects(null, "can-list-objects-with-continuation/lorem-ipsum-", 1, null)
                    .get(3, TimeUnit.SECONDS);

            assertSdkHttpSuccess(response.sdkHttpResponse());
            assertTrue("expected initial response to be truncated", response.isTruncated());
            assertEquals(1, response.contents().size());

            String keyInFirstResponse = response.contents().get(0).key();
            String expectedKeyInContinuationResponse = key1.equals(keyInFirstResponse) ? key2
                    : key1;

            response = bucketOps.listObjects(response.delimiter(), response.prefix(), 1,
                    response.nextContinuationToken()).get(3, TimeUnit.SECONDS);

            assertSdkHttpSuccess(response.sdkHttpResponse());
            assertFalse("expected continuation response to NOT be truncated",
                    response.isTruncated());
            assertEquals(1, response.contents().size());
            assertEquals(expectedKeyInContinuationResponse, response.contents().get(0).key());
        }
    }
}
