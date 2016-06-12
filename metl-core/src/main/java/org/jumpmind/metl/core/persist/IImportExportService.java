package org.jumpmind.metl.core.persist;

public interface IImportExportService {

    public String exportModel(String projectVersionId, String modelId);
    
    public String exportResource(String projectVersionId, String resourceId);
    
    public String exportFlow(String projectVersionId, String flowId);
    
    public String exportProject(String projectVersionId);
    
    public void importConfiguration(String dataToImport);
}
