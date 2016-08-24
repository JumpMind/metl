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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.FileInfo;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.properties.TypedProperties;

public class UnZip extends AbstractComponentRuntime {
    public static final String TYPE = "UnZip";

    public final static String SETTING_TARGET_RESOURCE = "target.resource";

    public final static String SETTING_SOURCE_RESOURCE = "source.resource";

    public final static String SETTING_TARGET_SUB_DIR = "target.sub.dir";

    public final static String SETTING_TARGET_RELATIVE_PATH = "target.relative.path";

    public static final String SETTING_MUST_EXIST = "must.exist";

    public final static String SETTING_DELETE_ON_COMPLETE = "delete.on.complete";

    public final static String SETTING_EXTRACT_EMPTY_FILES = "extract.empty.files";

    public final static String SETTING_ENCODING = "encoding";
    
    public final static String SETTING_OVERWRITE = "overwrite";

    boolean mustExist;

    String encoding = "UTF-8";

    boolean deleteOnComplete = true;

    boolean targetSubDir = false;

    boolean extractEmptyFiles = true;
    
    boolean overwrite = true;

    IDirectory sourceDir;

    IDirectory targetDir;

    String targetRelativePath;

    @Override
    public void start() {
        TypedProperties properties = getTypedProperties();

        deleteOnComplete = properties.is(SETTING_DELETE_ON_COMPLETE, deleteOnComplete);

        String sourceResourceId = properties.get(SETTING_SOURCE_RESOURCE);
        IResourceRuntime sourceResource = context.getDeployedResources().get(sourceResourceId);
        if (sourceResource == null) {
            throw new MisconfiguredException("The source resource must be defined");
        } else {
            sourceDir = sourceResource.reference();
        }

        String targetResourceId = properties.get(SETTING_TARGET_RESOURCE);
        IResourceRuntime targetResource = context.getDeployedResources().get(targetResourceId);
        if (targetResource == null) {
            throw new MisconfiguredException("The target resource must be defined");
        } else {
            targetDir = targetResource.reference();
        }

        targetRelativePath = properties.get(SETTING_TARGET_RELATIVE_PATH, "");
        overwrite = properties.is(SETTING_OVERWRITE, overwrite);
        targetSubDir = properties.is(SETTING_TARGET_SUB_DIR, targetSubDir);
        mustExist = properties.is(SETTING_MUST_EXIST, mustExist);
        extractEmptyFiles = properties.is(SETTING_EXTRACT_EMPTY_FILES, extractEmptyFiles);
        encoding = properties.get(SETTING_ENCODING, encoding);

    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof TextMessage) {
            List<String> files = ((TextMessage)inputMessage).getPayload();
            ArrayList<String> filePaths = new ArrayList<String>();
            for (String fileName : files) {
                log(LogLevel.INFO, "Preparing to extract file : %s", fileName);
                FileInfo sourceZipFile = sourceDir.listFile(fileName);
                if (mustExist && sourceZipFile == null) {
                    throw new IoException(String.format("Could not find file to extract: %s", fileName));
                }
                if (sourceZipFile != null) {
                    File unzipDir = new File(LogUtils.getLogDir(), "unzip");
                    unzipDir.mkdirs();

                    File localZipFile = copyZipLocally(fileName, unzipDir);
                    ZipFile zipFile = getNewZipFile(localZipFile);
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        String targetDirNameResolved = resolveParamsAndHeaders(targetRelativePath, inputMessage);
                        if (targetSubDir) {
                            targetDirNameResolved = targetDirNameResolved + "/" + FilenameUtils.removeExtension(new FileInfo(fileName, false, 0, 0).getName());
                        }
                        for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
                            ZipEntry entry = e.nextElement();
                            if (!entry.isDirectory() && (extractEmptyFiles || entry.getSize() > 0)) {
                                String relativePathToEntry = targetDirNameResolved + "/" + entry.getName();
                                if (overwrite || targetDir.listFile(relativePathToEntry) == null) {
                                    info("Unzipping %s", entry.getName());
                                    out = targetDir.getOutputStream(relativePathToEntry, false);
                                    in = zipFile.getInputStream(entry);
                                    IOUtils.copy(in, out);
                                    filePaths.add(relativePathToEntry);
                                } else if (!overwrite) {
                                    info("Not unzipping %s.  It already exists and the override property is not enabled", entry.getName());
                                }
                            }
                        }
                    } catch (IOException e) {
                        throw new IoException(e);
                    } finally {
                        IOUtils.closeQuietly(in);
                        IOUtils.closeQuietly(out);
                        IOUtils.closeQuietly(zipFile);
                        FileUtils.deleteQuietly(localZipFile);
                    }
                    if (deleteOnComplete) {
                        sourceDir.delete(fileName);
                    }

                    log(LogLevel.INFO, "Extracted %s", fileName);
                    getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                }
            }
            if (filePaths.size() > 0) {
                callback.sendTextMessage(null, filePaths);
            }
        }
    }

    protected File copyZipLocally(String fileName, File unzipDir) {
        InputStream is = null;
        FileOutputStream os = null;
        try {
            is = sourceDir.getInputStream(fileName, true);
            if (is != null) {
                File localZipFile = new File(unzipDir, UUID.randomUUID().toString() + ".zip");
                os = new FileOutputStream(localZipFile);
                IOUtils.copy(is, os);
                return localZipFile;
            } else {
                String msg = String.format("Failed to open %s.", fileName);
                throw new IoException(msg);
            }
        } catch (IOException e) {
            throw new IoException(e);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    protected ZipFile getNewZipFile(File file) {
        try {
            return new ZipFile(file);
        } catch (IOException e) {
            throw new IoException(e);
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

}
