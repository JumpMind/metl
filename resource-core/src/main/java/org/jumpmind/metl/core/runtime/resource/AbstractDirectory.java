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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

abstract public class AbstractDirectory implements IDirectory {

    public AbstractDirectory() {
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
        return false;
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist) {
        return null;
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist, boolean closeSession) {
        return null;
    }
    
    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist, boolean closeSession, Map<String, String> headers,
            Map<String, String> parameters) {
        return getInputStream(relativePath, mustExist, closeSession);
    }

    @Override
    public boolean supportsOutputStream() {
        return false;
    }
    
    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist, boolean closeSession, boolean append, Map<String, String> headers,
            Map<String, String> parameters) {
        return getOutputStream(relativePath, mustExist, closeSession, append);
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist) {
        return null;
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist, boolean closeSession, boolean append) {
        return null;
    }
    
    @Override
    public void close(boolean success) {
    }

    @Override
    public void close() {
        close(true);
    }

    @Override
    public boolean delete(String relativePath) {
        return false;
    }

    @Override
    public boolean delete(String relativePath, boolean closeSession) {
        return false;
    }

    @Override
    public boolean supportsDelete() {
        return false;
    }

    @Override
    public List<FileInfo> listFiles(String... relativePaths) {
        return null;
    }

    @Override
    public List<FileInfo> listFiles(boolean closeSession, String... relativePaths) {
        return null;
    }

    @Override
    public FileInfo listFile(String relativePath) {
        return null;
    }

    @Override
    public FileInfo listFile(String relativePath, boolean closeSession) {
        return null;
    }

    @Override
    public void copyFile(String fromFilePath, String toFilePath) {
    }

    @Override
    public void copyFile(String fromFilePath, String toFilePath, boolean closeSession) {
    }

    @Override
    public void moveFile(String fromFilePath, String toFilePath) {
    }

    @Override
    public void moveFile(String fromFilePath, String toFilePath, boolean closeSession) {
    }

    @Override
    public void copyToDir(String fromFilePath, String toDirPath) {
    }

    @Override
    public void copyToDir(String fromFilePath, String toDirPath, boolean closeSession) {
    }

    @Override
    public void moveToDir(String fromFilePath, String toDirPath) {
    }

    @Override
    public void moveToDir(String fromFilePath, String toDirPath, boolean closeSession) {
    }

    @Override
    public boolean renameFile(String fromFilePath, String toFilePath) {
        return false;
    }

    @Override
    public boolean renameFile(String fromFilePath, String toFilePath, boolean closeSession) {
        return false;
    }

    @Override
    public void connect() {
    }

}
