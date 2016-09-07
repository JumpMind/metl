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

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.util.ConfigDatabaseUpgrader;
import org.jumpmind.db.util.ResettableBasicDataSource;
import org.jumpmind.metl.core.utils.DbTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SchemaTest {

    IDatabasePlatform platform;

    @Before
    public void setup() throws Exception {
        platform = DbTestUtils.createDatabasePlatform();
    }
    
    @After
    public void tearDown() throws Exception {
        ResettableBasicDataSource ds = platform.getDataSource();
        ds.close();
    }

    @Test
    public void createSchema() throws Exception {        
        ConfigDatabaseUpgrader upgrader = new ConfigDatabaseUpgrader("/schema.xml", platform, true, "METL_");
        upgrader.upgrade();
    }
}
