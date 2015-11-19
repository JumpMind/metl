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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.LocalFile;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;
import org.springframework.util.FileCopyUtils;

public class FileUtil extends AbstractComponentRuntime {

    public static final String ACTION_COPY = "Copy";
    public static final String ACTION_MOVE = "Move";
    /*
    public static final String ACTION_RENAME = "Rename";    
    public static final String ACTION_DELETE = "Delete";
    public static final String ACTION_TOUCH = "Touch";
    */

    public final static String SETTING_ACTION = "action";

    public final static String SETTING_RELATIVE_PATH = "relative.path";

    public final static String SETTING_GET_FILE_FROM_MESSAGE = "get.file.name.from.message";

    public static final String SETTING_MUST_EXIST = "must.exist";

    public static final String SETTING_TARGET_RESOURCE = "target.resource";

    public static final String SETTING_TARGET_RELATIVE_PATH = "target.relative.path";

    public static final String SETTING_NEW_NAME = "new.name";

    public static final String SETTING_APPEND_TO_NAME = "append.to.name";
    
    public static final String SETTING_OVERWRITE = "overwrite";

    String action = ACTION_COPY;
    
    String runWhen = PER_UNIT_OF_WORK;

    boolean getFileNameFromMessage = false;

    File sourcePathAndFile;

    boolean mustExist = false;

    String targetDirName;

    String targetRelativePath;

    String newName;

    String appendToName;

    boolean overwrite = true;
    
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    protected void start() {
        TypedProperties typedProperties = getTypedProperties();
        getFileNameFromMessage = typedProperties.is(SETTING_GET_FILE_FROM_MESSAGE, getFileNameFromMessage);
        if (!getFileNameFromMessage) {
            IResourceRuntime resource = getResourceRuntime();
            if (!(resource instanceof LocalFile)) {
                throw new MisconfiguredException("This component only supports local file resources");
            }

            String baseDir = resource.getResourceRuntimeSettings().get(LocalFile.LOCALFILE_PATH);
            if (isBlank(baseDir)) {
                throw new MisconfiguredException("The %s resource needs its path set", resource.getResource().getName());
            }

            String relativePath = typedProperties.get(SETTING_RELATIVE_PATH);
            if (isBlank(relativePath)) {
                throw new MisconfiguredException("The %s resource needs its path set", resource.getResource().getName());
            }

            sourcePathAndFile = new File(baseDir, relativePath);

        }

        action = typedProperties.get(SETTING_ACTION);
        mustExist = typedProperties.is(SETTING_MUST_EXIST, mustExist);
        String targetResourceId = typedProperties.get(SETTING_TARGET_RESOURCE);
        IResourceRuntime targetResource = context.getDeployedResources().get(targetResourceId);
        if (!(targetResource instanceof LocalFile)) {
            throw new MisconfiguredException("The target resource must be a local file resource");
        }

        targetDirName = targetResource.getResourceRuntimeSettings().get(LocalFile.LOCALFILE_PATH);
        if (isBlank(targetDirName)) {
            throw new MisconfiguredException("The target resource %s needs its path set", targetResource.getResource().getName());
        }
        targetRelativePath = typedProperties.get(SETTING_TARGET_RELATIVE_PATH);
        newName = typedProperties.get(SETTING_NEW_NAME, newName);
        appendToName = typedProperties.get(SETTING_APPEND_TO_NAME);
        overwrite = typedProperties.is(SETTING_OVERWRITE, overwrite);
        runWhen = typedProperties.get(RUN_WHEN, PER_UNIT_OF_WORK);
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
								FileUtils.deleteQuietly(new File(fileName));
							}
						}
					} catch (Exception e) {
						throw new IoException("Error processing file " + e.getMessage());
					}
				}
			}
			callback.sendMessage(null, filesProcessed);
		}
	}

    protected String copyFile(Message inputMessage, String fileName) throws Exception {
        File sourceFile = new File(fileName);
        if (mustExist && !sourceFile.exists()) {
            throw new FileNotFoundException("Unable to locate file " + fileName);
        } else if (sourceFile.exists()) {
            String targetName = newName;
            if (isBlank(targetName)) {
                targetName = sourceFile.getName();
            }

            Map<String, String> parms = new HashMap<>(getComponentContext().getFlowParametersAsString());
            parms.putAll(inputMessage.getHeader().getAsStrings());
            String tokenResolvedName = FormatUtils.replaceTokens(targetName, parms, true);
            String tokenResolvedAppendToName = FormatUtils.replaceTokens(appendToName, parms, true);

            String targetFileWithouPart = getTargetFileName(tokenResolvedName, sourceFile, tokenResolvedAppendToName);
            String targetFileName = targetFileWithouPart + ".part";

            File targetDir = new File(targetDirName);

            if (isNotBlank(targetRelativePath)) {
                targetDir = new File(targetDir, targetRelativePath);
            }

            targetDir.mkdirs();

            File targetFile = new File(targetDir, targetFileName);
            targetFile.getParentFile().mkdirs();
            if ((targetFile.exists() && overwrite) || !targetFile.exists()) {
                FileCopyUtils.copy(sourceFile, targetFile);
                File finalFile = new File(targetDir, targetFileWithouPart);
                if (!targetFile.renameTo(finalFile)) {
                    throw new IoException(String.format("Rename of %s to %s failed", targetFile.getAbsolutePath(), targetFileWithouPart));
                }

                return finalFile.getAbsolutePath();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    protected String getTargetFileName(String tokenResolvedName, File sourceFile, String tokenResolvedAppendToName) {
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
        if (getFileNameFromMessage) {
            files = inputMessage.getPayload();
        } else {
            files.add(sourcePathAndFile.getAbsolutePath());
        }
        return files;
    }

}
