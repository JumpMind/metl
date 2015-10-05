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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Resource;

public class LocalFileStreamable implements IStreamable {

    String basePath;

    public LocalFileStreamable(Resource resource, String basePath, boolean mustExist) {
        this.basePath = basePath;
    }

    @Override
    public boolean requiresContentLength() {
        return false;
    }

    @Override
    public void setContentLength(int length) {
    }

    @Override
    public boolean supportsInputStream() {
        return true;
    }

    protected File toFile(String relativePath, boolean mustExist) {
        File file;
    	if (StringUtils.isEmpty(basePath)) {
    		file = new File(relativePath);
    	} else {
    		file = new File(basePath, relativePath);
    	}
        if (!file.exists()) {
            if (!mustExist) {
            	if (file.getParentFile() != null) {
            		file.getParentFile().mkdirs();
            	} 
            } else {
                throw new IoException("Could not find " + file.getAbsolutePath());
            }
        }
        return file;
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist) {
        try {
            return new FileInputStream(toFile(relativePath, mustExist));
        } catch (FileNotFoundException e) {
            throw new IoException(e);
        }
    }

    @Override
    public boolean supportsOutputStream() {
        return true;
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist) {
        try {
            return new FileOutputStream(toFile(relativePath, mustExist));
        } catch (FileNotFoundException e) {
            throw new IoException(e);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean delete(String relativePath) {
        return FileUtils.deleteQuietly(toFile(relativePath, false));
    }

    @Override
    public boolean supportsDelete() {
        return true;
    }

    @Override
    public String toString() {
        return basePath;
    }
}
