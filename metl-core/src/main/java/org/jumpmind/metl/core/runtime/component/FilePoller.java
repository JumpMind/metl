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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.SettingDefinition;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.LocalFile;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;

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
    
    public final static String SORT_NAME = "Name";
    public final static String SORT_MODIFIED = "Last Modified";

    public final static String SETTING_FILE_SORT_ORDER = "file.sort.order";

    @SettingDefinition(order = 70, type = Type.TEXT, label = "Relative Trigger File Path")
    public final static String SETTING_TRIGGER_FILE_PATH = "trigger.file.path";

    String filePattern;

    String triggerFilePath;

    boolean useTriggerFile = false;

    boolean recurse = false;

    boolean cancelOnNoFiles = true;
    
    int maxFilesToPoll;

    String actionOnSuccess = ACTION_NONE;

    String archiveOnSuccessPath;

    String actionOnError = ACTION_NONE;

    String archiveOnErrorPath;
    
    String fileSortOption = SORT_MODIFIED;

    ArrayList<File> filesSent = new ArrayList<File>();

    @Override
    protected void start() {
        Component component = getComponent();
        Resource resource = component.getResource();
        if (!resource.getType().equals(LocalFile.TYPE)) {
            throw new IllegalStateException(String.format("The resource must be of type %s",
                    LocalFile.TYPE));
        }
        TypedProperties properties = getTypedProperties();

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
        fileSortOption = properties.get(SETTING_FILE_SORT_ORDER, fileSortOption);

    }
        
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        IResourceRuntime resourceRuntime = getResourceRuntime();
        String path = resourceRuntime.getResourceRuntimeSettings().get(LocalFile.LOCALFILE_PATH);
        if (useTriggerFile) {
            File triggerFile = new File(path, triggerFilePath);
            if (triggerFile.exists()) {
                pollForFiles(path, inputMessage, callback, unitOfWorkBoundaryReached);
                FileUtils.deleteQuietly(triggerFile);
            } else if (cancelOnNoFiles) {
                callback.sendShutdownMessage(true);
            }
        } else {
            pollForFiles(path, inputMessage, callback, unitOfWorkBoundaryReached);
        }
    }

    protected void pollForFiles(String path, Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {
        File pathDir = new File(path);
        ArrayList<String> filePaths = new ArrayList<String>();
        ArrayList<File> fileReferences = new ArrayList<File>();
        String[] includes = StringUtils.isNotBlank(filePattern) ? filePattern.split(",")
                : new String[] { "*" };
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(includes);
        scanner.setBasedir(pathDir);
        scanner.setCaseSensitive(false);
        scanner.scan();
        String[] files = scanner.getIncludedFiles();
        if (files.length > 0) {
            for(int i = 0; i < files.length && i < maxFilesToPoll; i++) {
                File file = new File(path, files[i]);
                filesSent.add(file);
                fileReferences.add(file);
            }
            
            Collections.sort(fileReferences, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                	int cmpr = 0;
                	if (SORT_NAME.equals(fileSortOption)) {
                		cmpr = new String(o1.getName()).compareTo(new String(o2.getName()));
                    } else if (SORT_MODIFIED.equals(fileSortOption)) {
                    	cmpr = new Long(o1.lastModified()).compareTo(new Long(o2.lastModified()));
                    }
                	return cmpr;
                }
            });
            
            for (File file : fileReferences) {
                log(LogLevel.INFO, "File polled: " + file.getAbsolutePath());
                getComponentStatistics().incrementNumberEntitiesProcessed();
                filePaths.add(file.getAbsolutePath());
            }
            callback.sendMessage(filePaths, unitOfWorkLastMessage);
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
        for (File srcFile : filesSent) {
            if(FileUtils.deleteQuietly(srcFile)) {
                log(LogLevel.INFO, "Deleted %s", srcFile.getAbsolutePath());
            } else {
                log(LogLevel.WARN, "Failed to delete %s", srcFile.getAbsolutePath());
            }            
        }
    }

    protected void archive(String archivePath) {
        String path = getResourceRuntime().getResourceRuntimeSettings().get(LocalFile.LOCALFILE_PATH);
        File destDir = new File(path, archivePath);
        for (File srcFile : filesSent) {
            try {
                File targetFile = new File(destDir, srcFile.getName());
                if (targetFile.exists()) {
                    info("The msgTarget file already exists.   Deleting it in order to archive a new file.");
                    FileUtils.deleteQuietly(targetFile);
                }
                log(LogLevel.INFO, "Archiving %s tp %s", srcFile.getAbsolutePath(), destDir.getAbsolutePath());
                FileUtils.moveFileToDirectory(srcFile, destDir, true);
            } catch (IOException e) {
                throw new IoException(e);
            }
        }
    }
}
