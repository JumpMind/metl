package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.jumpmind.exception.IoException;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.ShutdownMessage;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.MessageType;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.ResourceCategory;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceRuntime;
import org.jumpmind.symmetric.is.core.runtime.resource.LocalFile;
import org.jumpmind.util.FormatUtils;

@ComponentDefinition(
        typeName = FilePoller.TYPE,
        category = ComponentCategory.READER,
        iconImage = "filepoller.png",
        outgoingMessage = MessageType.TEXT,
        resourceCategory = ResourceCategory.STREAMABLE)
public class FilePoller extends AbstractComponentRuntime {

    public static final String TYPE = "File Poller";

    public static final String ACTION_NONE = "None";
    public static final String ACTION_DELETE = "Delete";
    public static final String ACTION_ARCHIVE = "Archive";

    @SettingDefinition(order = 10, required = true, type = Type.TEXT, label = "File Pattern")
    public final static String SETTING_FILE_PATTERN = "file.pattern";

    @SettingDefinition(
            order = 20,
            type = Type.BOOLEAN,
            defaultValue = "false",
            label = "Search Recursively")
    public final static String SETTING_RECURSE = "recurse";

    @SettingDefinition(
            order = 30,
            type = Type.BOOLEAN,
            defaultValue = "true",
            label = "Cancel On No Files")
    public final static String SETTING_CANCEL_ON_NO_FILES = "cancel.on.no.files";

    @SettingDefinition(order = 35, type = Type.CHOICE, defaultValue = "NONE", choices = {
            ACTION_NONE, ACTION_ARCHIVE, ACTION_DELETE }, label = "Action on Success")
    public final static String SETTING_ACTION_ON_SUCCESS = "action.on.success";

    @SettingDefinition(order = 40, type = Type.TEXT, label = "Archive On Success Path")
    public final static String SETTING_ARCHIVE_ON_SUCCESS_PATH = "archive.on.success.path";

    @SettingDefinition(order = 45, type = Type.CHOICE, defaultValue = "NONE", choices = {
            ACTION_NONE, ACTION_ARCHIVE, ACTION_DELETE }, label = "Action on Error")
    public final static String SETTING_ACTION_ON_ERROR = "action.on.error";

    @SettingDefinition(order = 50, type = Type.TEXT, label = "Archive On Error Path")
    public final static String SETTING_ARCHIVE_ON_ERROR_PATH = "archive.on.error.path";

    @SettingDefinition(
            order = 60,
            type = Type.BOOLEAN,
            defaultValue = "false",
            label = "Use Trigger File")
    public final static String SETTING_USE_TRIGGER_FILE = "use.trigger.file";

    @SettingDefinition(order = 70, type = Type.TEXT, label = "Relative Trigger File Path")
    public final static String SETTING_TRIGGER_FILE_PATH = "trigger.file.path";

    String filePattern;

    String triggerFilePath;

    boolean useTriggerFile = false;

    boolean recurse = false;

    boolean cancelOnNoFiles = true;

    String actionOnSuccess = ACTION_NONE;

    String archiveOnSuccessPath;

    String actionOnError = ACTION_NONE;

    String archiveOnErrorPath;

    ArrayList<File> filesSent = new ArrayList<File>();

    @Override
    protected void start() {
        Component component = getComponent();
        Resource resource = component.getResource();
        if (!resource.getType().equals(LocalFile.TYPE)) {
            throw new IllegalStateException(String.format("The resource must be of type %s",
                    LocalFile.TYPE));
        }

        filePattern = FormatUtils.replaceTokens(component.get(SETTING_FILE_PATTERN),
                context.getFlowParametersAsString(), true);
        triggerFilePath = FormatUtils.replaceTokens(component.get(SETTING_TRIGGER_FILE_PATH),
                context.getFlowParametersAsString(), true);
        useTriggerFile = component.getBoolean(SETTING_USE_TRIGGER_FILE, useTriggerFile);
        recurse = component.getBoolean(SETTING_RECURSE, recurse);
        cancelOnNoFiles = component.getBoolean(SETTING_CANCEL_ON_NO_FILES, cancelOnNoFiles);
        actionOnSuccess = component.get(SETTING_ACTION_ON_SUCCESS, actionOnSuccess);
        actionOnError = component.get(SETTING_ACTION_ON_ERROR, actionOnError);
        archiveOnErrorPath = FormatUtils.replaceTokens(
                component.get(SETTING_ARCHIVE_ON_ERROR_PATH), context.getFlowParametersAsString(),
                true);
        archiveOnSuccessPath = FormatUtils.replaceTokens(
                component.get(SETTING_ARCHIVE_ON_SUCCESS_PATH),
                context.getFlowParametersAsString(), true);

    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        IResourceRuntime resourceRuntime = getResourceRuntime();
        String path = resourceRuntime.getResourceRuntimeSettings().get(LocalFile.LOCALFILE_PATH);
        if (useTriggerFile) {
            File triggerFile = new File(path, triggerFilePath);
            if (triggerFile.exists()) {
                pollForFiles(path, inputMessage, messageTarget);
                FileUtils.deleteQuietly(triggerFile);
            } else if (cancelOnNoFiles) {
                getComponentStatistics().incrementOutboundMessages();
                messageTarget.put(new ShutdownMessage(getFlowStepId(), true));
            }
        } else {
            pollForFiles(path, inputMessage, messageTarget);
        }
    }

    protected void pollForFiles(String path, Message inputMessage, IMessageTarget messageTarget) {
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
            for (String filePath : files) {
                File file = new File(path, filePath);
                filesSent.add(file);
                fileReferences.add(file);
            }
            
            Collections.sort(fileReferences, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return new Long(o1.lastModified()).compareTo(new Long(o2.lastModified()));
                }
            });
            
            for (File file : fileReferences) {
                log(LogLevel.INFO, "File polled: " + file.getAbsolutePath());
                getComponentStatistics().incrementNumberEntitiesProcessed();
                filePaths.add(file.getAbsolutePath());
            }
            getComponentStatistics().incrementOutboundMessages();
            messageTarget.put(inputMessage.copy(getFlowStepId(), filePaths));
        } else if (cancelOnNoFiles) {
            getComponentStatistics().incrementOutboundMessages();
            messageTarget.put(new ShutdownMessage(getFlowStepId(), true));
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
    public void flowCompleted() {
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
                log(LogLevel.INFO, "Archiving %s tp %s", srcFile.getAbsolutePath(), destDir.getAbsolutePath());
                FileUtils.moveFileToDirectory(srcFile, destDir, true);
            } catch (IOException e) {
                throw new IoException(e);
            }
        }
    }
}
