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
package org.jumpmind.metl.core.runtime.flow;

import java.util.Collection;

import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.runtime.StandaloneTestFlowRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DataDiffFlowTest {

    static StandaloneTestFlowRunner standaloneFlowRunner;

    FlowName flow;

    public DataDiffFlowTest(FlowName flow) {
        this.flow = flow;
    }

    @Test
    public void testFlow() throws Exception {
        standaloneFlowRunner.testFlow(flow);
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> getFlows() throws Exception {
        standaloneFlowRunner = new StandaloneTestFlowRunner("/datadiff-flow-test-config.sql");
        return standaloneFlowRunner.getFlowAsTestParams();
    }

}
