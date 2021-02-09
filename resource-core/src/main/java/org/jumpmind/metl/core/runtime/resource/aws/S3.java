package org.jumpmind.metl.core.runtime.resource.aws;

import java.util.concurrent.atomic.AtomicReference;

import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.resource.AbstractResourceRuntime;
import org.jumpmind.properties.TypedProperties;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

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

    /**
     * Name of the setting that identifies whether or not AWS authentication
     * credentials may be updated asynchronously.
     */
    public static final String ASYNC_CREDENTIALS_UPDATE_V2 = "aws.s3.async.credentials.update.enabled";

    private final AtomicReference<S3Directory> bucketReference = new AtomicReference<>();

    @SuppressWarnings("unchecked")
    @Override
    public <T> T reference() {
        return (T) bucketReference.get();
    }

    @Override
    protected void start(final TypedProperties properties) {
        super.start(properties);

        bucketReference.set(createS3Directory(properties));
    }

    private S3Directory createS3Directory(final TypedProperties properties) {
        String regionId = properties.get(S3.Settings.REGION);
        Region region = (regionId != null) ? Region.of(regionId) : null;

        String bucketName = properties.get(S3.Settings.BUCKET_NAME, "").trim();
        if (bucketName == null || bucketName.isEmpty()) {
            throw new MisconfiguredException(S3.Settings.BUCKET_NAME);
        }

        ClientSideCrypto crypto = properties.is(ClientSideCrypto.Settings.ENABLED, false)
                ? createClientSideCrypto(properties, regionId)
                : null;

        return new S3Directory(getResource(), region, bucketName, crypto,
                AwsCredentialsProviderChain.of(DefaultCredentialsProvider.builder().build()));
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
        S3Directory s3Directory = bucketReference.getAndSet(null);
        if (s3Directory != null) {
            s3Directory.close();
        }

        super.stop();
    }

    @Override
    public boolean isTestSupported() {
        return true;
    }

    @Override
    public boolean test() {
        try {
            /*
             * XXX: when is this called? if the properties aren't available yet
             * then this will need to change
             */
            createS3Directory(getResourceRuntimeSettings());
            return true;
        } catch (Exception ex) {
            log.warn("test was unsuccessful: %s", ex.toString());
            return false;
        }
    }
}
