package org.jumpmind.metl.core.persist;

import java.util.List;

public interface IImportExportService {
    
    public void importConfiguration(String dataToImport, String userId);
    
    public String export(String projectVersionId, String usedId);
    
    public String export(String projectVersionId, List<String> flowIds, List<String> modelIds, List<String> resoruceIds, String userId);
}
