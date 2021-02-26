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

        public final static String TYPE = "AWS S3";
        
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

    private String regionId;

    private Region region;

    private String bucketName;

    private boolean clientSideCrypto;

    private String listFilesDelimiter = S3Directory.Settings.DEFAULT_LIST_FILES_DELIMITER;

    private int transferWindowSize = S3Directory.Settings.DEFAULT_TRANSFER_WINDOW_SIZE;

    private String restrictModeName;

    private String cmkSpec;

    private boolean useRegionAsCryptoDefault;

    private String defaultRegion;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T reference() {
        clientReference.set(S3AsyncClient.builder().region(region)
                .credentialsProvider(credentialsProvider).build());

        if (clientSideCrypto) {
            cseReference.set(createClientSideCrypto());
        }
        
        return (T) new S3Directory(getResource(), region, bucketName, clientReference.get(),
                cseReference.get(), listFilesDelimiter, transferWindowSize);
    }

    @Override
    protected void start(final TypedProperties properties) {
        super.start(properties);
        retrieveSettings(properties);
        retrieveClientSideCryptoSettings(properties);
    }

    private void retrieveSettings(final TypedProperties properties) {
        regionId = properties.get(S3.Settings.REGION);
        if (regionId != null && !regionId.isEmpty()) {
            region = (regionId != null) ? Region.of(regionId) : null;
        }

        bucketName = properties.get(S3.Settings.BUCKET_NAME);
        clientSideCrypto = properties.is(ClientSideCrypto.Settings.ENABLED, false);

        listFilesDelimiter = properties.get(S3Directory.Settings.LIST_FILES_DELIMITER,
                S3Directory.Settings.DEFAULT_LIST_FILES_DELIMITER);
        transferWindowSize = properties.getInt(S3Directory.Settings.TRANSFER_WINDOW_SIZE,
                S3Directory.Settings.DEFAULT_TRANSFER_WINDOW_SIZE);
    }
    
    private void retrieveClientSideCryptoSettings(final TypedProperties properties) {
        String defaultRestrictModeName = ClientSideCrypto.RestrictMode.ENCRYPT_DECRYPT.name();
        restrictModeName = properties.get(ClientSideCrypto.Settings.RESTRICT_MODE, defaultRestrictModeName);
        cmkSpec = properties.get(ClientSideCrypto.Settings.KMS_CMK_SPEC);
        useRegionAsCryptoDefault = properties.is(S3.Settings.USE_REGION_AS_CRYPTO_DEFAULT, false);
        defaultRegion = properties.get(ClientSideCrypto.Settings.DEFAULT_REGION);
    }

    private ClientSideCrypto createClientSideCrypto() {
        ClientSideCrypto.RestrictMode restrictMode = ClientSideCrypto.RestrictMode.valueOf(restrictModeName);
        String regionToUse = useRegionAsCryptoDefault ? regionId : defaultRegion;
        return ClientSideCrypto.forRestrictMode(restrictMode, cmkSpec, regionToUse);
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
