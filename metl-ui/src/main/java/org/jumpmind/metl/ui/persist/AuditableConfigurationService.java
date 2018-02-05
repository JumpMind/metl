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
package org.jumpmind.metl.ui.persist;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.DateUtils;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.AuditEvent;
import org.jumpmind.metl.core.model.AuditEvent.EventType;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.GlobalSetting;
import org.jumpmind.metl.core.model.IAuditable;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.persist.ConfigurationService;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.core.security.ISecurityService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.init.AppUI;
import org.jumpmind.persist.IPersistenceManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.context.WebApplicationContext;

public class AuditableConfigurationService extends ConfigurationService {

    ThreadPoolTaskScheduler scheduler;

    Map<String, Map<FlowName, Set<AbstractObject>>> changes = Collections
            .synchronizedMap(new HashMap<String, Map<FlowName, Set<AbstractObject>>>());

    long lastAuditTimeInMs = System.currentTimeMillis();

    public AuditableConfigurationService(IOperationsService operationsService, ISecurityService securityService,
            IDatabasePlatform databasePlatform, IPersistenceManager persistenceManager, String tablePrefix) {
        super(operationsService, securityService, databasePlatform, persistenceManager, tablePrefix);
    }

    @Override
    public void save(AbstractObject data) {
        log(data);
        super.save(data);
    }

    @Override
    public void delete(Component comp) {
        log(comp);
        super.delete(comp);
    }

    @Override
    public void delete(Model model) {
        log(model);
        super.delete(model);
    }

    @Override
    public void deleteFlow(Flow flow) {
        log(flow);
        super.deleteFlow(flow);
    }

    @Override
    public void delete(Resource resource) {
        log(resource);
        super.delete(resource);
    }

    protected void log(AbstractObject data) {
        WebApplicationContext context = AppUI.getWebApplicationContext();
        if (context != null) {
            ApplicationContext appContext = context.getBean(ApplicationContext.class);
            String userId = appContext.getUser().getLoginId();
            data.setLastUpdateBy(userId);
            if (isBlank(data.getCreateBy())) {
                data.setCreateBy(userId);
            }
            if (appContext.getCurrentFlow() != null) {
                if (data instanceof IAuditable) {
                    Map<FlowName, Set<AbstractObject>> components = changes.get(userId);
                    if (components == null) {
                        components = new HashMap<>();
                        changes.put(userId, components);
                    }
                    Set<AbstractObject> entries = components.get(appContext.getCurrentFlow());
                    if (entries == null) {
                        entries = new HashSet<>();
                        components.put(appContext.getCurrentFlow(), entries);
                    }
                    entries.add(data);
                }
            }
        }
    }

    @Override
    public void doInBackground() {
        recordAudit();
        purgeAudit();
    }

    protected void recordAudit() {
        Map<String, ProjectVersion> projectVersions = new HashMap<>();
        Map<String, Map<FlowName, Set<AbstractObject>>> saved = changes;
        changes = new HashMap<>();
        for (String userId : saved.keySet()) {
            Map<FlowName, Set<AbstractObject>> byUser = saved.get(userId);
            for (FlowName flow : byUser.keySet()) {
                ProjectVersion projectVersion = projectVersions.get(flow.getProjectVersionId());
                if (projectVersion == null) {
                    projectVersion = findProjectVersion(flow.getProjectVersionId());
                    projectVersions.put(projectVersion.getId(), projectVersion);
                }
                int changes = byUser.get(flow).size();
                AuditEvent event = new AuditEvent(EventType.CONFIG, String.format("%d changes were made since %s to '%s' in '%s'", changes,
                        SimpleDateFormat.getTimeInstance().format(new Date(lastAuditTimeInMs)), flow.getName(), projectVersion.getName()),
                        userId);
                save(event);
            }
        }
        lastAuditTimeInMs = System.currentTimeMillis();
    }

    protected void purgeAudit() {
        GlobalSetting retentionInDays = operationsService.findGlobalSetting(GlobalSetting.AUDIT_EVENT_RETENTION_IN_DAYS);
        int daysToKeep = GlobalSetting.DEFAULT_AUDIT_EVENT_RETENTION_IN_DAYS;
        if (retentionInDays != null) {
            daysToKeep = Integer.parseInt(retentionInDays.getValue());
        }

        Date cutOff = DateUtils.addDays(new Date(), -daysToKeep);
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        int deleted = template.update(String.format("delete from %1$s_audit_event where create_time < ? ", tablePrefix), cutOff);
        if (deleted > 0) {
            log.info("Purged {} audit events", deleted);
        }
    }

}
