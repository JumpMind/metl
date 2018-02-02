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
package org.jumpmind.metl.core.job;

import static org.jumpmind.metl.core.model.GlobalSetting.CONFIG_BACKUP_RETENTION_IN_DAYS;
import static org.jumpmind.metl.core.model.GlobalSetting.DEFAULT_CONFIG_BACKUP_RETENTION_IN_DAYS;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateUtils;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IImportExportService;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.core.util.AppConstants;
import org.jumpmind.properties.TypedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupJob implements Runnable {

    final protected Logger log = LoggerFactory.getLogger(getClass());

    IImportExportService importExportService;

    IConfigurationService configurationService;
    
    IOperationsService operationsService;
    
    String configDir;

    public BackupJob(IImportExportService importExportService,
            IConfigurationService configurationService, IOperationsService operationsService, String configDir) {
        this.importExportService = importExportService;
        this.configurationService = configurationService;
        this.operationsService = operationsService;
        this.configDir = configDir;
    }

    @Override
    public void run() {
        try {
            TypedProperties properties = operationsService.findGlobalSetttingsAsProperties();
            File backupDir = new File(configDir, "backups");
            backupDir.mkdirs();
            backup(properties, backupDir);
            purge(properties, backupDir);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    protected void backup(TypedProperties properties, File backupDir) throws IOException {
        log.info("Backing up to {}", backupDir.getAbsolutePath());
        File todaysDir = new File(backupDir,
                new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()));
        todaysDir.mkdirs();
        configurationService.backupDatabase(new File(todaysDir, "backup.zip").getAbsolutePath());
        Collection<ProjectVersion> versions = configurationService.findProjectVersions().values();
        for (ProjectVersion version : versions) {
            String json = importExportService.exportProjectVersion(version.getId(), AppConstants.SYSTEM_USER);
            FileUtils.write(new File(todaysDir,
                    (version.getProject().getName() + "-" + version.getName()).toLowerCase().replaceAll(" - ", " ").replaceAll(" ", "-") + ".json"),
                    json);
        }
    }
    
    protected void purge(TypedProperties properties, File backupDir) throws IOException {
        File[] files = backupDir.listFiles();
        int retentionInDays = properties.getInt(CONFIG_BACKUP_RETENTION_IN_DAYS, DEFAULT_CONFIG_BACKUP_RETENTION_IN_DAYS);
        Date cutoff = DateUtils.addDays(new Date(), -retentionInDays);
        for (File file : files) {
            if (file.isDirectory() && file.lastModified() < cutoff.getTime()) {
                log.info("Purging backup " + file.getAbsolutePath());
                FileUtils.deleteDirectory(file);
            }
        }
    }
 
}