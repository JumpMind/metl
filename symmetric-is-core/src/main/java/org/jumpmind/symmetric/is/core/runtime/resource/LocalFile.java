package org.jumpmind.symmetric.is.core.runtime.resource;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.ResourceCategory;

@ResourceDefinition(typeName=LocalFile.TYPE, resourceCategory=ResourceCategory.STREAMABLE)
public class LocalFile extends AbstractResourceRuntime {

	public static final String TYPE = "Local File System";

	@SettingDefinition(order = 0, required = true, type = Type.TEXT, label = "Path")
	public final static String LOCALFILE_PATH = "localfile.path";

	@SettingDefinition(type = Type.BOOLEAN, order = 20, required = true, provided = true, defaultValue = "false", label = "Must Exist")
	public static final String LOCALFILE_MUST_EXIST = "localfile.must.exist";

	IStreamable streamableResource;

	@Override
	protected void start(TypedProperties properties) {
		streamableResource = new LocalFileStreamable(resource,
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