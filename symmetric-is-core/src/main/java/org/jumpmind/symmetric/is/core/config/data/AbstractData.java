package org.jumpmind.symmetric.is.core.config.data;

import java.util.Date;
import java.util.UUID;

public abstract class AbstractData {

    String id = UUID.randomUUID().toString();

    String name;

    Date createTime;

    String createBy;
    
    String folderId;
    
    Date lastModifyTime;
    
    String lastModifyBy;
    
}
