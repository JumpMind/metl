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
package org.jumpmind.metl.ui.common;

import java.io.Serializable;
import java.sql.SQLException;

import org.apache.commons.dbcp.BasicDataSource;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.BasicDataSourceFactory;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.vaadin.ui.sqlexplorer.IDb;

public class DbResource implements IDb, Serializable {

        private static final long serialVersionUID = 1L;

        TypedProperties properties;

        String name;

        IDatabasePlatform platform;

        public DbResource(String name, TypedProperties properties) {
            this.name = name;
            this.properties = properties;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public IDatabasePlatform getPlatform() {
            if (platform == null) {                
                BasicDataSource dataSource = BasicDataSourceFactory.create(properties);
                dataSource.setMaxActive(2);
                dataSource.setMaxIdle(1);
                dataSource.setMinIdle(0);
                dataSource.setInitialSize(0);
                platform = JdbcDatabasePlatformFactory.createNewPlatformInstance(dataSource,
                        new SqlTemplateSettings(), false, false);
            }
            return platform;
        }
        
        public void close() {
            if (platform != null) {
                BasicDataSource ds = (BasicDataSource)platform.getDataSource();
                if (ds != null) {
                    try {
                        ds.close();
                    } catch (SQLException e) {
                    }
                }
            }
        }        

    }