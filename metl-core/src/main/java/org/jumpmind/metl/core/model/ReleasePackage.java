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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReleasePackage extends AbstractNamedObject {

    private static final long serialVersionUID = 1L;
    
    protected Date releaseDate;
    
    protected boolean released;
    
    protected String name;
    
    protected String versionLabel;
    
    protected List<Rppv> projectVersions;

    public ReleasePackage() {
        this.projectVersions = new ArrayList<Rppv>();
    }

    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }

    @Override
    public void setName(String name) {
        this.name = name;        
    }

    @Override
    public String getName() {
        return this.name;
    }
    
    public void setReleased(boolean realeased) {
        this.released = realeased;
    }
    
    public boolean isReleased() {
        return released;
    }
    
    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }
    
    public Date getReleaseDate() {
        return releaseDate;
    }
    
    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }
    
    public String getVersionLabel() {
        return versionLabel;
    }
    
    public List<Rppv> getProjectVersions() {
        return projectVersions;
    }

    public void setProjectVersions(List<Rppv> projectVersions) {
        this.projectVersions = projectVersions;
    }
}
