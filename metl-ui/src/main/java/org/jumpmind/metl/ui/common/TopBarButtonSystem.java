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
package org.jumpmind.metl.ui.common;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.GlobalSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.UIScope;

@Component
@UIScope
@Order(value=10)
public class TopBarButtonSystem extends TopBarButton {

    private static final long serialVersionUID = 1L;

    @Autowired
    protected ApplicationContext context;

    public TopBarButtonSystem() {
        super(FontAwesome.WARNING);
    }
    
    @PostConstruct
    public void init() {
        String caption = "";
        GlobalSetting setting = context.getOperationsService().findGlobalSetting(GlobalSetting.SYSTEM_TEXT);
        if (setting != null) {
            caption = setting.getValue();
        }

        if (StringUtils.isNotBlank(caption)) {
            setCaption(caption);
            setHtmlContentAllowed(true);
        } else {
            setVisible(false);
        }
    }

}
