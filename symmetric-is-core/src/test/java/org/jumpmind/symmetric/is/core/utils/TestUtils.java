package org.jumpmind.symmetric.is.core.utils;

import java.io.File;
import java.util.Date;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.BasicDataSourceFactory;
import org.jumpmind.db.util.BasicDataSourcePropertyConstants;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.Component;
import org.jumpmind.symmetric.is.core.config.ComponentFlow;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNodeLink;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.data.AgentData;
import org.jumpmind.symmetric.is.core.config.data.AgentDeploymentData;
import org.jumpmind.symmetric.is.core.config.data.ComponentData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowVersionData;
import org.jumpmind.symmetric.is.core.config.data.ComponentVersionData;
import org.jumpmind.symmetric.is.core.config.data.FolderData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;
import org.jumpmind.symmetric.is.core.runtime.component.NoOpProcessorComponent;

public class TestUtils {

    public static IDatabasePlatform createDatabasePlatform() throws Exception {
        final String DB_DIR = "build/dbs";
        File dir = new File(DB_DIR);
        if (dir.exists()) {
            FileUtils.deleteDirectory(new File(DB_DIR));
        }
        TypedProperties properties = new TypedProperties();
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_DRIVER, "org.h2.Driver");
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_URL, "jdbc:h2:file:"
                + DB_DIR + "/testdb");
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_USER, "jumpmind");
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_PASSWORD, "jumpmind");
        DataSource ds = BasicDataSourceFactory.create(properties);
        return JdbcDatabasePlatformFactory.createNewPlatformInstance(ds, new SqlTemplateSettings(),
                false);
    }
    
    public static Folder createFolder(String name) {
    	
    	FolderData data = new FolderData();
    	data.setCreateBy("Test");
    	data.setCreateTime(new Date());
    	data.setName(name);
    	data.setId(name);
    	Folder folder = new Folder(data);
    	
    	return folder;
    }
    
    public static ComponentFlowVersion createFlow(String name, Folder folder) {
    	
    	ComponentFlow flow = createComponentFlow(name, folder);
    	ComponentFlowVersionData data = new ComponentFlowVersionData();
    	data.setComponentFlowId(flow.getId());
    	data.setCreateBy("Test");
    	data.setCreateTime(new Date());
    	data.setId(name);
    	data.setVersionName(name);
    	ComponentFlowVersion flowVersion = new ComponentFlowVersion(flow, data);

    	return flowVersion;
    }
    
    public static void addNodeToComponentFlow(ComponentFlowVersion flow, ComponentFlowNode node) {    	
    	flow.getComponentFlowNodes().add(node);    	
    }
    
    public static ComponentFlowNode createNoOpProcessorComponentFlowNode(ComponentFlowVersion flowVersion, 
    		String name, Folder folder) {

    	Component component = createComponent(NoOpProcessorComponent.type, false);
    	ComponentVersion componentVersion = createComponentVersion(component, name, null, null);
    	ComponentFlowNodeData data = new ComponentFlowNodeData();
    	data.setComponentFlowVersionId(flowVersion.getId());
    	data.setComponentVersionId(componentVersion.getId());
    	data.setCreateBy("Test");
    	data.setCreateTime(new Date());
    	data.setId(name);
    	data.setLastModifyBy("Test");
    	data.setLastModifyTime(new Date());
    	
    	ComponentFlowNode node = new ComponentFlowNode(componentVersion, data);
    	
    	return node;
    }
    
    public static Agent createAgent(String name, Folder folder) {
    	
    	AgentData data = new AgentData();
    	data.setCreateBy("Test");
    	data.setCreateTime(new Date());
    	data.setFolderId(folder.getId());
    	data.setHeartbeatTime(new Date());
    	data.setHost("localhost");
    	data.setId(name);
    	data.setLastModifyBy("Test");
    	data.setLastModifyTime(new Date());
    	data.setLastStartTime(new Date());
    	data.setName(name);
    	//TODO: determine what these will eventually be for
//    	data.setStartMode(startMode);
//    	data.setStatus(status);
    	
    	Agent agent = new Agent(folder, data, null);
    	
    	return agent;
    }
    
    public static AgentDeployment createAgentDeployment(String name, Agent agent, ComponentFlowVersion flow) {
    	
    	AgentDeploymentData data = new AgentDeploymentData();
    	data.setAgentId(agent.getId());
    	data.setComponentFlowVersionId(flow.getId());
    	data.setCreateBy("Test");
    	data.setCreateTime(new Date());
    	data.setId(name);
    	data.setLastModifyBy("Test");
    	data.setLastModifyTime(new Date());
    	
    	AgentDeployment deployment = new AgentDeployment(flow, data);
    	
    	return deployment;
    			
    }

    public static ComponentFlowNodeLink createComponentLink(ComponentFlowNode srcNode, ComponentFlowNode destNode) {
    	
    	ComponentFlowNodeLink link = new ComponentFlowNodeLink(srcNode.getId(), destNode.getId());
    	return link;
    }
    
    private static ComponentFlow createComponentFlow(String name, Folder folder) {
    	
    	ComponentFlowData data = new ComponentFlowData();
    	data.setCreateBy("Test");
    	data.setCreateTime(new Date());
    	data.setFolderId(folder.getId());
    	data.setId("ComponentFlowId");
    	data.setName("ComponentFlow");
    	ComponentFlow componentFlow = new ComponentFlow(folder, data);
    	
    	return componentFlow;
    }
    
    private static Component createComponent(String type, boolean shared) {

    	ComponentData data = new ComponentData(type, shared);
    	Component component = new Component(data);
    	component.setCreateBy("Test");
    	component.setCreateTime(new Date());
    	component.setLastModifyBy("Test");
    	component.setCreateTime(new Date());
    	
    	return component;
    }
    
    private static ComponentVersion createComponentVersion(Component component, 
    		String name, Connection connection, SettingData... settings) {
    	
    	ComponentVersionData data = new ComponentVersionData();
    	data.setComponentId(component.getId());
    	data.setCreateBy("Test");
    	data.setCreateTime(new Date());
    	data.setId(name);
    	data.setName(name);
    	data.setLastModifyBy("Test");
    	data.setLastModifyTime(new Date());
    	//TODO: allow passing in of a model
    	data.setInputModelVersiondId(null);
    	data.setOutputModelVersionId(null);
    	
    	ComponentVersion componentVersion = new ComponentVersion(component, connection, data, settings);
    	
    	return componentVersion;	
    }
    
}
