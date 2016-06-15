package org.jumpmind.metl.core.persist;

import java.util.List;

public interface IImportExportService {

    public String exportModel(String projectVersionId, String modelId);
    
    public String exportResource(String projectVersionId, String resourceId);
    
    public String exportFlow(String projectVersionId, String flowId);
    
    public String exportProject(String projectVersionId);
    
    public void importConfiguration(String dataToImport);
    
    public String export(String projectVersionId, List<String> flowIds, List<String> modelIds, List<String> resoruceIds);
}
