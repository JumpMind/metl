package org.jumpmind.symmetric.is.core.persist;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.config.AbstractObject;
import org.jumpmind.symmetric.is.core.config.data.AbstractData;

public abstract class AbstractService {

    protected IPersistenceManager persistenceManager;
    
    protected String tablePrefix;

    AbstractService(IPersistenceManager persistenceManager, String tablePrefix) {
        this.persistenceManager = persistenceManager;
        this.tablePrefix = tablePrefix;
    }
    
    protected String tableName(Class<?> clazz) {
        StringBuilder name = new StringBuilder(tablePrefix);
        String[] tokens = StringUtils.splitByCharacterTypeCamelCase(clazz.getSimpleName()
                .substring(0, clazz.getSimpleName().indexOf("Data")));
        for (String string : tokens) {
            name.append("_");
            name.append(string);
        }
        return name.toString();
    }

    protected <T> List<T> find(Class<T> clazz, Map<String, Object> params) {
        return persistenceManager.find(clazz, params, null, null, tableName(clazz));
    }

    protected <T> T findOne(Class<T> clazz, Map<String, Object> params) {
        List<T> all = persistenceManager.find(clazz, params, null, null, tableName(clazz));
        if (all.size() > 0) {
            return all.get(0);
        } else {
            return null;
        }
    }
    
    protected void delete(AbstractData data) {
        persistenceManager.delete(data, null, null, tableName(data.getClass()));
    }
    
    protected void refresh(AbstractObject<?> object) {
        persistenceManager.refresh(object.getData(), null, null, tableName(object.getData()
                .getClass()));
    }
    
    public void save(AbstractObject<?> obj) {
        save(obj.getData());
    }

    public void save(AbstractData data) {
        data.setLastModifyTime(new Date());
        persistenceManager.save(data, null, null, tableName(data.getClass()));
    }
    
}
