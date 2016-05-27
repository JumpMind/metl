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
package org.jumpmind.metl.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.h2.H2DatabasePlatform;
import org.jumpmind.db.platform.oracle.OracleDatabasePlatform;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.SqlScript;
import org.jumpmind.exception.IoException;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

public class DatabaseScriptContainer {
    protected final Log logger = LogFactory.getLog(getClass());

    final static String IMPORT_PREFIX = "-- import:";

    private List<DatabaseScript> preInstallScripts = new ArrayList<DatabaseScript>();
    private List<DatabaseScript> postInstallScripts = new ArrayList<DatabaseScript>();

    private JdbcTemplate jdbcTemplate;
    private IDatabasePlatform platform;

    private Map<String, String> replacementTokens;

    private String scriptLocation;

    public DatabaseScriptContainer(String scriptLocation, IDatabasePlatform platform) {
        try {
            this.scriptLocation = scriptLocation;
            this.platform = platform;
            this.jdbcTemplate = new JdbcTemplate(platform.getDataSource());

            replacementTokens = new HashMap<String, String>();
            // Add any replacement tokens

            Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(new DefaultResourceLoader())
                    .getResources(String.format("classpath*:%s/*.sql", scriptLocation));
            for (Resource r : resources) {
                DatabaseScript script = new DatabaseScript(r.getFilename());
                script.setResource(r);

                if (script.getWhen() == DatabaseScript.WHEN_PREINSTALL) {
                    preInstallScripts.add(script);
                } else if (script.getWhen() == DatabaseScript.WHEN_POSTINSTALL) {
                    postInstallScripts.add(script);
                }
            }
        } catch (IOException e) {
            throw new IoException(e);
        }
    }

    public void executePreInstallScripts(String fromVersion, String toVersion) {
        executeScripts(fromVersion, toVersion, this.preInstallScripts);
    }

    public void executePostInstallScripts(String fromVersion, String toVersion) {
        executeScripts(fromVersion, toVersion, this.postInstallScripts);
    }

    public void executeScripts(String fromVersion, String toVersion, List<DatabaseScript> scripts) {
        if (scripts != null) {
            Collections.sort(scripts);
            DatabaseScript from = new DatabaseScript();
            from.parseVersion(fromVersion);

            DatabaseScript to = new DatabaseScript();
            to.parseVersion(toVersion);

            for (DatabaseScript s : scripts) {
                if (isDatabaseMatch(s) && ((s.compareVersionTo(from) > 0 && s.compareVersionTo(to) <= 0) || s.getMajor() == 999)) {
                    try {
                        executeImports(s.getResource());
                        execute(s.getResource().getURL());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    protected void executeImports(Resource resource) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String line = reader.readLine();
            while (line != null) {
                if (line.startsWith(IMPORT_PREFIX)) {
                    String file = line.substring(IMPORT_PREFIX.length()).trim();
                    Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(new DefaultResourceLoader())
                            .getResources(String.format("classpath*:%s/%s", scriptLocation, file));
                    for (Resource resource2 : resources) {
                        execute(resource2.getURL());
                    }
                }
                line = reader.readLine();
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public void execute(final URL script) {
        logger.info("Executing script " + script.toString());

        jdbcTemplate.execute(new ConnectionCallback<Object>() {
            public Object doInConnection(Connection c) throws SQLException, DataAccessException {
                ISqlTemplate template = platform.getSqlTemplate();
                SqlScript sqlscript = new SqlScript(script, template, true, ";", replacementTokens);
                sqlscript.setFailOnSequenceCreate(false);
                sqlscript.setFailOnDrop(false);
                sqlscript.setFailOnError(false);
                sqlscript.execute();
                return null;
            }
        });
    }

    public boolean isDatabaseMatch(DatabaseScript script) {
        if (script.getDescription().equals("H2Only")) {
            return platform instanceof H2DatabasePlatform;
        } else if (script.getDescription().equals("OracleOnly")) {
            return platform instanceof OracleDatabasePlatform;
        } else {
            return true;
        }
    }
}
