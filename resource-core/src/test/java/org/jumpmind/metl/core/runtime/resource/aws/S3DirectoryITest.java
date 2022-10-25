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
package org.jumpmind.metl.core.runtime.resource.aws;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jumpmind.metl.core.runtime.resource.IS3BucketOperations;
import org.junit.Test;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class S3DirectoryITest extends S3ITestSupport {
    private S3Directory s3Directory() {
        return new S3Directory(null, null, TEST_BUCKET_NAME,
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
        try (IS3BucketOperations bucketOps = s3Directory()) {
            PutObjectResponse response = bucketOps.putObject(key, testFile()).get(3,
                    TimeUnit.SECONDS);

            assertSdkHttpSuccess(response.sdkHttpResponse());
        }
    }

    @Test
    public void canHeadObject() throws InterruptedException, ExecutionException, TimeoutException {
        assumeTrue("AWS credentials are not present", awsCredentialsArePresent());

        String key = "can-head-object/lorem-ipsum";
        try (IS3BucketOperations bucketOps = s3Directory()) {
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
        try (IS3BucketOperations bucketOps = s3Directory()) {
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
        try (IS3BucketOperations bucketOps = s3Directory()) {
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
        try (IS3BucketOperations bucketOps = s3Directory()) {
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
        try (IS3BucketOperations bucketOps = s3Directory()) {
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
        try (IS3BucketOperations bucketOps = s3Directory()) {
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
