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

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

abstract public class ContentMessage<T extends Serializable> extends Message {

    private static final long serialVersionUID = 1L;
    
    T payload;

    public ContentMessage(String originatingStepId, T payload) {
        super(originatingStepId);
        this.payload = payload;
    }

    public ContentMessage(String originatingStepId) {
        super(originatingStepId);
    }
    
    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
    
    public String getTextFromPayload() {
        StringBuilder b = new StringBuilder();
        if (payload instanceof Collection) {
            Iterator<?> i = ((Collection<?>)payload).iterator();
            while (i.hasNext()) {
                Object obj = i.next();
                b.append(obj);
                if (i.hasNext()) {
                    b.append(System.getProperty("line.separator"));
                }
            }
        } else if (payload instanceof CharSequence) {
            b.append((CharSequence)payload);
        }
        return b.toString();
    }
    

}
