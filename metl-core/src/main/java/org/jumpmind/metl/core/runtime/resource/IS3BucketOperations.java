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
package org.jumpmind.metl.core.runtime.resource;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public interface IS3BucketOperations extends IDirectory, AutoCloseable {
    /**
     * Returns the configured AWS S3 region.
     */
    Region region();

    /**
     * Returns the configured AWS S3 bucket name.
     */
    String bucketName();

    /**
     * Uploads an object to an S3 bucket.
     * 
     * @param objectKey
     *            the S3 object key
     * @param objectSource
     *            the file representing the object to be uploaded
     * @return the future that will indicate when the {@link PutObjectResponse}
     *         is ready to be handled
     */
    CompletableFuture<PutObjectResponse> putObject(String objectKey, File objectSource);

    /**
     * Uploads an object to an S3 bucket.
     * 
     * @param objectKey
     *            the S3 object key
     * @param objectSource
     *            the object to be uploaded
     * @return the future that will indicate when the {@link PutObjectResponse}
     *         is ready to be handled
     */
    CompletableFuture<PutObjectResponse> putObject(String objectKey, byte[] objectSource);

    /**
     * Returns the metadata for an object in an S3 bucket.
     * 
     * @param objectKey
     *            the S3 object key
     * @return the future that will indicate when the {@link HeadObjectResponse}
     *         is ready to be handled
     */
    CompletableFuture<HeadObjectResponse> headObject(String objectKey);

    /**
     * Downloads an object from an S3 bucket.
     * 
     * @param objectKey
     *            the S3 object key
     * @return the future that will indicate when the {@link GetObjectResponse}
     *         is ready to be handled
     */
    CompletableFuture<ResponseBytes<GetObjectResponse>> getObject(String objectKey);

    /**
     * Downloads an object from an S3 bucket.
     * 
     * @param objectKey
     *            the S3 object key
     * @param objectSink
     *            where (to which file) the object will be downloaded
     * @return the future that will indicate when the {@link GetObjectResponse}
     *         is ready to be handled
     */
    CompletableFuture<GetObjectResponse> getObject(String objectKey, File objectSink);

    /**
     * Deletes an object from an S3 bucket.
     * 
     * @param objectKey
     *            the S3 object key
     * @return the future that will indicate when the
     *         {@link DeleteObjectResponse} is ready to be handled
     */
    CompletableFuture<DeleteObjectResponse> deleteObject(String objectKey);

    /**
     * Returns metadata for all objects contained within an S3 bucket.
     * 
     * @return the future that will indicate when the
     *         {@link ListObjectsV2Response} is ready to be handled
     */
    CompletableFuture<ListObjectsV2Response> listObjects();

    /**
     * Returns metadata for objects contained within an S3 bucket.
     * 
     * @param delimiter
     *            the S3 object key delimiter used to limit the results
     * @param prefix
     *            the S3 object key prefix used to limit the results
     * @return the future that will indicate when the
     *         {@link ListObjectsV2Response} is ready to be handled
     * 
     * @see <a href=
     *      "https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-prefixes.html">Organizing
     *      objects using prefixes</a>
     */
    CompletableFuture<ListObjectsV2Response> listObjects(String delimiter, String prefix);

    /**
     * Returns metadata for objects contained within an S3 bucket.
     * 
     * @param delimiter
     *            the S3 object key delimiter used to limit the results
     * @param prefix
     *            the S3 object key prefix used to limit the results
     * @param maxKeys
     *            the maximum number of results to return
     * @return the future that will indicate when the
     *         {@link ListObjectsV2Response} is ready to be handled
     * 
     * @see <a href=
     *      "https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-prefixes.html">Organizing
     *      objects using prefixes</a>
     */
    CompletableFuture<ListObjectsV2Response> listObjects(String delimiter, String prefix,
            Integer maxKeys);

    /**
     * Returns metadata for objects contained within an S3 bucket.
     * 
     * @param delimiter
     *            the S3 object key delimiter used to limit the results
     * @param prefix
     *            the S3 object key prefix used to limit the results
     * @param maxKeys
     *            the maximum number of results to return
     * @param continuationToken
     *            the continuation token from a prior call to one of the
     *            <code>listObjects</code> overloads
     * @return the future that will indicate when the
     *         {@link ListObjectsV2Response} is ready to be handled
     * 
     * @see <a href=
     *      "https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-prefixes.html">Organizing
     *      objects using prefixes</a>
     */
    CompletableFuture<ListObjectsV2Response> listObjects(String delimiter, String prefix,
            Integer maxKeys, String continuationToken);
}
