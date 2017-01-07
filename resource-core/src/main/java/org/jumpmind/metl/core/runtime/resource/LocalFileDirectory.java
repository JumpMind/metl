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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Resource;

public class LocalFileDirectory extends AbstractDirectory {

    String basePath;

    public LocalFileDirectory(Resource resource, String basePath, boolean mustExist) {
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

    @Override
    public FileInfo listFile(String relativePath) {
        File file = new File(basePath, relativePath);
        if (file.exists()) {
            return new FileInfo(relativePath, file.isDirectory(), file.lastModified(), file.length());
        } else {
            return null;
        }
    }

    @Override
    public FileInfo listFile(String relativePath, boolean closeSession) {
        return listFile(relativePath);
    }

    @Override
    public List<FileInfo> listFiles(String... relativePaths) {
        List<FileInfo> list = new ArrayList<>();
        if (relativePaths != null && relativePaths.length > 0) {
            for (String relativePath : relativePaths) {
                File file = new File(basePath, relativePath);
                if (file.isFile() && file.exists()) {
                    list.add(new FileInfo(relativePath, false, file.lastModified(), file.length()));
                } else {
                    list.addAll(listFiles(new File(basePath, relativePath)));
                }
            }
        } else {
            list.addAll(listFiles(new File(basePath)));
        }
        return list;
    }

    @Override
    public List<FileInfo> listFiles(boolean closeSession, String... relativePaths) {
        return listFiles(relativePaths);
    }

    @Override
    public void copyFile(String fromFilePath, String toFilePath) {
        try {
            File fromFile = new File(basePath, fromFilePath);
            File toFile = new File(basePath, toFilePath);
            toFile.getParentFile().mkdirs();
            toFile.delete();
            FileUtils.copyFile(fromFile, toFile);
        } catch (IOException e) {
            throw new IoException(e);
        }
    }

    @Override
    public void copyFile(String fromFilePath, String toFilePath, boolean closeSession) {
        copyFile(fromFilePath, toFilePath);
    }

    @Override
    public void moveFile(String fromFilePath, String toFilePath) {
        try {
            File fromFile = new File(basePath, fromFilePath);
            File toFile = new File(basePath, toFilePath);
            toFile.getParentFile().mkdirs();
            toFile.delete();
            FileUtils.copyFile(fromFile, toFile);
            fromFile.delete();
        } catch (IOException e) {
            throw new IoException(e);
        }
    }

    @Override
    public void moveFile(String fromFilePath, String toFilePath, boolean closeSession) {
        moveFile(fromFilePath, toFilePath);
    }

    @Override
    public boolean renameFile(String fromFilePath, String toFilePath) {
        File fromFile = new File(basePath, fromFilePath);
        File toFile = new File(basePath, toFilePath);
        toFile.getParentFile().mkdirs();
        toFile.delete();
        return fromFile.renameTo(toFile);
    }

    @Override
    public boolean renameFile(String fromFilePath, String toFilePath, boolean closeSession) {
        return renameFile(fromFilePath, toFilePath);
    }

    @Override
    public void copyToDir(String fromFilePath, String toDirPath) {
        try {
            File fromFile = new File(basePath, fromFilePath);
            File toDir = new File(basePath, toDirPath);
            File toFile = new File(toDir, fromFile.getName());
            toFile.delete();
            FileUtils.copyFileToDirectory(fromFile, toDir, true);
        } catch (IOException e) {
            throw new IoException(e);
        }
    }

    @Override
    public void copyToDir(String fromFilePath, String toDirPath, boolean closeSession) {
        copyToDir(fromFilePath, toDirPath);
    }

    @Override
    public void moveToDir(String fromFilePath, String toDirPath) {
        try {
            File fromFile = new File(basePath, fromFilePath);
            File toDir = new File(basePath, toDirPath);
            File toFile = new File(toDir, fromFile.getName());
            toFile.delete();
            FileUtils.moveFileToDirectory(fromFile, toDir, true);
        } catch (IOException e) {
            throw new IoException(e);
        }
    }

    @Override
    public void moveToDir(String fromFilePath, String toDirPath, boolean closeSession) {
        moveToDir(fromFilePath, toDirPath);
    }    

    protected List<FileInfo> listFiles(File dir) {
        String fileSeparator = System.getProperty("file.separator");
        List<FileInfo> list = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                String normPath = FilenameUtils.normalize(file.getAbsolutePath());
                String normBasePath = FilenameUtils.normalize(basePath);
                int index = normPath.indexOf(normBasePath);
                if (index >= 0) {
                    if (!normBasePath.endsWith(fileSeparator)) {
                        index++;
                    }
                    normPath = normPath.substring(index + normBasePath.length());
                }
                list.add(new FileInfo(normPath, file.isDirectory(), file.lastModified(), file.length()));
            }
        }
        return list;
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
            if (mustExist) {
                throw new IoException(e);
            } else {                
                return null;
            }
        }
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist, boolean closeSession) {
        return getInputStream(relativePath, mustExist);
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
    public OutputStream getOutputStream(String relativePath, boolean mustExist, boolean closeSession, boolean append) {
        try {
            return new FileOutputStream(toFile(relativePath, mustExist), append);
        } catch (FileNotFoundException e) {
            throw new IoException(e);
        }
    }

    @Override
    public void close() {
    }
    
    @Override
    public void close(boolean success) {
    }

    @Override
    public boolean delete(String relativePath) {
        return FileUtils.deleteQuietly(toFile(relativePath, false));
    }

    @Override
    public boolean delete(String relativePath, boolean closeSession) {
        return delete(relativePath);
    }

    @Override
    public boolean supportsDelete() {
        return true;
    }

    @Override
    public String toString() {
        return basePath;
    }

    @Override
    public void connect() {
    }
}
