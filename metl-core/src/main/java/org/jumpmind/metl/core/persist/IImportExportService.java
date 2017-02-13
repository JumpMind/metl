package org.jumpmind.metl.core.persist;

import java.util.List;

import org.jumpmind.metl.core.model.Agent;

public interface IImportExportService {
    
    public void importConfiguration(String dataToImport, String userId);
    
    public String exportProjectVersion(String projectVersionId, String userdId);
    
    public String exportFlows(String projectVersionId, List<String> flowIds, List<String> modelIds, List<String> resoruceIds, String userId);
    
    public String exportReleasePackage(String releasePackageId, String userId);
    
    public String export(Agent agent);

}
