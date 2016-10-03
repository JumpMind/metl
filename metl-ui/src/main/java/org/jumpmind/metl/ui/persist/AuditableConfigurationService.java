package org.jumpmind.metl.ui.persist;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.AuditEvent;
import org.jumpmind.metl.core.model.AuditEvent.EventType;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.IAuditable;
import org.jumpmind.metl.core.model.Model;
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

    Map<String, Set<String>> changes = Collections
            .synchronizedMap(new HashMap<String, Set<String>>());

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
            String userId = context.getBean(ApplicationContext.class).getUser().getLoginId();
            data.setLastUpdateBy(userId);
            if (data instanceof IAuditable) {
                Set<String> components = changes.get(userId);
                if (components == null) {
                    components = new HashSet<>();
                    changes.put(userId, components);
                }
                components.add(data.getId());
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
        scheduler.scheduleAtFixedRate(() -> audit(), 300000);
    }

    protected void audit() {
        Map<String, Set<String>> saved = changes;
        changes = new HashMap<>();
        for (String userId : saved.keySet()) {
            int changes = saved.get(userId).size();
            int minutes = (int) (System.currentTimeMillis() - lastAuditTimeInMs) / 60000;
            AuditEvent event = new AuditEvent(EventType.CONFIG,
                    String.format("%d changes were made in the last %d minutes", changes, minutes),
                    userId);
            save(event);
        }
        lastAuditTimeInMs = System.currentTimeMillis();
    }

    @PreDestroy
    protected void stopAuditJob() {
        audit();
        scheduler.destroy();
    }

}
