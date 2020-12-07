package org.jumpmind.metl.core.runtime.resource;

import java.util.Properties;

import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.properties.TypedProperties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SQSQueue extends AbstractResourceRuntime {
	
	public final static String TYPE = "SQSQueue";

	public static final String SETTING_ACCESS_KEY = "access.key";

	public static final String SETTING_SECRET_ACCESS_KEY = "secret.access.key";
	
	public static final String SETTING_REGION = "region";

	Properties properties = new Properties();
	
    SqsClient sqsClient;
    
	@SuppressWarnings("unchecked")
	@Override
	public <T> T reference() {
		return (T) sqsClient;
	}

	public void start(Resource resource, TypedProperties resourceRuntimeSettings) {
		if (properties.get(SETTING_ACCESS_KEY) != null &&
				properties.get(SETTING_SECRET_ACCESS_KEY) != null)  {
			Thread.currentThread().setContextClassLoader(null);
			sqsClient = createSqsClient(resourceRuntimeSettings);
		}
	}

	private SqsClient createSqsClient(TypedProperties settings) {
		AwsCredentials credentials = AwsBasicCredentials.create(settings.get(SETTING_ACCESS_KEY),
				settings.get(SETTING_SECRET_ACCESS_KEY));
		return SqsClient.builder().region(Region.of(settings.get(SETTING_REGION)))
				.credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
	}	

	@Override
	public void stop() {
		sqsClient.close();
	}

}
