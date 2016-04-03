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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jumpmind.metl.core.runtime.component.definition.XMLSetting.Type;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SettingDefinition {

    int order() default 0;
    
    Type type();

    boolean required() default false;

    String[] choices() default {};

    String defaultValue() default "";

    String label() default "";

    boolean visible() default true;

    /*
     * When set, this setting must be provided by the user/caller of the object
     * that defined the setting. For example, a file resource needs to be
     * provided the name of the file or an SMTP resource needs to be provided
     * the subject and to list for an email.
     */
    boolean provided() default false;

}
