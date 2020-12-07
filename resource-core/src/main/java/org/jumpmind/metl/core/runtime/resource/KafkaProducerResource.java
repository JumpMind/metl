package org.jumpmind.metl.core.runtime.resource;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.properties.TypedProperties;

import java.util.Properties;

public class KafkaProducerResource extends AbstractResourceRuntime {

	public final static String TYPE = "KafkaProducer";

	public static final String SETTING_BOOTSTRAP_SERVERS = "bootstrap.servers";

	public static final String SETTING_KEY_SERIALIZER = "key.serializer";

	public static final String SETTING_VALUE_SERIALIZER = "value.serializer";

	public static final String SETTING_ACKS = "acks";

	public static final String SETTING_BUFFER_MEMORY = "buffer.memory";

	public static final String SETTING_COMPRESSION_TYPE = "compression.type";

	public static final String SETTING_RETRIES = "retries";

	public static final String SETTING_BATCH_SIZE = "batch.size";

	public static final String SETTING_CLIENT_ID = "client.id";

	public static final String SETTING_DELIVERY_TIMEOUT_MS = "delivery.timeout.ms";

	public static final String SETTING_LINGER_MS = "linger.ms";

	public static final String SETTING_MAX_REQUEST_SIZE = "max.request.size";

	public static final String SETTING_REQUST_TIMEOUT_MS = "request.timeout.ms";

	Properties properties = new Properties();
	@SuppressWarnings("rawtypes")
	Producer producer;

	@SuppressWarnings("unchecked")
	@Override
	public <T> T reference() {
		return (T) producer;
	}

	@SuppressWarnings("rawtypes")
	public void start(Resource resource, TypedProperties resourceRuntimeSettings) {

		setProperties(resourceRuntimeSettings);
		if (properties.get(SETTING_BOOTSTRAP_SERVERS) != null) {
			Thread.currentThread().setContextClassLoader(null);
			producer = new KafkaProducer(properties);
		}
	}

	private void setProperties(TypedProperties runtimeProperties) {
		for (Object obj : runtimeProperties.keySet()) {
			if (runtimeProperties.get(obj) != null) {
				String stringKeyValue = (String) obj;
				Object value = runtimeProperties.get(obj);
				if (stringKeyValue.equalsIgnoreCase(SETTING_KEY_SERIALIZER)
						|| stringKeyValue.equalsIgnoreCase(SETTING_VALUE_SERIALIZER)) {
					value = getSerializerClass((String) value);
				} else if (stringKeyValue.equalsIgnoreCase("SETTING_BUFFER_MEMORY") ||
						stringKeyValue.equalsIgnoreCase("SETTING_LINGER_MS")) {
					value = new Long((Integer)value);
				}
				properties.put(stringKeyValue, value);
			}
		}
	}

	@Override
	public void stop() {
		producer.close();
	}

	private String getSerializerClass(String serializerCode) {
		if (serializerCode.equalsIgnoreCase("StringSerializer")) {
			return "org.apache.kafka.common.serialization.StringSerializer";
		} else if (serializerCode.equalsIgnoreCase("DoubleSerializer")) {
			return "org.apache.kafka.common.serialization.DoubleSerializer";
		} else if (serializerCode.equalsIgnoreCase("FloatSerializer")) {
			return "org.apache.kafka.common.serialization.FloatSerializer";
		} else if (serializerCode.equalsIgnoreCase("LongSerializer")) {
			return "org.apache.kafka.common.serialization.LongSerializer";
		} else if (serializerCode.equalsIgnoreCase("ShortSerializer")) {
			return "org.apache.kafka.common.serialization.ShortSerializer";
		} else {
			return null;
		}
	}
	
	@Override
    public boolean isTestSupported() {
    	return false;
    }
	
	public String getKeySerializer() {
		return getSerializerClass((String)properties.getProperty(SETTING_KEY_SERIALIZER));
	}
	
	public String getValueSerializer() {
		return getSerializerClass((String)properties.getProperty(SETTING_VALUE_SERIALIZER));
	}
}
