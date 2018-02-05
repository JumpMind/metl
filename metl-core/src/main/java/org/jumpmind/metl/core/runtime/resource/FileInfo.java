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
package org.jumpmind.metl.core.runtime.resource;

import org.apache.commons.io.FilenameUtils;

public class FileInfo {

    long size;
    String relativePath;
    boolean directory;
    long lastUpdated;
    String name;

    public FileInfo(String path, boolean directory, long lastUpdated, long size) {
        this.relativePath = path;
        this.directory = directory;
        this.lastUpdated = lastUpdated;
        this.size = size;
        name = FilenameUtils.getName(relativePath);
    }
    
    public long getSize() {
        return size;
    }
    
    public String getName() {
        return name;
    }
    
    public String getRelativePath() {
        return relativePath;
    }
    
    public boolean isDirectory() {
        return directory;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    @Override
    public String toString() {
        return relativePath;
    }

}
