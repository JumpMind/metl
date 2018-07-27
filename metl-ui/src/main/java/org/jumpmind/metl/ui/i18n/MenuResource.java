package org.jumpmind.metl.ui.i18n;

import org.apache.commons.lang3.StringUtils;

public class MenuResource {

	
	
	
	public  static  String getNewProject() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.project"),
		};	
		return    StringUtils.join(newProjectArray, "|");
	}
	
	
	public  static  String getNewBranch() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.projectBranch"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}

	public  static  String getNewDependency() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.projectDependency"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getNewDesign() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.flow"),
				MessageSource.message("common.design"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getNewTest() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.flow"),
				MessageSource.message("common.test"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
//     add("File|New|Model|Hierarchical");
	public  static  String getNewHierarchical() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.model"),
				MessageSource.message("common.hierarchical"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
//     add("File|New|Model|Relational");
	public  static  String getNewRelational() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.model"),
				MessageSource.message("common.relational"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getNewDatabase() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.resource"),
				MessageSource.message("common.database"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getNewFTP() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.resource"),
				MessageSource.message("common.directory"),
				MessageSource.message("common.FTP"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getNewFileSystem() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.resource"),
				MessageSource.message("common.directory"),
				MessageSource.message("common.fileSystem"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getNewJMS() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.resource"),
				MessageSource.message("common.directory"),
				MessageSource.message("common.JMS"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getNewSFTP() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.resource"),
				MessageSource.message("common.directory"),
				MessageSource.message("common.SFTP"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getNewSMB() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.resource"),
				MessageSource.message("common.directory"),
				MessageSource.message("common.SMB"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getNewHTTP() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.resource"),
				MessageSource.message("common.HTTP"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getNewMailSession() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.resource"),
				MessageSource.message("common.mailSesssion"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getNewSubscribeJMS() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.new"),
				MessageSource.message("common.resource"),
				MessageSource.message("common.subscribe"),
				MessageSource.message("common.JMS"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
//     add("File|Open");
	public  static  String getFileOpen() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.open"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getFileImport() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.import"),
		};	
		return  StringUtils.join(newProjectArray, "|") + "...";
	}
	public  static  String getFileExport() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.export"),
		};	
		return  StringUtils.join(newProjectArray, "|") + "...";
	}
	public  static  String getEditRename() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.edit"),
				MessageSource.message("common.rename"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getEditCut() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.edit"),
				MessageSource.message("common.cut"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getEditCopy() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.edit"),
				MessageSource.message("common.copy"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getEditPaste() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.edit"),
				MessageSource.message("common.paste"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getEditChangeDependency() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.edit"),
				MessageSource.message("common.changeDependencyVersion"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getEditRemove() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.edit"),
				MessageSource.message("common.remove"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	public  static  String getFileHidden() {
		String[] newProjectArray =   new String[] {
				MessageSource.message("common.file"),
				MessageSource.message("common.hidden"),
		};	
		return  StringUtils.join(newProjectArray, "|");
	}
	
	
}
