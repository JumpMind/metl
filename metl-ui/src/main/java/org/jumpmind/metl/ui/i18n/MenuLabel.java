package org.jumpmind.metl.ui.i18n;

public enum MenuLabel {
	  Design, Release, Deploy, Manage, Explore, Admin, Help,Logout;
	
	public String getMenuLabel() {
		return MessageSource.message("category."+this.name().toLowerCase());
	}
	
}
