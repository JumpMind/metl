package org.jumpmind.symmetric.is.core.config;


public class FormatVersion extends AbstractObjectWithSettings {

	private static final long serialVersionUID = 1L;
	
	Format format;
	ModelVersion modelVersion;
	String versionName;
	
	public FormatVersion(String id) {
		this.id = id;
	}
	
	public Format getFormat() {
		return this.format;
	}
	
	public void setFormat(Format format) {
		this.format = format;
	}
	
	public String getFormatId() {
		return format != null ? format.getId() : null;
	}
	
	public void setFormatId(String formatId) {
		if (formatId != null) {
			format = new Format(formatId);
		} else {
			format = null;
		}
	}
	
	public ModelVersion getModelVersion() {
		return modelVersion;
	}

	public void setModelVersion(ModelVersion modelVersion) {
		this.modelVersion = modelVersion;
	}

	public String getModelVersionId() {
		return modelVersion != null ? modelVersion.getId() : null;
	}
	
	public void setModelVersionId(String modelVersionId) {
		if (modelVersionId != null) {
			modelVersion = new ModelVersion(modelVersionId);
		}
 	}
	
	public String getVersionName() {
		return versionName;
	}

	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	protected Setting createSettingData() {
		return new FormatVersionSetting(id);
	}
	
	public void setName(String name) {
	}

	public String getName() {
		return this.format.getName();
	}

}
