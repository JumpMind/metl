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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.persist.IPersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractService {
    
    final protected Logger log = LoggerFactory.getLogger(getClass()); 

    protected IPersistenceManager persistenceManager;
    
    protected String tablePrefix;

    AbstractService(IPersistenceManager persistenceManager, String tablePrefix) {
        this.persistenceManager = persistenceManager;
        this.tablePrefix = tablePrefix;
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

    
}
