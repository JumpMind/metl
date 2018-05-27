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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.Row;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.AbstractObjectCreateTimeDescSorter;
import org.jumpmind.metl.core.model.AbstractObjectLastUpdateTimeDescSorter;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.model.AgentDeploymentSummary;
import org.jumpmind.metl.core.model.AgentFlowDeployParm;
import org.jumpmind.metl.core.model.AgentName;
import org.jumpmind.metl.core.model.AgentParameter;
import org.jumpmind.metl.core.model.AgentResource;
import org.jumpmind.metl.core.model.AgentResourceSetting;
import org.jumpmind.metl.core.model.AuditEvent;
import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.FolderType;
import org.jumpmind.metl.core.model.GlobalSetting;
import org.jumpmind.metl.core.model.Group;
import org.jumpmind.metl.core.model.GroupPrivilege;
import org.jumpmind.metl.core.model.Notification;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.model.User;
import org.jumpmind.metl.core.model.UserGroup;
import org.jumpmind.metl.core.model.UserHist;
import org.jumpmind.metl.core.model.UserSetting;
import org.jumpmind.metl.core.security.ISecurityService;
import org.jumpmind.metl.core.security.SecurityConstants;
import org.jumpmind.metl.core.util.NameValue;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.properties.TypedProperties;

public class OperationsService extends AbstractService implements IOperationsService {
    
    IDatabasePlatform databasePlatform;

    public OperationsService(ISecurityService securityService, IPersistenceManager persistenceManager, IDatabasePlatform databasePlatform, String tablePrefix) {
        super(securityService, persistenceManager, tablePrefix);
        this.databasePlatform = databasePlatform;
    }
    

    @Override
    public List<AgentName> findAgentsInFolder(Folder folder) {
        Map<String, Object> params = new HashMap<String, Object>();
        String folderId = null;
        if (folder != null) {
            folderId = folder.getId();
        }
        params.put("folderId", folderId);
        params.put("deleted", 0);
        return find(AgentName.class, params, Agent.class);
    }

    @Override
    public List<Agent> findAgents() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deleted", 0);
        return findAgents(params);
    }

    @Override
    public List<AuditEvent> findAuditEvents(int limit) {
        List<AuditEvent> list = persistenceManager.find(AuditEvent.class, null, null,
                tableName(AuditEvent.class));
        AbstractObjectCreateTimeDescSorter.sort(list);
        return list;
    }

    @Override
    public Agent findAgent(String agentId, boolean includeDeployments) {
        Agent agent = findOne(Agent.class, new NameValue("id", agentId));
        if (agent.getFolder() != null) {
            refresh(agent.getFolder());
        }
        refreshAgentParameters(agent);
        refreshAgentResourceSettings(agent);
        if (includeDeployments) {
            refreshAgentDeployments(agent);
        }
        return agent;
    }

    protected List<Agent> findAgents(Map<String, Object> params) {
        return findAgents(params, null);
    }

    protected List<Agent> findAgents(Map<String, Object> params, Folder folder) {
        List<Agent> list = persistenceManager.find(Agent.class, params, null, null,
                tableName(Agent.class));
        Map<String, Folder> folderMapById = new HashMap<String, Folder>();
        if (folder != null) {
            folderMapById.put(folder.getId(), folder);
        } else {
            folderMapById = foldersById(null, FolderType.AGENT);
        }

        for (Agent agent : list) {
            refreshAgentParameters(agent);
            refreshAgentResourceSettings(agent);
            refreshAgentDeployments(agent);
            agent.setFolder(folderMapById.get(agent.getFolderId()));
        }

        Collections.sort(list, new Comparator<Agent>() {
            @Override
            public int compare(Agent o1, Agent o2) {
                return o1.getCreateTime().compareTo(o2.getCreateTime());
            }
        });
        return list;
    }

    public synchronized void refreshAgentParameters(Agent agent) {
        Map<String, Object> settingParams = new HashMap<String, Object>();
        settingParams.put("agentId", agent.getId());
        List<AgentParameter> parameters = persistenceManager.find(AgentParameter.class,
                settingParams, null, null, tableName(AgentParameter.class));
        agent.setAgentParameters(parameters);
    }


    @Override
    public AgentDeploy findAgentDeployment(String id) {
        AgentDeploy agentDeployment = findOne(AgentDeploy.class, new NameValue("id", id));
        refreshAgentDeploymentRelations(agentDeployment);
        return agentDeployment;
    }

    @Override
    public AgentResource findAgentResource(String agentId, Resource resource) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("agentId", agentId);
        params.put("resourceId", resource.getId());
        @SuppressWarnings("unchecked")
        List<AgentResourceSetting> settings = (List<AgentResourceSetting>) findSettings(
                AgentResourceSetting.class, params);

        for (Setting resourceSetting : resource.getSettings()) {
            boolean exists = false;
            for (AgentResourceSetting setting : settings) {
                if (setting.getName().equals(resourceSetting.getName())) {
                    exists = true;
                }
            }
            if (!exists) {
                AgentResourceSetting setting = new AgentResourceSetting();
                setting.setId(resource.getId());
                setting.setAgentId(agentId);
                setting.setResourceId(resource.getId());
                setting.setName(resourceSetting.getName());
                setting.setValue(resourceSetting.getValue());
                settings.add(setting);
            }
        }

        AgentResource agentResource = new AgentResource();
        agentResource.setProjectVersionId(resource.getProjectVersionId());
        agentResource.setId(resource.getId());
        agentResource.setAgentId(agentId);
        agentResource.setType(resource.getType());
        agentResource.setSettings(settings);
        return agentResource;
    }
    
    @Override
    public User findUser(String id) {
        User user = null;
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        List<User> users = persistenceManager.find(User.class, params, null, null,
                tableName(User.class));
        if (users.size() > 0) {
            user = users.get(0);
            refresh(user);
        }
        return user;
    }

    @Override
    public User findUserByLoginId(String loginId) {
        User user = null;
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("loginId", loginId);
        List<User> users = persistenceManager.find(User.class, params, null, null,
                tableName(User.class));
        if (users.size() > 0) {
            user = users.get(0);
            refresh(user);
        }
        return user;
    }

    @Override
    public List<User> findUsersByGroup(String groupId) {
        List<User> users = new ArrayList<User>();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("groupId", groupId);
        List<UserGroup> userGroups = persistenceManager.find(UserGroup.class, params, null, null,
                tableName(UserGroup.class));
        for (UserGroup userGroup : userGroups) {
            users.add(findUser(userGroup.getUserId()));
        }
        return users;
    }

    @Override
    public List<User> findUsers() {
        return persistenceManager.find(User.class, null, null, null, tableName(User.class));
    }

    @Override
    public Group findGroup(String id) {
        Group group = null;
        Map<String, Object> params = new HashMap<String, Object>();
        params = new HashMap<String, Object>();
        params.put("id", id);
        List<Group> groups = persistenceManager.find(Group.class, params, null, null,
                tableName(Group.class));
        if (groups.size() > 0) {
            group = groups.get(0);
            refresh(group);
        }
        return group;
    }

    @Override
    public Group findGroupByName(String name) {
        Group group = null;
        Map<String, Object> params = new HashMap<String, Object>();
        params = new HashMap<String, Object>();
        params.put("name", name);
        List<Group> groups = persistenceManager.find(Group.class, params, null, null,
                tableName(Group.class));
        if (groups.size() > 0) {
            group = groups.get(0);
            refresh(group);
        }
        return group;
    }

    @Override
    public List<Group> findGroups() {
        return persistenceManager.find(Group.class, null, null, null, tableName(Group.class));
    }

    @Override
    public void delete(AgentDeploy agentDeployment) {
        List<AgentFlowDeployParm> params = agentDeployment.getAgentDeploymentParms();
        for (AgentFlowDeployParm agentDeploymentParameter : params) {
            delete((AbstractObject) agentDeploymentParameter);
        }
        delete((AbstractObject) agentDeployment);
    }
    
    @Override
    public void delete(Agent agent) {
        agent.setDeleted(true);
        save((AbstractObject) agent);
    }


    @Override
    public void refresh(User user) {
        Map<String, Object> params = new HashMap<String, Object>();
        params = new HashMap<String, Object>();
        params.put("userId", user.getId());

        @SuppressWarnings("unchecked")
        List<UserSetting> settings = (List<UserSetting>) findSettings(UserSetting.class, params);
        user.setSettings(settings);

        List<Group> groups = new ArrayList<Group>();
        List<UserGroup> userGroups = persistenceManager.find(UserGroup.class, params, null, null,
                tableName(UserGroup.class));
        for (UserGroup userGroup : userGroups) {
            groups.add(findGroup(userGroup.getGroupId()));
        }
        user.setGroups(groups);
    }

    @Override
    public void refresh(Group group) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("groupId", group.getId());
        group.setGroupPrivileges(persistenceManager.find(GroupPrivilege.class, params, null, null,
                tableName(GroupPrivilege.class)));
    }
    
    @Override
    public List<Notification> findNotifications() {
        return persistenceManager.find(Notification.class, null, null, null,
                tableName(Notification.class));
    }

    @Override
    public List<Notification> findNotificationsForAgent(String agentId) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("notificationLevel", Notification.NotificationLevel.AGENT.toString());
        param.put("linkId", agentId);
        param.put("enabled", 1);
        List<Notification> agentNotifications = persistenceManager.find(Notification.class, param,
                null, null, tableName(Notification.class));

        param = new HashMap<String, Object>();
        param.put("notificationLevel", Notification.NotificationLevel.GLOBAL.toString());
        param.put("enabled", 1);
        List<Notification> notifications = persistenceManager.find(Notification.class, param, null,
                null, tableName(Notification.class));
        notifications.addAll(agentNotifications);
        return notifications;
    }

    @Override
    public List<Notification> findNotificationsForDeployment(AgentDeploy deployment) {
        List<Notification> notifications = findNotificationsForAgent(deployment.getAgentId());
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("notificationLevel", Notification.NotificationLevel.DEPLOYMENT.toString());
        param.put("linkId", deployment.getId());
        param.put("enabled", 1);
        List<Notification> agentNotifications = persistenceManager.find(Notification.class, param,
                null, null, tableName(Notification.class));
        notifications.addAll(agentNotifications);
        return notifications;
    }

    @Override
    public void refresh(Notification notification) {
        refresh((AbstractObject) notification);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<GlobalSetting> findGlobalSettings() {
        return (List<GlobalSetting>) findSettings(GlobalSetting.class, null);
    }

    @Override
    public GlobalSetting findGlobalSetting(String name) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("name", name);
        @SuppressWarnings("unchecked")
        List<GlobalSetting> settings = (List<GlobalSetting>) findSettings(GlobalSetting.class,
                param);
        if (settings.size() > 0) {
            return settings.get(0);
        }
        return null;
    }

    @Override
    public GlobalSetting findGlobalSetting(String name, String defaultValue) {
        GlobalSetting setting = findGlobalSetting(name);
        if (setting == null) {
            setting = new GlobalSetting();
            setting.setName(name);
            setting.setValue(defaultValue);
        }
        return setting;
    }

    @Override
    public TypedProperties findGlobalSetttingsAsProperties() {
        TypedProperties properties = new TypedProperties();
        for (GlobalSetting setting : findGlobalSettings()) {
            properties.put(setting.getName(), setting.getValue());
        }
        return properties;
    }

    @Override
    public Map<String, String> findGlobalSettingsAsMap() {
        Map<String, String> globalSettings = new HashMap<String, String>();
        for (GlobalSetting setting : findGlobalSettings()) {
            globalSettings.put(setting.getName(), setting.getValue());
        }
        return globalSettings;
    }
    

    @Override
    public List<UserHist> findUserHist(String id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", id);
        List<UserHist> list = find(UserHist.class, params);
        AbstractObjectLastUpdateTimeDescSorter.sort(list);
        return list;
    }

    @Override
    public void savePassword(User user, String newPassword) {
        UserHist hist = new UserHist();
        hist.setUserId(user.getId());
        hist.setLastUpdateTime(user.getLastPasswordTime());
        hist.setPassword(user.getPassword());
        hist.setSalt(user.getSalt());
        hist.setAuthMethod(user.getAuthMethod());
        save(hist);

        user.setAuthMethod(SecurityConstants.PASSWORD_AUTH_METHOD_SHASH);
        user.setSalt(securityService.nextSecureHexString(10));
        user.setLastPasswordTime(new Date());
        user.setPassword(securityService.hash(user.getSalt(), newPassword));
        user.setFailedLogins(0);
        save(user);
    }
    
    @Override
    public boolean isUserLoginEnabled() {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template.queryForInt(String.format("select count(*) from %1$s_user where password is not null", tablePrefix)) > 0;
    }

    public boolean isUserLocked(User user) {
    	GlobalSetting failedAttemptLimitSetting = findGlobalSetting(GlobalSetting.PASSWORD_FAILED_ATTEMPTS, 
                Integer.toString(GlobalSetting.PASSWORD_FAILED_ATTEMPTS_DEFAULT));
        int failedAttempts = Integer.parseInt(failedAttemptLimitSetting.getValue());
     
        boolean result = false;
        if (failedAttempts > 0) {
            if (user.getFailedLogins() >= failedAttempts) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public void delete(User user) {
        refresh(user);
        for (Setting setting : user.getSettings()) {
            persistenceManager.delete(setting, null, null, tableName(UserSetting.class));
        }
        for (Group group : user.getGroups()) {
            persistenceManager.delete(new UserGroup(user.getId(), group.getId()), null, null,
                    tableName(UserGroup.class));
        }

        List<UserHist> history = findUserHist(user.getId());
        for (UserHist userHist : history) {
            persistenceManager.delete(userHist, null, null, tableName(UserHist.class));
        }

        persistenceManager.delete(user, null, null, tableName(User.class));
    }

    @Override
    public void delete(Group group) {
        refresh(group);
        for (GroupPrivilege groupPriv : group.getGroupPrivileges()) {
            persistenceManager.delete(groupPriv, null, null, tableName(GroupPrivilege.class));
        }
        persistenceManager.delete(group, null, null, tableName(Group.class));
    }
    

    @Override
    public void save(AgentDeploy agentDeployment) {
        save((AbstractObject) agentDeployment);
        List<AgentFlowDeployParm> parameters = agentDeployment.getAgentDeploymentParms();
        for (AgentFlowDeployParm agentDeploymentParameter : parameters) {
            save((AbstractObject) agentDeploymentParameter);
        }
    }    

    @Override
    public List<AgentDeploymentSummary> findAgentDeploymentSummary(String agentId) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template.query(String.format(
                "select p.name as project_name, v.version_label, '%2$s' as type, " +
                "d.id, d.name, d.start_type, d.log_level, d.start_expression, d.status, f.id as flow_id, " +
                "f.row_id " +
                "from %1$s_agent_deploy d " +
                "inner join %1$s_flow f on f.id = d.flow_id " +
                "inner join %1$s_project_version v on v.id = f.project_version_id " +
                "inner join %1$s_project p on p.id = v.project_id " +
                "where d.agent_id = ? " +
                "union " +
                "select distinct p.name, v.version_label, '%3$s', " +
                "r.id, r.name, null, null, null, null, null as flow_id, " +
                "r.row_id " +
                "from %1$s_agent_deploy d " +
                "inner join %1$s_flow f on f.id = d.flow_id " +
                "inner join %1$s_project_version v on v.id = f.project_version_id " +
                "inner join %1$s_project p on p.id = v.project_id " +
                "inner join %1$s_resource r on r.project_version_id = v.id " +
                "where d.agent_id = ? and r.deleted=0 " +
                "union " +
                "select distinct p.name, v.version_label, '%3$s', " +
                "r.id, r.name, null, null, null, null, null as flow_id, " +
                "r.row_id " +
                "from %1$s_agent_deploy d " +
                "inner join %1$s_flow f on f.id = d.flow_id " +
                "inner join %1$s_project_version_depends dp on dp.project_version_id = f.project_version_id " +
                "inner join %1$s_project_version v on v.id = dp.target_project_version_id " +
                "inner join %1$s_project p on p.id = v.project_id " +
                "inner join %1$s_resource r on r.project_version_id = v.id " +
                "where d.agent_id = ? and r.deleted=0 order by 5 "    ,
                tablePrefix, AgentDeploymentSummary.TYPE_FLOW, AgentDeploymentSummary.TYPE_RESOURCE), 
                new ISqlRowMapper<AgentDeploymentSummary>() {
                    public AgentDeploymentSummary mapRow(Row row) {
                        AgentDeploymentSummary summary = new AgentDeploymentSummary();
                        summary.setProjectName(row.getString("project_name"));
                        summary.setType(row.getString("type"));
                        summary.setName(row.getString("name"));
                        summary.setId(row.getString("id"));
                        summary.setStatus(row.getString("status"));
                        summary.setStartType(row.getString("start_type"));
                        summary.setLogLevel(row.getString("log_level"));
                        summary.setStartExpression(row.getString("start_expression"));
                        summary.setArtifactId(row.getString("flow_id", false));
                        summary.setRowId(row.getString("row_id", false));
                        summary.setProjectVersionLabel(row.getString("version_label", false));
                        return summary;
                    }
                }, agentId, agentId, agentId);
    }

    @Override
    public List<AgentResourceSetting> findMostRecentDeployedResourceSettings(String agentId, String resourceId) {
        List<AgentResourceSetting> resourceSettings = new ArrayList<AgentResourceSetting>();
        
        final String MOST_RECENT_DEPLOYED_RESOURCE_SETTING_SQL = 
                "select \n" + 
                "   ars.resource_id\n" + 
                "   , ars.name\n" + 
                "   , ars.value\n" + 
                "from\n" + 
                "   %1$s_agent_resource_setting ars\n" + 
                "   inner join %1$s_resource oldres\n" + 
                "      on ars.resource_id = oldres.id\n" + 
                "   inner join %1$s_resource newres\n" + 
                "      on oldres.row_id = newres.row_id\n" + 
//                "      and newres.id = ars.resource_id\n" + 
                "where\n" + 
                "   ars.agent_id='%2$s'\n" + 
                "   and newres.id='%3$s'\n" + 
                "   and ars.create_time =\n" + 
                "   (\n" + 
                "      select max(ars2.create_time)\n" + 
                "      from %1$s_agent_resource_setting ars2\n" + 
                "      where ars2.name = ars.name\n" + 
                "   )";
        
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        List<Row> ids = template.query(String.format(MOST_RECENT_DEPLOYED_RESOURCE_SETTING_SQL, tablePrefix, agentId, resourceId));
        for (Row row : ids) {
            AgentResourceSetting resourceSetting = new AgentResourceSetting();
            resourceSetting.setAgentId(agentId);
            resourceSetting.setResourceId(row.getString("resource_id"));
            resourceSetting.setName(row.getString("name"));
            resourceSetting.setValue(row.getString("value"));
            resourceSettings.add(resourceSetting);
        }
        return resourceSettings;
    }
    
    protected void refreshAgentResourceSettings(Agent agent) {
        Map<String, Object> settingParams = new HashMap<String, Object>();
        settingParams.put("agentId", agent.getId());
        @SuppressWarnings("unchecked")
        List<AgentResourceSetting> settings = (List<AgentResourceSetting>) findSettings(
                AgentResourceSetting.class, settingParams);
        agent.setAgentResourceSettings(settings);
    }

    protected void refreshAgentDeployments(Agent agent) {
        Map<String, Object> settingParams = new HashMap<String, Object>();
        settingParams.put("agentId", agent.getId());
        List<AgentDeploy> deployments = persistenceManager.find(AgentDeploy.class,
                settingParams, null, null, tableName(AgentDeploy.class));
        List<AgentDeploy> list = new ArrayList<>(deployments.size());
        for (AgentDeploy agentDeployment : deployments) {
            refreshAgentDeploymentRelations(agentDeployment);
            list.add(agentDeployment);
        }
        agent.setAgentDeployments(list);
    }

    protected void refreshAgentDeploymentRelations(AgentDeploy agentDeployment) {
        if (agentDeployment != null) {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("agentDeploymentId", agentDeployment.getId());
            agentDeployment.setAgentDeploymentParms(
                    persistenceManager.find(AgentFlowDeployParm.class, params, null, null,
                            tableName(AgentFlowDeployParm.class)));
        }
    }
}
