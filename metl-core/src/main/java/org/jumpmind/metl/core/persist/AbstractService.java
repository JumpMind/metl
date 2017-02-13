/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.persist;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.AbstractObjectLastUpdateTimeDescSorter;
import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.FolderType;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.security.ISecurityService;
import org.jumpmind.metl.core.security.SecurityConstants;
import org.jumpmind.persist.IPersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractService {
    
    final protected Logger log = LoggerFactory.getLogger(getClass()); 

    protected IPersistenceManager persistenceManager;
    
    protected String tablePrefix;
    
    protected ISecurityService securityService;

    AbstractService(ISecurityService securityService, IPersistenceManager persistenceManager, String tablePrefix) {
        this.persistenceManager = persistenceManager;
        this.tablePrefix = tablePrefix;
        this.securityService = securityService;
    }
    
    protected String tableName(Class<?> clazz) {
        StringBuilder name = new StringBuilder(tablePrefix);
        int end = clazz.getSimpleName().indexOf("Name");
        if (end < 0) {
            end = clazz.getSimpleName().length();
        }
        String[] tokens = StringUtils.splitByCharacterTypeCamelCase(clazz.getSimpleName()
                .substring(0, end));
        for (String string : tokens) {
            name.append("_");
            name.append(string);
        }
        return name.toString();
    }
    
    protected <T> List<T> find(Class<T> clazzToMap, Map<String, Object> params, Class<?> tableClazz) {
        return persistenceManager.find(clazzToMap, params, null, null, tableName(tableClazz));
    }

    protected <T> List<T> find(Class<T> clazz, Map<String, Object> params) {
        return persistenceManager.find(clazz, params, null, null, tableName(clazz));
    }
    
    protected <T> int count(Class<T> clazz, Map<String, Object> params) {
        return persistenceManager.count(clazz, null, null, tableName(clazz), params);
    }

    protected <T> T findOne(Class<T> clazz, Map<String, Object> params) {
        List<T> all = persistenceManager.find(clazz, params, null, null, tableName(clazz));
        if (all.size() > 0) {
            return all.get(0);
        } else {
            return null;
        }
    }
    
    protected <T> T findOne(Class<T> clazzToMap, Map<String, Object> params, Class<?> tableNameClazz) {
        List<T> all = persistenceManager.find(clazzToMap, params, null, null, tableName(tableNameClazz));
        if (all.size() > 0) {
            return all.get(0);
        } else {
            return null;
        }
    }    

    public void delete(AbstractObject data) {
        persistenceManager.delete(data, null, null, tableName(data.getClass()));
    }

    protected void refresh(AbstractObject object) {
        persistenceManager.refresh(object, null, null, tableName(object
                .getClass()));
    }
    
    public void save(AbstractObject data) {
        data.setLastUpdateTime(new Date());
        persistenceManager.save(data, null, null, tableName(data.getClass()));
    }
    
    protected void rethrow(Throwable error) {
        if (error instanceof RuntimeException) {
            throw (RuntimeException)error;
        } else if (error instanceof Error) {
            throw (Error)error;
        } else {
            throw new RuntimeException(error);
        }
    }

    protected List<? extends Setting> findSettings(Class<? extends Setting> clazz,
            Map<String, Object> params) {
        List<? extends Setting> settings = persistenceManager.find(clazz, params, null, null,
                tableName(clazz));
        for (Setting setting : settings) {
            if (isPassword(setting)) {
                String value = setting.getValue();
                if (value != null && value.startsWith(SecurityConstants.PREFIX_ENC)) {
                    try {
                        setting.setValue(securityService.decrypt(
                                value.substring(SecurityConstants.PREFIX_ENC.length() - 1)));
                    } catch (Exception ex) {
                        setting.setValue(null);
                        log.error("Failed to decrypt password for the setting: " + setting.getName()
                                + ".  The encrypted value was: " + value
                                + ".  Please check your keystore.", ex);
                    }
                }
            }
        }
        AbstractObjectLastUpdateTimeDescSorter.sort(settings);
        return settings;
    }    
    
    protected boolean isPassword(Setting setting) {
        return setting.getName().contains("password");
    }

    protected Map<String, Folder> foldersById(String projectVersionId, FolderType type) {
        Map<String, Object> byType = new HashMap<String, Object>();
        byType.put("type", type.name());
        if (isNotBlank(projectVersionId)) {
            byType.put("projectVersionId", projectVersionId);
        }
        byType.put("deleted", 0);
        List<Folder> folders = find(Folder.class, byType);

        Map<String, Folder> all = new HashMap<String, Folder>();
        for (Folder folder : folders) {
            all.put(folder.getId(), folder);
        }
        return all;
    }
    
    public void save(Setting setting) {
        boolean isPassword = isPassword(setting);
        String unencrypted = setting.getValue();
        if (isPassword && isNotBlank(unencrypted)) {
            String encrypted = SecurityConstants.PREFIX_ENC + securityService.encrypt(unencrypted);
            setting.setValue(encrypted);
        }
        save((AbstractObject) setting);
        if (isPassword) {
            setting.setValue(unencrypted);
        }
    }


}
