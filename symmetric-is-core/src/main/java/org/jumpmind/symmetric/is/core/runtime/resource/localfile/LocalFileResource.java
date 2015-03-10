package org.jumpmind.symmetric.is.core.runtime.resource.localfile;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;
import org.jumpmind.symmetric.is.core.runtime.resource.IResource;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceCategory;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceDefinition;

@ResourceDefinition(typeName=LocalFileResource.TYPE, resourceCategory=ResourceCategory.STREAMABLE)
public class LocalFileResource extends AbstractRuntimeObject implements
		IResource {

	public static final String TYPE = "Local File System";

	@SettingDefinition(order = 0, required = true, type = Type.STRING, label = "Path")
	public final static String LOCALFILE_PATH = "localfile.path";

	@SettingDefinition(type = Type.BOOLEAN, order = 20, required = true, provided = true, defaultValue = "false", label = "Must Exist")
	public static final String LOCALFILE_MUST_EXIST = "localfile.must.exist";

	IStreamableResource streamableResource;

	@Override
	public void start(Resource resource) {
		TypedProperties properties = resource.toTypedProperties(this, false);
		streamableResource = new FileStreamableResource(resource,
				properties.getProperty(LOCALFILE_PATH), 
				properties.is(LOCALFILE_MUST_EXIST));
	}

	@Override
	public void stop() {
		streamableResource.close();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T reference() {
		// TODO think about renaming reference to resource or something more meaningful
		return (T) streamableResource;
	}

}
