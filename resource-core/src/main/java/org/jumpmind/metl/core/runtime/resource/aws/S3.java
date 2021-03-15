package org.jumpmind.metl.core.runtime.resource.aws;

import static java.util.Objects.requireNonNull;

import org.jumpmind.metl.core.runtime.resource.AbstractResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.IS3BucketOperations;
import org.jumpmind.properties.TypedProperties;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class S3 extends AbstractResourceRuntime {
    public static final class Settings {
        /** Name of the setting that identifies the AWS S3 region. */
        public static final String REGION = "aws.s3.region";

        public static final String USE_REGION_AS_CRYPTO_DEFAULT = "aws.s3.region.as.crypto.default";

        public static final String BUCKET_NAME = "aws.s3.bucket.name";

        Settings() {
            throw new UnsupportedOperationException("do not instantiate");
        }
    }

    private IS3BucketOperations bucketOps;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T reference() {
        return (T) bucketOps;
    }

    @Override
    protected void start(final TypedProperties properties) {
        String regionId = properties.get(S3.Settings.REGION);
        Region region = (regionId != null) ? Region.of(regionId) : null;

        String bucketName = properties.get(S3.Settings.BUCKET_NAME);

        AwsCredentialsProvider credentialsProvider = AwsCredentialsProviderChain
                .of(DefaultCredentialsProvider.builder().build());

        S3AsyncClient client = S3AsyncClient.builder().region(region)
                .credentialsProvider(credentialsProvider).build();

        ClientSideCrypto cse = null;
        if (properties.is(ClientSideCrypto.Settings.ENABLED, false)) {
            cse = createClientSideCrypto(properties, regionId);
        }

        String listFilesDelimiter = properties.get(S3Directory.Settings.LIST_FILES_DELIMITER,
                S3Directory.Settings.DEFAULT_LIST_FILES_DELIMITER);
        int transferWindowSize = properties.getInt(S3Directory.Settings.TRANSFER_WINDOW_SIZE,
                S3Directory.Settings.DEFAULT_TRANSFER_WINDOW_SIZE);

        bucketOps = new S3Directory(getResource(), region, bucketName, client, cse,
                listFilesDelimiter, transferWindowSize);
    }

    private ClientSideCrypto createClientSideCrypto(final TypedProperties properties,
            final String s3Region) {
        String defaultRestrictModeName = ClientSideCrypto.RestrictMode.ENCRYPT_DECRYPT.name();
        String restrictModeName = properties.get(ClientSideCrypto.Settings.RESTRICT_MODE,
                defaultRestrictModeName);
        ClientSideCrypto.RestrictMode restrictMode = ClientSideCrypto.RestrictMode
                .valueOf(restrictModeName);

        String cmkSpec = properties.get(ClientSideCrypto.Settings.KMS_CMK_SPEC);

        String defaultRegion = properties.is(S3.Settings.USE_REGION_AS_CRYPTO_DEFAULT, false)
                ? s3Region
                : properties.get(ClientSideCrypto.Settings.DEFAULT_REGION);

        return ClientSideCrypto.forRestrictMode(restrictMode, cmkSpec, defaultRegion);
    }

    @Override
    public void stop() {
        bucketOps.close();
    }

    @Override
    public boolean isTestSupported() {
        return true;
    }

    /*
     * TODO: when is this called? if the properties aren't available yet then
     * this will need to change
     */
    @Override
    public boolean test() {
        try {
            start(getResourceRuntimeSettings());
            requireNonNull((IS3BucketOperations) reference(), "reference");
            return true;
        } catch (Exception ex) {
            log.warn("test was unsuccessful: %s", ex.toString());
            return false;
        } finally {
            stop();
        }
    }
}
