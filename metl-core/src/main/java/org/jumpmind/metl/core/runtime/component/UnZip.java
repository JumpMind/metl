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
package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.IStreamable;
import org.jumpmind.metl.core.runtime.resource.LocalFile;
import org.jumpmind.properties.TypedProperties;

public class UnZip extends AbstractComponentRuntime {
    public static final String TYPE = "UnZip";

    public final static String SETTING_TARGET_RESOURCE = "target.resource";

    public final static String SETTING_TARGET_SUB_DIR = "target.sub.dir";

    public static final String SETTING_MUST_EXIST = "must.exist";

    public final static String SETTING_DELETE_ON_COMPLETE = "delete.on.complete";

    public final static String SETTING_ENCODING = "encoding";

    String targetDirName;

    boolean mustExist;

    String encoding = "UTF-8";

    boolean deleteOnComplete = false;

    boolean targetSubDir = false;

    @Override
    protected void start() {
        TypedProperties properties = getTypedProperties();
        deleteOnComplete = properties.is(SETTING_DELETE_ON_COMPLETE, deleteOnComplete);
        String targetResourceId = properties.get(SETTING_TARGET_RESOURCE);
        IResourceRuntime targetResource = context.getDeployedResources().get(targetResourceId);
        if (!(targetResource instanceof LocalFile)) {
            throw new MisconfiguredException("The target resource must be a local file resource");
        }

        targetDirName = targetResource.getResourceRuntimeSettings().get(LocalFile.LOCALFILE_PATH);
        if (isBlank(targetDirName)) {
            throw new MisconfiguredException("The target resource %s needs its path set", targetResource.getResource().getName());
        }
        targetSubDir = properties.is(SETTING_TARGET_SUB_DIR, targetSubDir);
        mustExist = properties.is(SETTING_MUST_EXIST, mustExist);
        encoding = properties.get(SETTING_ENCODING, encoding);
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        List<String> files = inputMessage.getPayload();
        ArrayList<String> filePaths = new ArrayList<String>();

        IStreamable streamable = getResourceReference();
        String path = getResourceRuntime().getResourceRuntimeSettings().get(LocalFile.LOCALFILE_PATH);

        for (String fileName : files) {
            log(LogLevel.INFO, "Preparing to extract file : %s", fileName);
            File file = getNewFile(fileName);
            if (mustExist && !file.exists()) {
                throw new IoException(String.format("Could not find file to extract: %s", fileName));
            }
            if (file.exists()) {
                try {
                    ZipFile zipFile = getNewZipFile(file);
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        String finalPath = targetSubDir
                                ? targetDirName + File.separator + FilenameUtils.removeExtension(file.getName()) + File.separator
                                : targetDirName + File.separator;

                        for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
                            ZipEntry entry = e.nextElement();
                            log(LogLevel.INFO, entry.getName());

                            if (!entry.isDirectory()) {
                                out = streamable.getOutputStream(finalPath + entry.getName(), false);
                                in = zipFile.getInputStream(entry);
                                IOUtils.copy(in, out);
                                filePaths.add(getNewFile(path + finalPath, entry.getName()).getAbsolutePath());
                            }
                        }
                    } finally {
                        IOUtils.closeQuietly(in);
                        IOUtils.closeQuietly(out);
                        zipFile.close();
                    }
                    if (deleteOnComplete) {
                        FileUtils.deleteQuietly(file);
                    }
                } catch (IOException e) {
                    throw new IoException(e);
                }
                log(LogLevel.INFO, "Extracted %s", fileName);
                getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
            }
        }
        callback.sendMessage(null, filePaths, unitOfWorkBoundaryReached);
    }

    protected File getNewFile(String file) {
        return new File(file);
    }

    protected File getNewFile(String path, String child) {
        return new File(path, child);
    }

    protected ZipFile getNewZipFile(File file) throws IOException {
        return new ZipFile(file);
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

}
