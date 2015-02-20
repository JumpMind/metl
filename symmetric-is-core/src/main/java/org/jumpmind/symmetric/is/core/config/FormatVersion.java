package org.jumpmind.symmetric.is.core.config;


public class FormatVersion extends AbstractObjectWithSettings {

	private static final long serialVersionUID = 1L;
	
	String versionName;
	String formatId;
	String modelVersionId;
	
	public FormatVersion(String id) {
		this.id = id;
	}
	
	@Override
	protected Setting createSettingData() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
