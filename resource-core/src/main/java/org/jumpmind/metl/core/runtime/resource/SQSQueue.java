package org.jumpmind.metl.core.runtime.resource;

import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.properties.TypedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SQSQueue extends AbstractResourceRuntime {

    public final static String TYPE = "SQSQueue";

    public static final String SETTING_CREDENTIAL_TYPE = "credential.type";

    public static final String SETTING_ACCESS_KEY = "access.key";

    public static final String SETTING_SECRET_ACCESS_KEY = "secret.access.key";

    public static final String SETTING_REGION = "region";

    private String credentialType;
    private String accessKey;
    private String secretAccessKey;
    private String region;

    SqsClient sqsClient;

    protected static final Logger log = LoggerFactory.getLogger(SMBDirectory.class);

    @SuppressWarnings("unchecked")
    @Override
    public <T> T reference() {
        if (credentialType.equals("AWS SDK") && (accessKey == null || secretAccessKey == null)) {
            throw new MisconfiguredException("Access Key and Secret Access Key are required for Credential Type 'AWS SDK'");
        }

        if (credentialType.equals("System") && (accessKey != null || secretAccessKey != null)) {
            log.warn("AccessKey and SecretAccessKey provided will be ignored for Credential Type 'System'");
        }

        return (T) createSqsClient();
    }

    @Override
    public void start(TypedProperties resourceRuntimeSettings) {
        credentialType = resourceRuntimeSettings.get(SETTING_CREDENTIAL_TYPE);
        accessKey = resourceRuntimeSettings.get(SETTING_ACCESS_KEY);
        secretAccessKey = resourceRuntimeSettings.get(SETTING_SECRET_ACCESS_KEY);
        region = resourceRuntimeSettings.get(SETTING_REGION);

        Thread.currentThread().setContextClassLoader(null);
    }

    private SqsClient createSqsClient() {
        try {
            if (credentialType.equals("System")) {
               return SqsClient.builder()
                       .region(Region.of(region))
                       .build();
            }

            AwsCredentials credentials = AwsBasicCredentials.create(accessKey, secretAccessKey);
            AwsCredentialsProvider provider = StaticCredentialsProvider.create(credentials);

            return SqsClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(provider)
                    .build();
        } catch (Exception e) {
           throw new RuntimeException("Could not create SQS Client: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
    	if (sqsClient != null) {
    		sqsClient.close();
    	}
    }
}
