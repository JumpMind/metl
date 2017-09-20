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
package org.jumpmind.metl.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.component.NoOp;
import org.junit.Assert;

public class TestUtils {
	public static final String XML_BASIC = "Base.xml";
	public static final String XML_SINGLE_LINE = "SingleLine.xml";
	
    public static void main(String[] args) {
        System.out.println(Integer.MAX_VALUE);
    }
    public static Folder createFolder(String name) {

        Folder data = new Folder();
        data.setCreateBy("Test");
        data.setCreateTime(new Date());
        data.setName(name);
        data.setId(name);
        return data;
    }

    public static Flow createFlow(String name, Folder folder) {
        Flow flow = new Flow(folder);
        flow.setCreateBy("Test");
        flow.setCreateTime(new Date());
        flow.setFolderId(folder != null ? folder.getId() : null);
        flow.setId("FlowId");
        flow.setName("Flow");
        flow.setId(name);
        return flow;
    }

    public static void addStepToFlow(Flow flow, FlowStep step) {
        flow.getFlowSteps().add(step);
    }

    public static FlowStep createNoOpProcessorFlowStep(Flow flow, String name,
            Folder folder) {
        Component component = createComponent(NoOp.TYPE, false, null, null, null,
                null, null, (Setting[]) null);
        FlowStep step = new FlowStep(component);
        step.setFlowId(flow.getId());
        step.setCreateBy("Test");
        step.setCreateTime(new Date());
        step.setId(name);
        step.setLastUpdateBy("Test");
        step.setLastUpdateTime(new Date());
        step.setComponent(component);
        return step;
    }

    public static Agent createAgent(String name, Folder folder) {

        Agent agent = new Agent(folder);
        agent.setCreateBy("Test");
        agent.setCreateTime(new Date());
        agent.setFolderId(folder.getId());
        agent.setHeartbeatTime(new Date());
        agent.setId(name);
        agent.setLastUpdateBy("Test");
        agent.setLastUpdateTime(new Date());
        agent.setLastStartTime(new Date());
        agent.setName(name);
        return agent;
    }

//    public static AgentDeployment createAgentDeployment(String name, Agent agent, Flow flow) {
//        AgentDeployment deployment = new AgentDeployment(flow);
//        ProjectVersion projectVersion = new ProjectVersion();
//        projectVersion.setVersionLabel("1.0");
//        deployment.setProjectVersion(projectVersion);
//        deployment.setAgentId(agent.getId());
//        deployment.setCreateBy("Test");
//        deployment.setCreateTime(new Date());
//        deployment.setId(name);
//        deployment.setLastUpdateBy("Test");
//        deployment.setLastUpdateTime(new Date());
//        return deployment;
//    }

    public static FlowStepLink createComponentLink(FlowStep srcStep, FlowStep destStep) {

        FlowStepLink link = new FlowStepLink(srcStep.getId(), destStep.getId());
        return link;
    }

    public static Component createComponent(String type, boolean shared) {
        Component component = new Component();
        component.setCreateBy("Test");
        component.setCreateTime(new Date());
        component.setLastUpdateBy("Test");
        component.setCreateTime(new Date());
        return component;
    }

    public static Component createComponent(String type, boolean shared, Resource resource,
            Model inputModelVersion, Model outputModelVersion,
            List<ComponentEntitySetting> entitySettings,
            List<ComponentAttribSetting> attributeSettings, Setting... settings) {
        Component component = new Component(resource,
                inputModelVersion, outputModelVersion, entitySettings, attributeSettings, settings);
        component.setType(type);
        component.setShared(shared);
        component.setCreateBy("Test");
        component.setCreateTime(new Date());
        component.setId("Test");
        component.setLastUpdateBy("Test");
        component.setLastUpdateTime(new Date());
        return component;
    }
    
    public static File getTestXMLFile(String fileName) {
    	ClassLoader classLoader = TestUtils.class.getClassLoader();
    	return new File(classLoader.getResource(fileName).getFile());
    }
    
    public static String getTestXMLFileContent(String fileName) {
    	try {
    		return FileUtils.readFileToString(getTestXMLFile(fileName));
    	}
    	catch (Exception e) {
    		return null;
    	}
    }
    
    public static void assertList(List<String> expected, List<String> actual, boolean isXML) {
    	assertNullNotNull(expected, actual);
    	if (expected != null && actual != null) {
    		assertEquals(expected.size(), actual.size());
    		for (int i = 0; i < expected.size(); i++) {
    			if (isXML) {
    				try {
    					assertNullNotNull(expected.get(i), actual.get(i));
    					if (expected.get(i).length() == 0 || actual.get(i).length() == 0) {
    						assertEquals(expected.get(i).length(), actual.get(i).length());
    					}
    					XMLUnit.setIgnoreWhitespace(true);
    					XMLAssert.assertXMLEqual(expected.get(i), actual.get(i));
    				}
    				catch (Exception e) {
    					Assert.fail("Unable to compare xml payloads" + e.getMessage());
    				}
    			}
    			else {
    				assertEquals(expected.get(i), actual.get(i));
    			}
    		}
    	}
    }
    
    public static void assertNullNotNull(Object expected, Object actual) {
    	if (expected == null && actual != null) {
    		fail();
    	}
    	else if (expected != null && actual == null) {
    		fail();
    	}
    }

}
