package org.jumpmind.metl.core.runtime.flow;

import java.util.Collection;

import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.runtime.StandaloneTestFlowRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DeduperFlowTest {

    static StandaloneTestFlowRunner standaloneFlowRunner;

    FlowName flow;

    public DeduperFlowTest(FlowName flow) {
        this.flow = flow;
    }

    @Test
    public void testFlow() throws Exception {
        standaloneFlowRunner.testFlow(flow);
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> getFlows() throws Exception {
        standaloneFlowRunner = new StandaloneTestFlowRunner("/deduper-flow-test-config.sql");
        return standaloneFlowRunner.getFlowAsTestParams();
    }

}
