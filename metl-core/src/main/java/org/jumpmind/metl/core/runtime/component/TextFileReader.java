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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IStreamable;
import org.jumpmind.metl.core.runtime.resource.LocalFile;
import org.jumpmind.util.FormatUtils;

public class TextFileReader extends AbstractComponentRuntime {

    public static final String TYPE = "Text File Reader";

    public static final String ACTION_NONE = "None";
    public static final String ACTION_DELETE = "Delete";
    public static final String ACTION_ARCHIVE = "Archive";

    public final static String SETTING_GET_FILE_FROM_MESSAGE = "get.file.name.from.message";

    public final static String SETTING_RELATIVE_PATH = "textfilereader.relative.path";

    public static final String SETTING_MUST_EXIST = "textfilereader.must.exist";

    public static final String SETTING_ROWS_PER_MESSAGE = "textfilereader.text.rows.per.message";

    public final static String SETTING_ACTION_ON_SUCCESS = "action.on.success";

    public final static String SETTING_ARCHIVE_ON_SUCCESS_PATH = "archive.on.success.path";

    public final static String SETTING_ACTION_ON_ERROR = "action.on.error";

    public final static String SETTING_ARCHIVE_ON_ERROR_PATH = "archive.on.error.path";

    public final static String SETTING_ENCODING = "textfilereader.encoding";

    public static final String SETTING_HEADER_LINES_TO_SKIP = "textfilereader.text.header.lines.to.skip";

    String relativePathAndFile;

    boolean mustExist;

    boolean getFileNameFromMessage = false;

    boolean unitOfWorkLastMessage = false;

    String actionOnSuccess = ACTION_NONE;

    String archiveOnSuccessPath;

    String actionOnError = ACTION_NONE;

    String archiveOnErrorPath;

    int textRowsPerMessage = 10000;

    int textHeaderLinesToSkip;

    String encoding = "UTF-8";

    List<String> filesRead;

    @Override
    protected void start() {
        filesRead = new ArrayList<String>();
        Component component = getComponent();
        relativePathAndFile = component.get(SETTING_RELATIVE_PATH, relativePathAndFile);
        mustExist = component.getBoolean(SETTING_MUST_EXIST, mustExist);
        textRowsPerMessage = component.getInt(SETTING_ROWS_PER_MESSAGE, textRowsPerMessage);
        textHeaderLinesToSkip = component.getInt(SETTING_HEADER_LINES_TO_SKIP, textHeaderLinesToSkip);
        textRowsPerMessage = component.getInt(SETTING_ROWS_PER_MESSAGE, textRowsPerMessage);
        getFileNameFromMessage = component.getBoolean(SETTING_GET_FILE_FROM_MESSAGE, getFileNameFromMessage);
        actionOnSuccess = component.get(SETTING_ACTION_ON_SUCCESS, actionOnSuccess);
        actionOnError = component.get(SETTING_ACTION_ON_ERROR, actionOnError);
        archiveOnErrorPath = FormatUtils.replaceTokens(component.get(SETTING_ARCHIVE_ON_ERROR_PATH), context.getFlowParametersAsString(),
                true);
        archiveOnSuccessPath = FormatUtils.replaceTokens(component.get(SETTING_ARCHIVE_ON_SUCCESS_PATH), context.getFlowParametersAsString(),
                true);
        encoding = component.get(SETTING_ENCODING, encoding);
    }
        
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        List<String> files = getFilesToRead(inputMessage);
        processFiles(files, callback, unitOfWorkBoundaryReached);
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
        IStreamable streamable = getResourceReference();
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
        ArrayList<String> files = new ArrayList<String>();
        if (getFileNameFromMessage) {
            List<String> fullyQualifiedFiles = inputMessage.getPayload();
            String path = getResourceRuntime().getResourceRuntimeSettings().get(LocalFile.LOCALFILE_PATH);
            for (String fullyQualifiedFile : fullyQualifiedFiles) {
                if (fullyQualifiedFile.startsWith(path)) {
                    files.add(fullyQualifiedFile.substring(path.length()));
                } else {
                    files.add(fullyQualifiedFile);
                }
            }
        } else {
            files.add(relativePathAndFile);
        }
        return files;
    }

    private void processFiles(List<String> files, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {
        int linesInMessage = 0;
        ArrayList<String> payload = new ArrayList<String>();

        filesRead.addAll(files);

        for (String file : files) {
            InputStream inStream = null;
            BufferedReader reader = null;
            int currentFileLinesRead = 0;
            String currentLine;
            try {
                info("Reading file: %s", file);
                IStreamable resource = (IStreamable) getResourceReference();
                String filePath = FormatUtils.replaceTokens(file, context.getFlowParametersAsString(), true);
                inStream = resource.getInputStream(filePath, mustExist);
                reader = new BufferedReader(new InputStreamReader(inStream, encoding));

                while ((currentLine = reader.readLine()) != null) {
                    currentFileLinesRead++;
                    if (linesInMessage == textRowsPerMessage) {
                        initAndSendMessage(payload, callback, false);
                        linesInMessage = 0;
                        payload = new ArrayList<String>();
                    }
                    if (currentFileLinesRead > textHeaderLinesToSkip) {
                        getComponentStatistics().incrementNumberEntitiesProcessed();
                        payload.add(currentLine);
                        linesInMessage++;
                    }
                }
                initAndSendMessage(payload, callback, unitOfWorkLastMessage);
                linesInMessage = 0;
            } catch (IOException e) {
                throw new IoException("Error reading from file " + e.getMessage());
            } finally {
                IOUtils.closeQuietly(reader);
                IOUtils.closeQuietly(inStream);
            }
        }

    }

    private void initAndSendMessage(ArrayList<String> payload, ISendMessageCallback callback, boolean lastMessage) {
        callback.sendMessage(payload, lastMessage);
    }
}
