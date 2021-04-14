package org.jumpmind.metl.core.runtime.resource.aws;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

public class S3ITestSupport {
    protected static final String TEST_BUCKET_NAME = S3ITestSupport.class.getSimpleName()
            .toLowerCase() + "-" + UUID.randomUUID();

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
        CreateBucketRequest request = CreateBucketRequest.builder().bucket(TEST_BUCKET_NAME)
                .build();
        CreateBucketResponse response = s3.createBucket(request);

        if (!response.sdkHttpResponse().isSuccessful()) {
            throw new IllegalStateException(String.format(
                    "test bucket '%s' not created: HTTP %d %s", TEST_BUCKET_NAME,
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

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(TEST_BUCKET_NAME)
                .build();
        ListObjectsV2Response listResponse = s3.listObjectsV2(listRequest);

        delete_all_objects: while (true) {
            listResponse.contents().stream().forEach(o -> {
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(TEST_BUCKET_NAME).key(o.key()).build();
                s3.deleteObject(deleteObjectRequest);
            });

            if (listResponse.isTruncated()) {
                listRequest = ListObjectsV2Request.builder().bucket(TEST_BUCKET_NAME)
                        .continuationToken(listResponse.nextContinuationToken()).build();
                listResponse = s3.listObjectsV2(listRequest);
            } else {
                break delete_all_objects;
            }
        }

        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                .bucket(TEST_BUCKET_NAME).build();
        DeleteBucketResponse deleteBucketResponse = s3.deleteBucket(deleteBucketRequest);
        if (!deleteBucketResponse.sdkHttpResponse().isSuccessful()) {
            System.err.printf("test bucket '%s' not deleted: HTTP %d %s", TEST_BUCKET_NAME,
                    deleteBucketResponse.sdkHttpResponse().statusCode(), deleteBucketResponse
                            .sdkHttpResponse().statusText().orElse("(reason not provided)"));
        }
    }

    protected static boolean awsCredentialsArePresent() {
        return System.getenv(SdkSystemSetting.AWS_ACCESS_KEY_ID.environmentVariable()) != null
                || System.getProperty(SdkSystemSetting.AWS_ACCESS_KEY_ID.property()) != null
                || Files.isReadable(Paths.get(System.getProperty("user.home"), ".aws/credentials"));
    }

    protected static AwsCredentialsProvider awsCredentialsProvider() {
        return AwsCredentialsProviderChain.of(DefaultCredentialsProvider.builder().build());
    }

    protected static File testFile() {
        return testFile("lorem-ipsum.txt");
    }

    protected static File testFile(final String fileName) {
        // allow this to work from EITHER file system or test JAR
        Path p;
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            p = Files.createTempFile(S3ITestSupport.class.getSimpleName(), null);
            Files.copy(is, p, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        File tmp = p.toFile();
        tmp.deleteOnExit();
        return tmp;
    }

    protected static void assertSdkHttpSuccess(final SdkHttpResponse response) {
        assertTrue(
                String.format("%d %s", response.statusCode(),
                        response.statusText().orElse("(reason not provided)")),
                response.isSuccessful());
    }
}
