package org.jumpmind.metl.core.runtime;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jumpmind.metl.core.model.Execution;
import org.jumpmind.metl.core.model.FlowName;

public class StandaloneTestFlowRunner extends StandaloneFlowRunner {

    public StandaloneTestFlowRunner(String configFile) {
        setConfigSqlScript(configFile);
        setIncludeRegularFlows(false);
        setIncludeTestFlows(true);
        System.setProperty("h2.baseDir", "working");
        setLogDir("working/logs");
    }

    public Collection<Object[]> getFlowAsTestParams() {
        List<FlowName> flows = getFlows();
        List<Object[]> params = new ArrayList<>();
        for (FlowName flowName : flows) {
            params.add(new Object[] { flowName });
        }
        return params;
    }

    public void testFlow(FlowName flow) throws Exception {
        String executionId = runFlow(flow, true);
        Execution execution = findExecution(executionId);
        if (execution.isNotSuccess()) {
            fail(getFailureMessage(execution));
        }
    }

}
