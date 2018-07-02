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
package org.jumpmind.metl.core.runtime.resource;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.util.BasicDataSourceFactory;
import org.jumpmind.db.util.ResettableBasicDataSource;
import org.jumpmind.properties.TypedProperties;

public class Datasource extends AbstractResourceRuntime implements IDatasourceRuntime {

    public static final String TYPE = "Database";
    
    public final static String DB_POOL_DRIVER = "db.driver";

    public final static String DB_POOL_URL = "db.url";

    public final static String DB_POOL_USER = "db.user";

    public final static String DB_POOL_PASSWORD = "db.password";

    public final static String DB_POOL_VALIDATION_QUERY = "db.validation.query";

    public final static String DB_POOL_INITIAL_SIZE = "db.pool.initial.size";

    public final static String DB_POOL_MAX_ACTIVE = "db.pool.max.active";

    public final static String DB_POOL_MAX_IDLE = "db.pool.max.idle";

    public final static String DB_POOL_MIN_IDLE = "db.pool.min.idle";

    public final static String DB_POOL_MAX_WAIT = "db.pool.max.wait.millis";

    public final static String DB_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS = "db.pool.min.evictable.idle.millis";

    public final static String DB_POOL_TEST_ON_BORROW = "db.test.on.borrow";

    public final static String DB_POOL_TEST_ON_RETURN = "db.test.on.return";

    public final static String DB_POOL_TEST_WHILE_IDLE = "db.test.while.idle";

    public final static String DB_POOL_INIT_SQL = "db.init.sql";

    public final static String DB_FETCH_SIZE = "db.fetch.size";

    public final static String DB_QUERY_TIMEOUT = "db.query.timeout";
    
    public final static String DB_CONNECTION_PROPERTIES = "db.connection.properties";
    
    ResettableBasicDataSource dataSource = new ResettableBasicDataSource();
    
    protected Map<String, Table> tableCache = new HashMap<String, Table>();
    
    @Override
    protected void start(TypedProperties properties) {
        this.dataSource = BasicDataSourceFactory
                .create(properties);
    }

    @Override
    public void stop() {
        try {
            dataSource.close();
        } catch (Exception e) {
        } finally {
            dataSource = null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T reference() {
        return (T)dataSource;
    }
    
    public void putTableInCache(String catalogName, String schemaName, String tableName, Table table) {
        String key = Table.getFullyQualifiedTableName(catalogName, schemaName, tableName);
        tableCache.put(key, table);
    }
    
    public Table getTableFromCache(String catalogName, String schemaName, String tableName) {
        String key = Table.getFullyQualifiedTableName(catalogName, schemaName, tableName);
        return tableCache.get(key);
    }
    
    @Override
    public boolean isTestSupported() {
        return true;
    }
    
    @Override
    public boolean test() {
        try {
            Connection c = dataSource.getConnection();
            c.close();
            dataSource.close();
            return true;
        } catch (Exception ex) {
            throw new RuntimeException("Error connecting to database", ex);
        }
    }
    
}
