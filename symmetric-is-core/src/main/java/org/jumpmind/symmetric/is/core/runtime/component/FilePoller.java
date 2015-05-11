package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.ShutdownMessage;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.LocalFile;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceCategory;

@ComponentDefinition(
        typeName = FilePoller.TYPE,
        category = ComponentCategory.READER,
        iconImage = "filepoller.png",
        outgoingMessage = MessageType.TEXT,
        resourceCategory = ResourceCategory.STREAMABLE)
public class FilePoller extends AbstractComponentRuntime {

    public static final String TYPE = "File Poller";

    @SettingDefinition(order = 10, required = true, type = Type.TEXT, label = "File Pattern")
    public final static String SETTING_FILE_PATTERN = "file.pattern";
    
    @SettingDefinition(order = 20, type = Type.BOOLEAN, defaultValue="false", label = "Search Recursively")
    public final static String SETTING_RECURSE = "recurse";
    
    @SettingDefinition(order = 30, type = Type.BOOLEAN, defaultValue="true", label = "Cancel On No Files")
    public final static String SETTING_CANCEL_ON_NO_FILES = "cancel.on.no.files";
    
    @SettingDefinition(order = 35, type = Type.BOOLEAN, defaultValue="false", label = "Archive On Success")
    public final static String SETTING_ARCHIVE_ON_SUCCESS = "archive.on.success";
    
    @SettingDefinition(order = 40, type = Type.TEXT, label = "Archive On Success Path")
    public final static String SETTING_ARCHIVE_ON_SUCCESS_PATH = "archive.on.success.path";
    
    @SettingDefinition(order = 45, type = Type.BOOLEAN, defaultValue="false", label = "Archive On Error")
    public final static String SETTING_ARCHIVE_ON_ERROR = "archive.on.error";

    @SettingDefinition(order = 50, type = Type.TEXT, label = "Archive On Error Path")
    public final static String SETTING_ARCHIVE_ON_ERROR_PATH = "archive.on.error.path";


    @SettingDefinition(order = 60, type = Type.BOOLEAN, defaultValue="false", label = "Use Trigger File")
    public final static String SETTING_USE_TRIGGER_FILE = "use.trigger.file";

    @SettingDefinition(order = 70, type = Type.TEXT, label = "Relative Trigger File Path")
    public final static String SETTING_TRIGGER_FILE_PATH = "trigger.file.path";
    
    String filePattern;
    
    String triggerFilePath;
    
    boolean useTriggerFile = false;
    
    boolean recurse = false;
    
    boolean cancelOnNoFiles = true;
    
    boolean archiveOnSuccess = false;
    
    String archiveOnSuccessPath;
    
    boolean archiveOnError = false;
    
    String archiveOnErrorPath;
    
    IOFileFilter fileFilter;
    
    ArrayList<File> filesSent = new ArrayList<File>();
    
    @Override
    public void start() {       
        Component component = getComponent();
        Resource resource = component.getResource();
        if (!resource.getType().equals(LocalFile.TYPE)) {
            throw new IllegalStateException(String.format("The resource must be of type %s",LocalFile.TYPE));
        }
                
        filePattern = component.get(SETTING_FILE_PATTERN);
        triggerFilePath = component.get(SETTING_TRIGGER_FILE_PATH);
        useTriggerFile = component.getBoolean(SETTING_USE_TRIGGER_FILE, useTriggerFile);
        recurse = component.getBoolean(SETTING_RECURSE, recurse);
        cancelOnNoFiles = component.getBoolean(SETTING_CANCEL_ON_NO_FILES, cancelOnNoFiles);
        archiveOnSuccess = component.getBoolean(SETTING_ARCHIVE_ON_SUCCESS, archiveOnSuccess);
        archiveOnError = component.getBoolean(SETTING_ARCHIVE_ON_ERROR, archiveOnError);
        archiveOnErrorPath = component.get(SETTING_ARCHIVE_ON_ERROR_PATH);
        archiveOnSuccessPath = component.get(SETTING_ARCHIVE_ON_SUCCESS_PATH);
        
        fileFilter = createIOFileFilter();
        
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        Resource resource = getComponent().getResource();
        String path = resource.get(LocalFile.LOCALFILE_PATH);
        if (useTriggerFile) {
            File triggerFile = new File(path, triggerFilePath);
            if (triggerFile.exists()) {
                pollForFiles(path, inputMessage, messageTarget);
                FileUtils.deleteQuietly(triggerFile);
            } else if (cancelOnNoFiles) {
                messageTarget.put(new ShutdownMessage(getFlowStepId(), true));
            }
        } else {
            pollForFiles(path, inputMessage, messageTarget);
        }
    }
    
    protected void pollForFiles(String path, Message inputMessage, IMessageTarget messageTarget) {
        File pathDir = new File(path);
        ArrayList<String> filePaths = new ArrayList<String>();
        File[] files = pathDir.listFiles((FilenameFilter)fileFilter);
        if (files.length > 0) {
            for (File file : files) {
                filesSent.add(file);
                log(LogLevel.INFO,  "File polled: " + file.getAbsolutePath());
                String filePath = file.getAbsolutePath(); 
                filePaths.add(filePath.substring(pathDir.getAbsolutePath().length()));
            }
            messageTarget.put(inputMessage.copy(getFlowStepId(), filePaths));
        } else if (cancelOnNoFiles) {
            messageTarget.put(new ShutdownMessage(getFlowStepId(), true));
        }        
    }
    
    @Override
    public void flowCompletedWithErrors(Throwable myError) {
        if (archiveOnError) {
            archive(archiveOnErrorPath);
        }
    }
    
    @Override
    public void flowCompleted() {
        if (archiveOnSuccess) {
            archive(archiveOnSuccessPath);
        }
    }
    
    protected void archive(String archivePath) {
        Resource resource = getComponent().getResource();
        String path = resource.get(LocalFile.LOCALFILE_PATH);
        File destDir = new File(path, archivePath);
        for (File srcFile : filesSent) {
            try {
                FileUtils.moveFileToDirectory(srcFile, destDir, true);
            } catch (IOException e) {
                throw new IoException(e);
            }                
        }
    }
    
    protected IOFileFilter createIOFileFilter() {
        String[] includes = StringUtils.isNotBlank(filePattern) ? filePattern.split(",") : new String[] {"*"};
        IOFileFilter filter = new WildcardFileFilter(includes);
        if (!recurse) {
            List<IOFileFilter> fileFilters = new ArrayList<IOFileFilter>();
            fileFilters.add(filter);
            fileFilters.add(new NotFileFilter(FileFilterUtils.directoryFileFilter()));
            filter = new AndFileFilter(fileFilters);
        } else {
            List<IOFileFilter> fileFilters = new ArrayList<IOFileFilter>();
            fileFilters.add(filter);
            fileFilters.add(FileFilterUtils.directoryFileFilter());
            filter = new OrFileFilter(fileFilters);
        }
        return filter;
    }

}
