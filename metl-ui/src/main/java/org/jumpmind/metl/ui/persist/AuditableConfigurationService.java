package org.jumpmind.metl.ui.persist;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

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
import org.jumpmind.metl.core.persist.ConfigurationSqlService;
import org.jumpmind.metl.core.runtime.component.definition.IComponentDefinitionFactory;
import org.jumpmind.metl.core.security.ISecurityService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.init.AppUI;
import org.jumpmind.persist.IPersistenceManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.context.WebApplicationContext;

public class AuditableConfigurationService extends ConfigurationSqlService {

    ThreadPoolTaskScheduler scheduler;

    Map<String, Map<FlowName, Set<AbstractObject>>> changes = Collections
            .synchronizedMap(new HashMap<String, Map<FlowName, Set<AbstractObject>>>());

    long lastAuditTimeInMs = System.currentTimeMillis();

    public AuditableConfigurationService(ISecurityService securityService,
            IComponentDefinitionFactory componentDefinitionFactory,
            IDatabasePlatform databasePlatform, IPersistenceManager persistenceManager,
            String tablePrefix) {
        super(securityService, componentDefinitionFactory, databasePlatform, persistenceManager,
                tablePrefix);
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
            if (appContext.getCurrentFlow() != null) {
                String userId = appContext.getUser().getLoginId();
                data.setLastUpdateBy(userId);
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

    @PostConstruct
    protected void startAuditJob() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setDaemon(true);
        scheduler.setThreadNamePrefix("audit-job-");
        scheduler.setPoolSize(1);
        scheduler.initialize();
        scheduler.scheduleAtFixedRate(() -> doInBackground(), 300000);
    }
    
    protected void doInBackground() {
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
                int minutes = (int) Math
                        .ceil(((double) (System.currentTimeMillis() - lastAuditTimeInMs))
                                / (double) 60000);
                AuditEvent event = new AuditEvent(EventType.CONFIG, String.format(
                        "%d changes were made in the last %d minutes to '%s' in '%s'", changes, minutes, flow.getName(), projectVersion.getName()), userId);
                save(event);
            }
        }
        lastAuditTimeInMs = System.currentTimeMillis();
    }
    
    protected void purgeAudit() {
        GlobalSetting retentionInDays = findGlobalSetting(GlobalSetting.AUDIT_EVENT_RETENTION_IN_DAYS);
        int daysToKeep = GlobalSetting.DEFAULT_AUDIT_EVENT_RETENTION_IN_DAYS;
        if (retentionInDays != null) {
            daysToKeep = Integer.parseInt(retentionInDays.getValue());
        }
        
        Date cutOff = DateUtils.addDays(new Date(), -daysToKeep);
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        int deleted = template.update(String.format(
                "delete from %1$s_audit_event where create_time < ? ",
                tablePrefix), cutOff);
        log.info("Purged {} audit events", deleted);
    }

    @PreDestroy
    protected void stopAuditJob() {
        recordAudit();
        scheduler.destroy();
    }

}
