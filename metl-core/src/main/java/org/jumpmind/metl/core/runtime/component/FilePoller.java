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
import org.jumpmind.metl.core.runtime.TextMessage;
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
    
    public final static String SETTING_GET_FILE_PATTERN_FROM_MESSAGE = "get.file.pattern.from.message";

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

    boolean cancelOnNoFiles = true;
    
    boolean getFilePatternFromMessage = false;
    
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
                context.getFlowParameters(), true);
        triggerFilePath = FormatUtils.replaceTokens(properties.get(SETTING_TRIGGER_FILE_PATH),
                context.getFlowParameters(), true);
        useTriggerFile = properties.is(SETTING_USE_TRIGGER_FILE, useTriggerFile);
        cancelOnNoFiles = properties.is(SETTING_CANCEL_ON_NO_FILES, cancelOnNoFiles);
        actionOnSuccess = properties.get(SETTING_ACTION_ON_SUCCESS, actionOnSuccess);
        actionOnError = properties.get(SETTING_ACTION_ON_ERROR, actionOnError);        
        archiveOnErrorPath = FormatUtils.replaceTokens(
                properties.get(SETTING_ARCHIVE_ON_ERROR_PATH), context.getFlowParameters(),
                true);
        archiveOnSuccessPath = FormatUtils.replaceTokens(
                properties.get(SETTING_ARCHIVE_ON_SUCCESS_PATH),
                context.getFlowParameters(), true);
        maxFilesToPoll = properties.getInt(SETTING_MAX_FILES_TO_POLL);
        minFilesToPoll = properties.getInt(SETTING_MIN_FILES_TO_POLL);
        fileSortOption = properties.get(SETTING_FILE_SORT_ORDER, fileSortOption);
        runWhen = properties.get(RUN_WHEN, PER_UNIT_OF_WORK);        
        getFilePatternFromMessage = properties.is(SETTING_GET_FILE_PATTERN_FROM_MESSAGE);
        
        if (!getFilePatternFromMessage && StringUtils.isEmpty(filePattern)) {
            throw new MisconfiguredException(
                    "If file patterns do not come from inbound messages, then a file pattern must be set.");
        }
    }
        
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

	@Override
	public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
	    
        List<String> filePatternsToPoll = getFilePatternsToPoll(inputMessage);
		if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
				|| (!PER_UNIT_OF_WORK.equals(runWhen) && !(inputMessage instanceof ControlMessage))) {
		    IDirectory directory = getResourceReference();
			if (useTriggerFile) {
				List<FileInfo> triggerFiles = directory.listFiles(triggerFilePath);
				if (triggerFiles != null && triggerFiles.size() > 0) {
					pollForFiles(filePatternsToPoll, callback, unitOfWorkBoundaryReached);
					directory.delete(triggerFilePath);
				} else if (cancelOnNoFiles) {
					callback.sendShutdownMessage(true);
				}
			} else {
				pollForFiles(filePatternsToPoll, callback, unitOfWorkBoundaryReached);
			}
		}
	}
		
    protected List<String> getFilePatternsToPoll(Message inputMessage) {
        ArrayList<String> filePatternsToPoll = null;
        if (getFilePatternFromMessage && inputMessage instanceof TextMessage) {
            filePatternsToPoll = ((TextMessage)inputMessage).getPayload();
        } else {
            filePatternsToPoll = new ArrayList<String>(1);
            filePatternsToPoll.add(filePattern);
        }
        return filePatternsToPoll;
    }

    
    protected List<FileInfo> matchFiles(String pattern, IDirectory resourceDirectory,
            AntPathMatcher pathMatcher) {

        StringBuilder subPartToMatch = new StringBuilder();
        List<FileInfo> fileMatches = new ArrayList<FileInfo>();
        String[] patternParts = pattern.split("/");

        for (int i = 0; i < patternParts.length; i++) {
            if (i != patternParts.length - 1) {
                // directory specifications
                if (!pathMatcher.isPattern(patternParts[i])) {
                    // fixed path with no wildcards
                    if (subPartToMatch.length() > 0) {
                        subPartToMatch.append("/");
                    }
                    subPartToMatch.append(patternParts[i]);
                } else {
                    // some type of wildcard pattern in a relative directory
                    List<FileInfo> childFileMatches = listFilesAndDirsFromDirectory(
                            subPartToMatch.toString(), patternParts[i], resourceDirectory, pathMatcher);
                    String childPartToMatch = null;
                    String remainderPath = "";
                    for (int j=i;j<patternParts.length;j++) {
                        remainderPath = remainderPath + patternParts[j];
                        if (j != patternParts.length-1) {
                            remainderPath = remainderPath + "/";
                        }
                    }
                    for (FileInfo fileInfo : childFileMatches) {
                        if (fileInfo.isDirectory()) {
                            childPartToMatch = subPartToMatch + "/" + fileInfo.getName() + "/" + remainderPath;
                            fileMatches.addAll(
                                    matchFiles(childPartToMatch, resourceDirectory, pathMatcher));
                        }
                    }
                }
            }
        }
        fileMatches.addAll(listFilesAndDirsFromDirectory(subPartToMatch.toString(),
                patternParts[patternParts.length - 1], resourceDirectory, pathMatcher));

        return fileMatches;
    }

    protected List<FileInfo> listFilesAndDirsFromDirectory(String pattern, String fileSpecification,
            IDirectory resourceDirectory, AntPathMatcher pathMatcher) {
        
        List<FileInfo> files = new ArrayList<FileInfo>();
        List<FileInfo> matchedFiles = new ArrayList<FileInfo>();
        files = resourceDirectory.listFiles(pattern);
        for (FileInfo file : files) {
            if (pathMatcher.match(fileSpecification, file.getName())) {
                matchedFiles.add(file);
            }
        }
        return matchedFiles;
    }
    
    protected void pollForFiles(List<String> filePatternsToPoll, ISendMessageCallback callback,
            boolean unitOfWorkLastMessage) {

        AntPathMatcher pathMatcher = new AntPathMatcher();

        IDirectory directory = getResourceReference();

        List<FileInfo> matches = new ArrayList<>();
        
        ArrayList<FileInfo> filesToSend = new ArrayList<FileInfo>();

        for (String patternToPoll : filePatternsToPoll) {

            String[] includes = StringUtils.isNotBlank(patternToPoll) ? patternToPoll.split(",")
                    : new String[] { "*" };

            for (String pattern : includes) {
                matches.addAll(matchFiles(pattern, directory, pathMatcher));
            }

            if (matches.size() >= minFilesToPoll) {
                for (int i = 0; i < matches.size() && i < maxFilesToPoll; i++) {
                    filesSent.add(matches.get(i));
                    filesToSend.add(matches.get(i));
                }

                Collections.sort(filesSent, (o1, o2) -> {
                    int cmpr = 0;
                    if (SORT_NAME.equals(fileSortOption)) {
                        cmpr = new String(o1.getRelativePath())
                                .compareTo(new String(o2.getRelativePath()));
                    } else if (SORT_MODIFIED.equals(fileSortOption)) {
                        cmpr = new Long(o1.getLastUpdated())
                                .compareTo(new Long(o2.getLastUpdated()));
                    }
                    return cmpr;
                });

                ArrayList<String> filePaths = new ArrayList<>();
                for (FileInfo file : filesToSend) {
                    log(LogLevel.INFO, "File polled: " + file.getRelativePath());
                    getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                    filePaths.add(file.getRelativePath());
                    if (filePaths.size() <= filesPerMessage) {
                        callback.sendTextMessage(null, filePaths);
                        filePaths = new ArrayList<>();
                    }
                }

                if (filePaths.size() > 0) {
                    callback.sendTextMessage(null, filePaths);
                }
            } else if (cancelOnNoFiles) {
                callback.sendShutdownMessage(true);
            }
            matches.clear();
            filesToSend.clear();
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
            directory.moveToDir(srcFile.getRelativePath(), archivePath);
        }
    }
    
    public void setRunWhen(String runWhen) {
        this.runWhen = runWhen;
    }
}
