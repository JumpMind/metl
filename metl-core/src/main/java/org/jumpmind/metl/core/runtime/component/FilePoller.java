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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.SettingDefinition;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.FileInfo;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;
import org.springframework.util.AntPathMatcher;

public class FilePoller extends AbstractComponentRuntime {

    public static final String TYPE = "File Poller";

    public static final String ACTION_NONE = "None";
    public static final String ACTION_DELETE = "Delete";
    public static final String ACTION_ARCHIVE = "Archive";

    public final static String SETTING_FILE_PATTERN = "file.pattern";

    public final static String SETTING_RECURSE = "recurse";

    public final static String SETTING_CANCEL_ON_NO_FILES = "cancel.on.no.files";

    public final static String SETTING_ACTION_ON_SUCCESS = "action.on.success";

    public final static String SETTING_ARCHIVE_ON_SUCCESS_PATH = "archive.on.success.path";

    public final static String SETTING_ACTION_ON_ERROR = "action.on.error";

    public final static String SETTING_ARCHIVE_ON_ERROR_PATH = "archive.on.error.path";

    public final static String SETTING_USE_TRIGGER_FILE = "use.trigger.file";
    
    public final static String SETTING_MAX_FILES_TO_POLL = "max.files.to.poll";
    
    public final static String SETTING_MIN_FILES_TO_POLL = "min.files.to.poll";
    
    public final static String SORT_NAME = "Name";
    public final static String SORT_MODIFIED = "Last Modified";

    public final static String SETTING_FILE_SORT_ORDER = "file.sort.order";

    @SettingDefinition(order = 70, type = Type.TEXT, label = "Relative Trigger File Path")
    public final static String SETTING_TRIGGER_FILE_PATH = "trigger.file.path";
    
    String runWhen = PER_UNIT_OF_WORK;

    String filePattern;

    String triggerFilePath;

    boolean useTriggerFile = false;

    boolean recurse = false;

    boolean cancelOnNoFiles = true;
    
    int maxFilesToPoll;
    
    int minFilesToPoll = 1;

    String actionOnSuccess = ACTION_NONE;

    String archiveOnSuccessPath;

    String actionOnError = ACTION_NONE;

    String archiveOnErrorPath;
    
    String fileSortOption = SORT_MODIFIED;
    
    int filesPerMessage = 1000;

    ArrayList<FileInfo> filesSent = new ArrayList<FileInfo>();

    @Override
    protected void start() {
        Component component = getComponent();
        Resource resource = component.getResource();
        if (resource == null) {
            throw new MisconfiguredException("A resource is required");
        }
        TypedProperties properties = getTypedProperties();

        filesPerMessage = properties.getInt(ROWS_PER_MESSAGE);
        filePattern = FormatUtils.replaceTokens(properties.get(SETTING_FILE_PATTERN),
                context.getFlowParametersAsString(), true);
        triggerFilePath = FormatUtils.replaceTokens(properties.get(SETTING_TRIGGER_FILE_PATH),
                context.getFlowParametersAsString(), true);
        useTriggerFile = properties.is(SETTING_USE_TRIGGER_FILE, useTriggerFile);
        recurse = properties.is(SETTING_RECURSE, recurse);
        cancelOnNoFiles = properties.is(SETTING_CANCEL_ON_NO_FILES, cancelOnNoFiles);
        actionOnSuccess = properties.get(SETTING_ACTION_ON_SUCCESS, actionOnSuccess);
        actionOnError = properties.get(SETTING_ACTION_ON_ERROR, actionOnError);
        archiveOnErrorPath = FormatUtils.replaceTokens(
                properties.get(SETTING_ARCHIVE_ON_ERROR_PATH), context.getFlowParametersAsString(),
                true);
        archiveOnSuccessPath = FormatUtils.replaceTokens(
                properties.get(SETTING_ARCHIVE_ON_SUCCESS_PATH),
                context.getFlowParametersAsString(), true);
        maxFilesToPoll = properties.getInt(SETTING_MAX_FILES_TO_POLL);
        minFilesToPoll = properties.getInt(SETTING_MIN_FILES_TO_POLL);
        fileSortOption = properties.get(SETTING_FILE_SORT_ORDER, fileSortOption);
        runWhen = properties.get(RUN_WHEN, PER_UNIT_OF_WORK);

    }
        
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

	@Override
	public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
		if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
				|| (!PER_UNIT_OF_WORK.equals(runWhen) && !(inputMessage instanceof ControlMessage))) {
		    IDirectory directory = getResourceReference();
			if (useTriggerFile) {
				List<FileInfo> triggerFiles = directory.listFiles(triggerFilePath);
				if (triggerFiles != null && triggerFiles.size() > 0) {
					pollForFiles(inputMessage, callback, unitOfWorkBoundaryReached);
					directory.delete(triggerFilePath);
				} else if (cancelOnNoFiles) {
					callback.sendShutdownMessage(true);
				}
			} else {
				pollForFiles(inputMessage, callback, unitOfWorkBoundaryReached);
			}
		}
	}
	
	protected List<FileInfo> matchFiles (String pattern, IDirectory directory, AntPathMatcher pathMatcher) {
        List<FileInfo> matches = new ArrayList<>();
	    if (pathMatcher.isPattern(pattern)) {
	        String[] parts = pattern.split("/");
	        StringBuilder path = new StringBuilder();
	        for (String part : parts) {
                if (!pathMatcher.isPattern(part)) {
                    path.append(part).append("/");
                } else {
                    break;
                }
            }	        
	        matches.addAll(matchFiles(path.toString(), pattern, directory, pathMatcher));
	    } else {
            matches.addAll(directory.listFiles(pattern));	        
	    }
	    return matches;
	}
	
	protected List<FileInfo> matchFiles (String relativePath, String pattern, IDirectory directory, AntPathMatcher pathMatcher) {
	    List<FileInfo> matches = new ArrayList<>();
	    List<FileInfo> fileInfos = directory.listFiles(relativePath);
        for (FileInfo fileInfo : fileInfos) {
            if (matches.size() < maxFilesToPoll) {
                if (!fileInfo.isDirectory() && pathMatcher.match(pattern, fileInfo.getRelativePath()) 
                        && !matches.contains(fileInfo)) {
                    matches.add(fileInfo);
                } else if (fileInfo.isDirectory()) {
                    matches.addAll(matchFiles(fileInfo.getRelativePath(), pattern, directory, pathMatcher));
                }
            }
        }
	    return matches;
	}

    protected void pollForFiles(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {
        String[] includes = StringUtils.isNotBlank(filePattern) ? filePattern.split(",")
                : new String[] { "*" };
        
        AntPathMatcher pathMatcher = new AntPathMatcher();
        
        IDirectory directory = getResourceReference();
        
        List<FileInfo> matches = new ArrayList<>();
        for (String pattern : includes) {
            matches.addAll(matchFiles(pattern, directory, pathMatcher));
        }
        
        if (matches.size() >= minFilesToPoll) {
            for(int i = 0; i < matches.size() && i < maxFilesToPoll; i++) {
                filesSent.add(matches.get(i));
            }
            
            Collections.sort(filesSent, (o1,o2) -> {
                	int cmpr = 0;
                	if (SORT_NAME.equals(fileSortOption)) {
                		cmpr = new String(o1.getRelativePath()).compareTo(new String(o2.getRelativePath()));
                    } else if (SORT_MODIFIED.equals(fileSortOption)) {
                    	cmpr = new Long(o1.getLastUpdated()).compareTo(new Long(o2.getLastUpdated()));
                    }
                	return cmpr;
            });
            
            ArrayList<String> filePaths = new ArrayList<>();
            for (FileInfo file : filesSent) {
                log(LogLevel.INFO, "File polled: " + file.getRelativePath());
                getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                filePaths.add(file.getRelativePath());
                if (filePaths.size() <= filesPerMessage) {
                    callback.sendMessage(null, filePaths);
                    filePaths = new ArrayList<>();
                }
            }
            
            if (filePaths.size() > 0) {
                callback.sendMessage(null, filePaths);
            }
        } else if (cancelOnNoFiles) {
            callback.sendShutdownMessage(true);
        }
    }
    
    @Override
    public void flowCompletedWithErrors(Throwable myError) {
        if (ACTION_ARCHIVE.equals(actionOnError)) {
            archive(archiveOnErrorPath);
        } else if (ACTION_DELETE.equals(actionOnError)) {
            deleteFiles();
        }
    }

    @Override
    public void flowCompleted(boolean cancelled) {
        if (ACTION_ARCHIVE.equals(actionOnSuccess)) {
            archive(archiveOnSuccessPath);
        } else if (ACTION_DELETE.equals(actionOnSuccess)) {
            deleteFiles();
        }
    }

    protected void deleteFiles() {
        IDirectory directory = getResourceReference();
        for (FileInfo srcFile : filesSent) {
            if(directory.delete(srcFile.getRelativePath())) {
                log(LogLevel.INFO, "Deleted %s", srcFile.getRelativePath());
            } else {
                log(LogLevel.WARN, "Failed to delete %s", srcFile.getRelativePath());
            }            
        }
    }

    protected void archive(String archivePath) {
        IDirectory directory = getResourceReference();
        for (FileInfo srcFile : filesSent) {
            directory.move(srcFile.getRelativePath(), archivePath);
        }
    }
    
    public void setRunWhen(String runWhen) {
        this.runWhen = runWhen;
    }
}
