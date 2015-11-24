package org.jumpmind.metl.core.runtime.component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.metl.core.runtime.resource.LocalFile;
import org.jumpmind.util.FormatUtils;

public abstract class AbstractFileReader extends AbstractComponentRuntime {

    public static final String ACTION_NONE = "None";
    public static final String ACTION_DELETE = "Delete";
    public static final String ACTION_ARCHIVE = "Archive";

    public final static String SETTING_GET_FILE_FROM_MESSAGE = "get.file.name.from.message";
    public final static String SETTING_RELATIVE_PATH = "textfilereader.relative.path";
    public static final String SETTING_MUST_EXIST = "textfilereader.must.exist";
    public final static String SETTING_ACTION_ON_SUCCESS = "action.on.success";
    public final static String SETTING_ARCHIVE_ON_SUCCESS_PATH = "archive.on.success.path";
    public final static String SETTING_ACTION_ON_ERROR = "action.on.error";
    public final static String SETTING_ARCHIVE_ON_ERROR_PATH = "archive.on.error.path";

    String relativePathAndFile;
    boolean mustExist;
    boolean getFileNameFromMessage = false;
    boolean unitOfWorkLastMessage = false;
    String actionOnSuccess = ACTION_NONE;
    String archiveOnSuccessPath;
    String actionOnError = ACTION_NONE;
    String archiveOnErrorPath;
    String runWhen = PER_UNIT_OF_WORK;
    List<String> filesRead;

	protected void init() {
        filesRead = new ArrayList<String>();
        Component component = getComponent();
        relativePathAndFile = component.get(SETTING_RELATIVE_PATH, relativePathAndFile);
        mustExist = component.getBoolean(SETTING_MUST_EXIST, mustExist);
        getFileNameFromMessage = component.getBoolean(SETTING_GET_FILE_FROM_MESSAGE, getFileNameFromMessage);
        actionOnSuccess = component.get(SETTING_ACTION_ON_SUCCESS, actionOnSuccess);
        actionOnError = component.get(SETTING_ACTION_ON_ERROR, actionOnError);
        archiveOnErrorPath = FormatUtils.replaceTokens(component.get(SETTING_ARCHIVE_ON_ERROR_PATH), context.getFlowParametersAsString(),
                true);
        archiveOnSuccessPath = FormatUtils.replaceTokens(component.get(SETTING_ARCHIVE_ON_SUCCESS_PATH), context.getFlowParametersAsString(),
                true);
        runWhen = component.get(RUN_WHEN, runWhen);
	}
	
    @Override
    public boolean supportsStartupMessages() {
        return true;
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
        IDirectory streamable = getResourceReference();
        for (String srcFile : filesRead) {
            if (streamable.delete(srcFile)) {
                warn("Deleted %s", srcFile);
            } else {
                warn("Failed to delete %s", srcFile);
            }
        }
    }

    protected void archive(String archivePath) {
        String path = getResourceRuntime().getResourceRuntimeSettings().get(LocalFile.LOCALFILE_PATH);
        File destDir = new File(path, archivePath);
        for (String srcFileName : filesRead) {
            try {
                File srcFile = new File(path, srcFileName);
                File targetFile = new File(destDir, srcFile.getName());
                if (targetFile.exists()) {
                    info("The msgTarget file already exists.   Deleting it in order to archive a new file.");
                    FileUtils.deleteQuietly(targetFile);
                }
                info("Archiving %s tp %s", srcFile, destDir.getAbsolutePath());
                FileUtils.moveFileToDirectory(srcFile, destDir, true);
            } catch (IOException e) {
                throw new IoException(e);
            }
        }
    }


    private List<String> getFilesToRead(Message inputMessage) {
        ArrayList<String> files = null;
        if (getFileNameFromMessage) {
            files = inputMessage.getPayload();
        } else {
            files = new ArrayList<String>(1);
            files.add(relativePathAndFile);
        }
        return files;
    }

}
