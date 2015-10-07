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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IStreamable;

public class UnZip extends AbstractComponentRuntime {
	public static final String TYPE = "UnZip";

	public final static String SETTING_TARGET_PATH = "target.path";

	public final static String SETTING_TARGET_SUB_DIR = "target.sub.dir";

	public static final String SETTING_MUST_EXIST = "must.exist";

	public final static String SETTING_DELETE_ON_COMPLETE = "delete.on.complete";

	public final static String SETTING_ENCODING = "encoding";

	String targetPath;

	boolean mustExist;

	String encoding = "UTF-8";

	boolean deleteOnComplete = false;

	boolean targetSubDir = false;
	
	List<String> fileNames;

	@Override
	protected void start() {
		Component component = getComponent();
		deleteOnComplete = component.getBoolean(SETTING_DELETE_ON_COMPLETE, deleteOnComplete);
		targetPath = component.get(SETTING_TARGET_PATH, targetPath);
		targetSubDir = component.getBoolean(SETTING_TARGET_SUB_DIR, targetSubDir); 
		mustExist = component.getBoolean(SETTING_MUST_EXIST, mustExist);
		encoding = component.get(SETTING_ENCODING, encoding);
		fileNames = new ArrayList<String>();
	}

	@Override
	public void handle(Message inputMessage, ISendMessageCallback messageTarget, boolean unitOfWorkBoundaryReached) {
		List<String> files = inputMessage.getPayload();
		if (files != null) {
            fileNames.addAll(files);
        }
		
        if (unitOfWorkBoundaryReached) {
        	IStreamable streamable = getResourceReference();
    		ZipInputStream zis = null;
            
    		for (String fileName : fileNames) {
    			log(LogLevel.INFO, "Preparing to extract file : %s", fileName);
    		    File file = new File(fileName);
    			if (mustExist && !file.exists()) {
                    throw new IoException(String.format("Could not find file to extract: %s", fileName));
                }
    			if (file.exists()) {
    				try {
    					ZipFile zipFile = new ZipFile(file);
    					InputStream in = null;
    					OutputStream out = null;
    					try {
    						String finalPath = targetSubDir 
    								? targetPath + File.separator + FilenameUtils.removeExtension(file.getName()) + File.separator 
    								: targetPath + File.separator; 
    						
    						for (Enumeration<? extends ZipEntry> e = zipFile.entries();
    						        e.hasMoreElements();) {
    							ZipEntry entry = e.nextElement();
    							log(LogLevel.INFO, entry.getName());
    								
								if (!entry.isDirectory()) {
    								out = streamable.getOutputStream(finalPath + entry.getName(), false);
    								in = zipFile.getInputStream(entry);
		    					    IOUtils.copy(in, out);
								}
	    					}
    					} finally {
    					    IOUtils.closeQuietly(in);
    					    IOUtils.closeQuietly(out);
    					    zipFile.close();
    					}
    					if (deleteOnComplete) {
    						FileUtils.deleteQuietly(file);
    		            }       
	                } catch (IOException e) {
	                    throw new IoException(e);
	                }
	                log(LogLevel.INFO, "Extracted %s", fileName);
	                getComponentStatistics().incrementNumberEntitiesProcessed();
        		}
    		}
        }
	}
	
	
}
