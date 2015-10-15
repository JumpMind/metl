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
package org.jumpmind.metl.core.runtime;

import org.jumpmind.metl.core.util.NameValue;
import org.jumpmind.util.LinkedCaseInsensitiveMap;

public class EntityData extends LinkedCaseInsensitiveMap<Object> {

    private static final long serialVersionUID = 1L;

    public enum ChangeType {
        ADD, CHG, DEL
    };

    ChangeType changeType = ChangeType.ADD;

    public EntityData() {
    }

    public EntityData(NameValue... nameValues) {
        if (nameValues != null) {
            for (NameValue nameValue : nameValues) {
                put(nameValue.getName(), nameValue.getValue());
            }
        }
    }

    public EntityData copy() {
        return (EntityData) this.clone();
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

}
