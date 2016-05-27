package org.jumpmind.metl.core.persist;

import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ProjectVersion;

public interface IImportExportService {

    public String export(ProjectVersion projectVersion, Model model);
}
