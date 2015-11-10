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
package org.jumpmind.metl.core.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AbstractObjectLastUpdateTimeDescSorter implements Comparator<AbstractObject> {

    @Override
    public int compare(AbstractObject o1, AbstractObject o2) {
        if (o2.getLastUpdateTime() != null && o1.getLastUpdateTime() != null) {
            return o2.getLastUpdateTime().compareTo(o1.getLastUpdateTime());
        } else {
            return 0;
        }
    }

    public static void sort(List<? extends AbstractObject> list) {
        Collections.sort(list, new AbstractObjectLastUpdateTimeDescSorter());
    }

}
