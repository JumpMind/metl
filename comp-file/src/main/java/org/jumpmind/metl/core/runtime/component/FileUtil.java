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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.FileInfo;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;

public class FileUtil extends AbstractComponentRuntime {

    public static final String ACTION_COPY = "Copy";
    public static final String ACTION_MOVE = "Move";
    public static final String ACTION_DELETE = "Delete";

    public final static String SETTING_ACTION = "action";

    public final static String SETTING_RELATIVE_PATH = "relative.path";

    public final static String SETTING_GET_FILE_FROM_MESSAGE = "get.file.name.from.message";

    public static final String SETTING_MUST_EXIST = "must.exist";

    public static final String SETTING_TARGET_RELATIVE_PATH = "target.relative.path";

    public static final String SETTING_APPEND_TO_NAME = "append.to.name";
    
    public static final String SETTING_OVERWRITE = "overwrite";
    
    public static final String SETTING_TARGET_NAME = "new.name";

    String action = ACTION_COPY;
    
    String runWhen = PER_UNIT_OF_WORK;

    boolean getFileNameFromMessage = false;

    String sourceRelativePath;

    boolean mustExist = false;

    String targetRelativePath;

    String appendToName;
    
    String newName;

    boolean overwrite = true;
    
    IDirectory directory;
    
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    public void start() {
        TypedProperties typedProperties = getTypedProperties();
        
        directory = getResourceReference();
        if (directory == null) {
            throw new MisconfiguredException("A directory resource must be configured.  It is required.");
        }

        getFileNameFromMessage = typedProperties.is(SETTING_GET_FILE_FROM_MESSAGE, getFileNameFromMessage);
        if (!getFileNameFromMessage) {
            sourceRelativePath = typedProperties.get(SETTING_RELATIVE_PATH);
        }
        
        action = typedProperties.get(SETTING_ACTION);
        targetRelativePath = typedProperties.get(SETTING_TARGET_RELATIVE_PATH);
        appendToName = typedProperties.get(SETTING_APPEND_TO_NAME);
        overwrite = typedProperties.is(SETTING_OVERWRITE, overwrite);
        runWhen = typedProperties.get(RUN_WHEN, PER_UNIT_OF_WORK);
        newName = FormatUtils.replaceTokens(typedProperties.get(SETTING_TARGET_NAME), context.getFlowParameters(),true);
    }
    
    @Override
    public void stop() {
        if (directory != null) {
            directory.close();
        }
    }

	@Override
	public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
		if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
				|| (!PER_UNIT_OF_WORK.equals(runWhen) && !(inputMessage instanceof ControlMessage))) {
			List<String> files = getFilesToRead(inputMessage);
			ArrayList<String> filesProcessed = new ArrayList<>();
			if (files != null) {
				for (String fileName : files) {
					try {
						if (fileName != null) {
							String targetFile = null;
							if (action.equals(ACTION_COPY)) {
							    targetFile = copyFile(inputMessage, fileName);
							} else if (action.equals(ACTION_MOVE)) {
							    targetFile = moveFile(inputMessage, fileName);
                            } else if (action.equals(ACTION_DELETE)) {
                                targetFile = deleteFile(inputMessage, fileName);
                            }
							if (isNotBlank(targetFile)) {
                                getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                                filesProcessed.add(targetFile);
                            }
						}
					} catch (Exception e) {
						throw new IoException(e);
					}
				}
			}
			callback.sendTextMessage(null, filesProcessed);
		}
	}
	
    protected String deleteFile(Message inputMessage, String sourceFileName) throws Exception {
        sourceFileName = resolveFlowParams(sourceFileName);
        FileInfo sourceFileInfo = directory.listFile(sourceFileName, false);
        String deletedFileName = null;

        if (mustExist && sourceFileInfo == null) {
            throw new FileNotFoundException("Unable to locate file " + sourceFileName);
        } else if (sourceFileInfo != null) {
            directory.delete(sourceFileName);
            deletedFileName = sourceFileName;
        }
        return deletedFileName;
    }
	
	protected String moveFile(Message inputMessage, String sourceFileName) throws Exception {
        FileInfo sourceFileInfo = directory.listFile(sourceFileName, false);
        String movedFileName = null;
        
        if (mustExist && sourceFileInfo == null) {
            throw new FileNotFoundException("Unable to locate file " + sourceFileName);
        } else if (sourceFileInfo != null) {
            String targetPath = targetRelativePath;
            if (getFileNameFromMessage || targetRelativePath.endsWith("/") || isNotBlank(newName)) {
                String fileName = null;
                if (isNotBlank(newName)) {
                    fileName = newName;
                } else {
                    fileName = sourceFileInfo.getName();
                }
                targetPath = targetPath + "/" + fileName;
            }
            targetPath = resolveParamsAndHeaders(targetPath, inputMessage);
            String tokenResolvedAppendToName = resolveParamsAndHeaders(appendToName, inputMessage);
            String targetFileName = getTargetFileName(targetPath, sourceFileInfo, tokenResolvedAppendToName);

            if (overwrite && directory.listFile(targetFileName, false)!=null) {
                directory.delete(targetFileName);
            	directory.moveFile(sourceFileName, targetFileName, false);
                movedFileName = targetFileName;
            } else if (overwrite ||(!overwrite && directory.listFile(targetFileName, false)==null)) {
                directory.moveFile(sourceFileName, targetFileName, false);
                movedFileName = targetFileName;
            }
        }
        return movedFileName;
	}

    protected String copyFile(Message inputMessage, String sourceFileName) throws Exception {
        FileInfo sourceFileInfo = directory.listFile(sourceFileName, false);
        if (mustExist && sourceFileInfo == null) {
            throw new FileNotFoundException("Unable to locate file " + sourceFileName);
        } else if (sourceFileInfo != null) {
            String targetPath = targetRelativePath;
            if (getFileNameFromMessage || targetRelativePath.endsWith("/") || isNotBlank(newName)) {
                String fileName = null;
                if (isNotBlank(newName)) {
                    fileName = newName;
                } else {
                    fileName = sourceFileInfo.getName();
                }
                targetPath = targetPath + "/" + fileName;
            }
            targetPath = resolveParamsAndHeaders(targetPath, inputMessage);
            String tokenResolvedAppendToName = resolveParamsAndHeaders(appendToName, inputMessage);
            String targetFileWithoutPart = getTargetFileName(targetPath, sourceFileInfo, tokenResolvedAppendToName);
            String targetFileName = targetFileWithoutPart + ".part";

            FileInfo targetFile = directory.listFile(targetFileName, false);
            if ((targetFile != null && overwrite) || targetFile == null) {
                directory.copyFile(sourceFileName, targetFileName, false);
                if (!directory.renameFile(targetFileName, targetFileWithoutPart, false)) {
                    throw new IoException(String.format("Rename of %s to %s failed", targetFileName, targetFileWithoutPart));
                }
                return targetFileWithoutPart;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    protected String getTargetFileName(String tokenResolvedName, FileInfo sourceFile, String tokenResolvedAppendToName) {
        String fileName = (tokenResolvedName != null ? tokenResolvedName : sourceFile.getName());
        if (tokenResolvedAppendToName != null && tokenResolvedAppendToName.length() > 0) {
            String[] parts = fileName.split("\\.");
        	StringBuffer sb = new StringBuffer();
            if (parts.length > 1) {
            	int n = 0;
            	while (n < parts.length - 1) {
            		sb.append(parts[n]).append(".");
            		n++;
            	}
            	sb.deleteCharAt(sb.length() - 1);  // delete final '.' added before appending provided text
            	sb.append(tokenResolvedAppendToName).append(".").append(parts[n]);
                fileName = sb.toString();
            } else if (parts.length == 1) {
            	sb.append(parts[0]).append(tokenResolvedAppendToName);
            	fileName = sb.toString();
            }
        }
        return fileName;
    }

    private List<String> getFilesToRead(Message inputMessage) {
        ArrayList<String> files = new ArrayList<String>();
        if (getFileNameFromMessage && inputMessage instanceof TextMessage) {
            files = ((TextMessage)inputMessage).getPayload();
        } else {
            files.add(sourceRelativePath);
        }
        return files;
    }

}
