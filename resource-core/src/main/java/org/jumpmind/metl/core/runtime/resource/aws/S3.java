package org.jumpmind.metl.core.runtime.resource.aws;

import java.util.concurrent.atomic.AtomicReference;

import org.jumpmind.metl.core.runtime.resource.AbstractResourceRuntime;
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

    private final AtomicReference<S3AsyncClient> clientReference = new AtomicReference<>();

    private final AtomicReference<ClientSideCrypto> cseReference = new AtomicReference<>();

    private final AwsCredentialsProvider credentialsProvider = AwsCredentialsProviderChain
            .of(DefaultCredentialsProvider.builder().build());

    private Region region;

    private String bucketName;

    private String listFilesDelimiter = S3Directory.Settings.DEFAULT_LIST_FILES_DELIMITER;

    private int transferWindowSize = S3Directory.Settings.DEFAULT_TRANSFER_WINDOW_SIZE;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T reference() {
        return (T) new S3Directory(getResource(), region, bucketName, clientReference.get(),
                cseReference.get(), listFilesDelimiter, transferWindowSize);
    }

    @Override
    protected void start(final TypedProperties properties) {
        super.start(properties);

        String regionId = properties.get(S3.Settings.REGION);
        region = (regionId != null) ? Region.of(regionId) : null;

        bucketName = properties.get(S3.Settings.BUCKET_NAME);

        if (regionId != null && !regionId.isEmpty()) {
        	clientReference.set(S3AsyncClient.builder().region(region)
                .credentialsProvider(credentialsProvider).build());
        }
        if (properties.is(ClientSideCrypto.Settings.ENABLED, false)) {
            cseReference.set(createClientSideCrypto(properties, regionId));
        }

        listFilesDelimiter = properties.get(S3Directory.Settings.LIST_FILES_DELIMITER,
                S3Directory.Settings.DEFAULT_LIST_FILES_DELIMITER);
        transferWindowSize = properties.getInt(S3Directory.Settings.TRANSFER_WINDOW_SIZE,
                S3Directory.Settings.DEFAULT_TRANSFER_WINDOW_SIZE);
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
        region = null;
        bucketName = null;
        listFilesDelimiter = S3Directory.Settings.DEFAULT_LIST_FILES_DELIMITER;
        transferWindowSize = S3Directory.Settings.DEFAULT_TRANSFER_WINDOW_SIZE;

        cseReference.set(null);

        S3AsyncClient client = clientReference.getAndSet(null);
        if (client != null) {
            client.close();
        }

        super.stop();
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
            return true;
        } catch (Exception ex) {
            log.warn("test was unsuccessful: %s", ex.toString());
            return false;
        } finally {
            stop();
        }
    }
}
