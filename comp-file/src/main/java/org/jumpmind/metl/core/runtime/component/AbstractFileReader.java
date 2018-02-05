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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.metl.core.runtime.resource.LocalFile;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;

public abstract class AbstractFileReader extends AbstractComponentRuntime {

    public static final String ACTION_NONE = "None";
    public static final String ACTION_DELETE = "Delete";
    public static final String ACTION_ARCHIVE = "Archive";

    public final static String SETTING_GET_FILE_FROM_MESSAGE = "get.file.name.from.message";
    public final static String SETTING_RELATIVE_PATH = "relative.path";
    public static final String SETTING_MUST_EXIST = "must.exist";
    public final static String SETTING_ACTION_ON_SUCCESS = "action.on.success";
    public final static String SETTING_ARCHIVE_ON_SUCCESS_PATH = "archive.on.success.path";
    public final static String SETTING_ACTION_ON_ERROR = "action.on.error";
    public final static String SETTING_ARCHIVE_ON_ERROR_PATH = "archive.on.error.path";
    public final static String SETTING_CNTRL_MSG_ON_EOF = "control.message.on.eof";

    String relativePathAndFile;
    boolean mustExist;
    boolean getFileNameFromMessage = false;
    boolean unitOfWorkLastMessage = false;
    boolean controlMessageOnEof = false;
    String actionOnSuccess = ACTION_NONE;
    String archiveOnSuccessPath;
    String actionOnError = ACTION_NONE;
    String archiveOnErrorPath;
    String runWhen = PER_UNIT_OF_WORK;
    List<String> filesRead;
    IDirectory directory;

	protected void init() {
        filesRead = new ArrayList<String>();
        TypedProperties properties = getTypedProperties();
        relativePathAndFile = properties.get(SETTING_RELATIVE_PATH, relativePathAndFile);
        mustExist = properties.is(SETTING_MUST_EXIST, mustExist);
        getFileNameFromMessage = properties.is(SETTING_GET_FILE_FROM_MESSAGE, getFileNameFromMessage);
        actionOnSuccess = properties.get(SETTING_ACTION_ON_SUCCESS, actionOnSuccess);
        actionOnError = properties.get(SETTING_ACTION_ON_ERROR, actionOnError);
        archiveOnErrorPath = FormatUtils.replaceTokens(properties.get(SETTING_ARCHIVE_ON_ERROR_PATH), context.getFlowParameters(),
                true);
        archiveOnSuccessPath = FormatUtils.replaceTokens(properties.get(SETTING_ARCHIVE_ON_SUCCESS_PATH), context.getFlowParameters(),
                true);
        runWhen = properties.get(RUN_WHEN, runWhen);
        controlMessageOnEof = properties.is(SETTING_CNTRL_MSG_ON_EOF, controlMessageOnEof);
        
        if (getComponent().getResource() == null) {
            throw new MisconfiguredException(
                    "A resource has not been selected.  The resource is required if not configured to get the file name from the inbound message");
        }
        
        directory = getResourceReference();
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
        
        if (directory != null) {
            directory.close(false);
            directory = null;
        }
    }

    @Override
    public void flowCompleted(boolean cancelled) {
        if (ACTION_ARCHIVE.equals(actionOnSuccess)) {
            archive(archiveOnSuccessPath);
        } else if (ACTION_DELETE.equals(actionOnSuccess)) {
            deleteFiles();
        }
        directory.close(true);
        directory = null;
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
                info("Archiving %s to %s", srcFile, destDir.getAbsolutePath());
                FileUtils.moveFileToDirectory(srcFile, destDir, true);
            } catch (IOException e) {
                throw new IoException(e);
            }
        }
    }


    protected List<String> getFilesToRead(Message inputMessage) {
        ArrayList<String> files = null;
        if (getFileNameFromMessage) {
            if (inputMessage instanceof TextMessage) {
                files = ((TextMessage)inputMessage).getPayload();
            } else {
                files = new ArrayList<>(0);
            }
        } else {
            files = new ArrayList<String>(1);
            files.add(relativePathAndFile);
        }
        return files;
    }

}
