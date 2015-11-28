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
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    protected void start() {
        TypedProperties typedProperties = getTypedProperties();
        
        IDirectory directory = getResourceReference();
        if (directory == null) {
            throw new MisconfiguredException("A directory resource must be configured.  It is required.");
        }

        getFileNameFromMessage = typedProperties.is(SETTING_GET_FILE_FROM_MESSAGE, getFileNameFromMessage);
        if (!getFileNameFromMessage) {
            sourceRelativePath = typedProperties.get(SETTING_RELATIVE_PATH);
            if (isBlank(sourceRelativePath)) {
                throw new MisconfiguredException("The relative path to find the source file has not been set.  It is required.");
            }
        }
        
        action = typedProperties.get(SETTING_ACTION);
        targetRelativePath = typedProperties.get(SETTING_TARGET_RELATIVE_PATH);
        appendToName = typedProperties.get(SETTING_APPEND_TO_NAME);
        overwrite = typedProperties.is(SETTING_OVERWRITE, overwrite);
        runWhen = typedProperties.get(RUN_WHEN, PER_UNIT_OF_WORK);
        newName = typedProperties.get(SETTING_TARGET_NAME);
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
						String targetFile = null;
						if (action.equals(ACTION_COPY) || action.equals(ACTION_MOVE)) {
							targetFile = copyFile(inputMessage, fileName);
							if (isNotBlank(targetFile)) {
								getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
								filesProcessed.add(targetFile);
							}
						}

						if (action.equals(ACTION_MOVE)) {
							if (isNotBlank(targetFile)) {
							    IDirectory directory = getResourceReference();
								directory.delete(fileName);
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

    protected String copyFile(Message inputMessage, String sourceFileName) throws Exception {
        IDirectory directory = getResourceReference();
        FileInfo sourceFileInfo = directory.listFile(sourceFileName);
        if (mustExist && sourceFileInfo == null) {
            throw new FileNotFoundException("Unable to locate file " + sourceFileName);
        } else if (sourceFileInfo != null) {
            Map<String, String> parms = new HashMap<>(getComponentContext().getFlowParameters());
            parms.putAll(inputMessage.getHeader().getAsStrings());
            
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
            targetPath = FormatUtils.replaceTokens(targetPath, parms, true);
            String tokenResolvedAppendToName = FormatUtils.replaceTokens(appendToName, parms, true);
            String targetFileWithouPart = getTargetFileName(targetPath, sourceFileInfo, tokenResolvedAppendToName);
            String targetFileName = targetFileWithouPart + ".part";

            FileInfo targetFile = directory.listFile(targetFileName);
            if ((targetFile != null && overwrite) || targetFile == null) {
                directory.copyFile(sourceFileName, targetFileName);
                if (!directory.renameFile(targetFileName, targetFileWithouPart)) {
                    throw new IoException(String.format("Rename of %s to %s failed", targetFileName, targetFileWithouPart));
                }
                return targetFileWithouPart;
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
            if (parts.length > 1) {
                StringBuffer sb = new StringBuffer().append(parts[0]).append(tokenResolvedAppendToName).append(".").append(parts[1]);
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
