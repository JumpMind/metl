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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.FileInfo;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.properties.TypedProperties;

public class Zip extends AbstractComponentRuntime {

    public static final String TYPE = "Zip";

    public final static String SETTING_TARGET_RESOURCE = "target.resource";

    public final static String SETTING_SOURCE_RESOURCE = "source.resource";

    public final static String SETTING_TARGET_RELATIVE_PATH = "target.relative.path";

    public static final String SETTING_MUST_EXIST = "must.exist";

    public final static String SETTING_DELETE_ON_COMPLETE = "delete.on.complete";

    public final static String SETTING_ENCODING = "encoding";

    IResourceRuntime sourceResource;
    
    IResourceRuntime targetResource;
    
    String sourceResourceId;
    
    String targetRelativePath;
    
    String targetResourceId;

    boolean mustExist;

    boolean deleteOnComplete = false;

    String encoding = "UTF-8";

    List<String> fileNames;

    @Override
    public void start() {
        
        TypedProperties properties = getTypedProperties();

        deleteOnComplete = properties.is(SETTING_DELETE_ON_COMPLETE, deleteOnComplete);
        encoding = properties.get(SETTING_ENCODING, encoding);
        fileNames = new ArrayList<String>();

        sourceResourceId = properties.get(SETTING_SOURCE_RESOURCE);
        sourceResource = context.getDeployedResources().get(sourceResourceId);
        if (sourceResource == null) {
            throw new MisconfiguredException("The source resource must be defined");
        }

        targetResourceId = properties.get(SETTING_TARGET_RESOURCE);
        targetResource = context.getDeployedResources().get(targetResourceId);
        if (targetResource == null) {
            throw new MisconfiguredException("The target resource must be defined");
        }

        targetRelativePath = properties.get(SETTING_TARGET_RELATIVE_PATH, "");
        mustExist = properties.is(SETTING_MUST_EXIST, mustExist);
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback messageTarget, boolean unitOfWorkBoundaryReached) {    
        
        String targetPath = resolveParamsAndHeaders(targetRelativePath, inputMessage);
    	if (inputMessage instanceof TextMessage) {
            List<String> files = ((TextMessage)inputMessage).getPayload();
            fileNames.addAll(files);
            getComponentStatistics().incrementNumberEntitiesProcessed(files.size());
        }
        
        if (inputMessage instanceof ControlMessage) {
            IDirectory sourceDir = null;
            IDirectory targetDir = null;
            ZipOutputStream zos = null;

            sourceDir = sourceResource.reference();
            targetDir = targetResource.reference();
        	
            try {
            	targetDir.delete(targetPath);
                zos = new ZipOutputStream(targetDir.getOutputStream(targetPath, false), Charset.forName(encoding));

                for (String fileName : fileNames) {
                    FileInfo sourceZipFile = sourceDir.listFile(fileName);           
                    log(LogLevel.INFO, "Received file name to add to zip: %s", sourceZipFile);
                    if (mustExist && sourceZipFile == null) {
                        throw new IoException(String.format("Could not find file to zip: %s", sourceZipFile));
                    }

                    if (sourceZipFile != null) {
                        try {
                            if (!sourceZipFile.isDirectory()) {
                                ZipEntry entry = new ZipEntry(sourceZipFile.getName());
                                entry.setSize(sourceZipFile.getSize());
                                entry.setTime(sourceZipFile.getLastUpdated());
                                zos.putNextEntry(entry);
                                log(LogLevel.INFO, "Adding %s", sourceZipFile.getName());                        
                                InputStream fis = sourceDir.getInputStream(sourceZipFile.getRelativePath(), unitOfWorkBoundaryReached);
                                if (fis != null) {
                                    try {
                                        IOUtils.copy(fis, zos);
                                    } finally {
                                        IOUtils.closeQuietly(fis);
                                    }
                                }
                            }
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new IoException(e);
                        }
                    }
                }
                
                log(LogLevel.INFO, "Generated %s", targetPath);

            } finally {
                IOUtils.closeQuietly(zos);
            }
            
            if (deleteOnComplete) {
                for (String fileName : fileNames) {
                	sourceDir.delete(fileName);
                }
            }  
            
            fileNames.clear();
        }
    }

}
