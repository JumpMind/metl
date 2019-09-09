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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import javax.mail.Store;

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.MailSession;

public class EmailReader extends AbstractComponentRuntime {

    final static String EMAIL_SOURCE_FOLDER = "email.source.folder";
    final static String EMAIL_TARGET_FOLDER = "email.target.folder";
    final static String FILE_ATTACHMENT_TARGET_PATH = "file.attachment.target.path";
    final static String SUBJECT_SEARCH = "subject.search";
    final static String EMAIL_FROM_SEARCH = "email.from.search";
    final static String READ_ONLY_UNREAD_MESSAGES = "read.only.unread.messages";
    final static String MARK_ALL_AS_READ = "mark.all.as.read";
    
    MailSession mailSession;
    
    @Override
    public void start() {
        super.start();

        mailSession = getResourceRuntime() != null ? getResourceRuntime().reference() : null;
        if (mailSession == null) {
            info("Using global mail session because a resource was not configured");
            mailSession = new MailSession(context.getGlobalSettings());
        }
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        String runWhen = properties.get(RUN_WHEN);
        if ((PER_UNIT_OF_WORK.equals(runWhen) && unitOfWorkBoundaryReached)
                || (PER_MESSAGE.equals(runWhen) && !(inputMessage instanceof ControlMessage)) || 
                PER_ENTITY.equals(runWhen)) {
            try {
	        	String sourceFolder = properties.get(EMAIL_SOURCE_FOLDER, "INBOX");
	        	String targetFolder = properties.get(EMAIL_TARGET_FOLDER, "");
	        	String attachmentTargetPath = properties.get(FILE_ATTACHMENT_TARGET_PATH, "");
	        	String subjectSearch = properties.get(SUBJECT_SEARCH, "");
	        	String emailFromSearch = properties.get(EMAIL_FROM_SEARCH, "");
	        	boolean moveMailToFolder = false;
	        	boolean getOnlyUnreadMessages = properties.is(READ_ONLY_UNREAD_MESSAGES);
	        	boolean markAllMailAsRead = properties.is(MARK_ALL_AS_READ);
	        	boolean downloadAttachments = false;
	        	Store store = null;
	        	boolean searchBySubject = false;
	        	boolean searchByFrom = false;
	            ArrayList<String> payload = new ArrayList<String>();
	            
	            sourceFolder = resolveParamsAndHeaders(sourceFolder, inputMessage);
	            targetFolder = resolveParamsAndHeaders(targetFolder, inputMessage);
	            attachmentTargetPath = resolveParamsAndHeaders(attachmentTargetPath, inputMessage);
	            subjectSearch = resolveParamsAndHeaders(subjectSearch, inputMessage);
	            emailFromSearch = resolveParamsAndHeaders(emailFromSearch, inputMessage);
	        	
	        	if (!subjectSearch.isEmpty()) {
	        		searchBySubject = true;
	        	}
	        	if (!emailFromSearch.isEmpty()) {
	        		searchByFrom = true;
	        	}
	        	if (!targetFolder.isEmpty()) {
	        		moveMailToFolder = true;
	        	}
	        	if (!attachmentTargetPath.isEmpty()) {
	        		downloadAttachments = true;
	        	}
	
	        	store = mailSession.getStore();
	        	
	        	Folder rptFolder = null;
	        	if (!store.getFolder(targetFolder).exists()) {
	        		store.getFolder(targetFolder).create(Folder.HOLDS_MESSAGES);
	        	}
	        	
	        	Folder emailFolder = store.getFolder(sourceFolder);
	        	rptFolder = store.getFolder(targetFolder);
	        	Flags flagDeleted = new Flags(Flags.Flag.DELETED);
	        	Flags flagSeen = new Flags(Flags.Flag.SEEN);
	       	
	        	if (emailFolder != null && emailFolder.exists()) {
	        		javax.mail.Message[] messages = null;
	        		emailFolder.open(Folder.READ_WRITE);
		        	
	        		if (getOnlyUnreadMessages) {
	        			messages = emailFolder.search(new FlagTerm(flagSeen, false));
	        		} else {
	        			messages = emailFolder.getMessages();
	        		}
	        		
		        	List<javax.mail.Message> tmpList = new ArrayList<>();
	
		        	for (int i = 0; i < messages.length; i++) {
		                Map<String, Serializable> headers = new HashMap<>(1);
		                headers.putAll(inputMessage.getHeader());
		        		javax.mail.Message msg = messages[i];
		        		boolean fromAddrFound = false;
		        		Multipart multipart = null;
		        		int multipartCount = 0;
		        		
		        		if (msg.isMimeType("multipart/*")) {
			        		multipart = (Multipart) msg.getContent();
			        		multipartCount = multipart.getCount();
		        		}
		        		
		        		if (markAllMailAsRead) {
		        			emailFolder.setFlags(new javax.mail.Message[] {msg}, new Flags(Flags.Flag.SEEN), true);
		        		}
		                
		        		if (searchByFrom) {
		        			Address[] fromAddresses = msg.getFrom();
		        			InternetAddress searchAddress = new InternetAddress(emailFromSearch);
		        			
		        			for (Address addr : fromAddresses) {
		        				if (searchAddress.equals(addr)) {
		        					fromAddrFound = true;
		        					break;
		        				}
		        			}
		        		}
	
		        		if ((!searchBySubject && !searchByFrom)
		        			|| (searchBySubject && msg.getSubject().contains(subjectSearch) && !searchByFrom)
		        			|| (!searchBySubject && searchByFrom && fromAddrFound)
		        			|| (searchBySubject && msg.getSubject().contains(subjectSearch) && searchByFrom && fromAddrFound)) {
	
			                payload = new ArrayList<String>();
		        			emailFolder.setFlags(new javax.mail.Message[] {msg}, new Flags(Flags.Flag.SEEN), true);
		            		tmpList.add(msg);

		            		payload.add(getTextFromMessage(msg));

		            		for (int m = 0; m < multipartCount; m++) {
		            			MimeBodyPart bodyPart = (MimeBodyPart) multipart.getBodyPart(m);
		            			if (downloadAttachments && Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
		            				File fl = new File(bodyPart.getFileName());
		            				String fileName = fl.getName();
		            				if (!attachmentTargetPath.endsWith("/")) {
		            					attachmentTargetPath += "/";
		            				}
		            				File newFile = new File(attachmentTargetPath);
		            				if (!newFile.exists()) {
		            					if (!newFile.mkdirs()) {
		            						throw new RuntimeException("The target path did not exist and could not be created. May want to create the target path manually.");
		            					}
		            				}
		            				bodyPart.saveFile(attachmentTargetPath + fileName);
		            			}
		            		}
		            		
			                headers.put("email.from", msg.getFrom()[0].toString());
			                headers.put("email.subject", msg.getSubject());
			                headers.put("email.sent.date", msg.getSentDate().toString());
			                headers.put("email.received.date", msg.getReceivedDate().toString());
	                        callback.sendTextMessage(headers, payload);
		        		}
		        		
		                checkForInterruption();
		        	}
		        	
		        	javax.mail.Message[] rptMsgs = tmpList.toArray(new javax.mail.Message[tmpList.size()]);
		        	
		        	if (moveMailToFolder) {
			        	emailFolder.copyMessages(rptMsgs, rptFolder);
			    		emailFolder.setFlags(rptMsgs,  flagDeleted,  true);
			        	emailFolder.expunge();
		        	}
	        	}
	        } catch (MessagingException e) {
	            throw new RuntimeException(e);
	        } catch (IOException e) {
	            throw new RuntimeException(e);
		    } finally {
		        mailSession.closeTransport();
		    }     
        }
    }

    private String getTextFromMessage(javax.mail.Message message) throws IOException, MessagingException {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    private String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart) throws IOException, MessagingException {

        int count = mimeMultipart.getCount();
        if (count == 0)
            throw new MessagingException("Multipart with no body parts not supported.");
        boolean multipartAlt = new ContentType(mimeMultipart.getContentType()).match("multipart/alternative");
        if (multipartAlt)
            // alternatives appear in an order of increasing 
            // faithfulness to the original content. Customize as req'd.
            return getTextFromBodyPart(mimeMultipart.getBodyPart(count - 1));
        String result = "";
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            result += getTextFromBodyPart(bodyPart);
        }
        return result;
    }

    private String getTextFromBodyPart(
            BodyPart bodyPart) throws IOException, MessagingException {

        String result = "";
        if (bodyPart.isMimeType("text/plain")) {
            result = (String) bodyPart.getContent();
        } else if (bodyPart.isMimeType("text/html")) {
            String html = (String) bodyPart.getContent();
            result = org.jsoup.Jsoup.parse(html).text();
        } else if (bodyPart.getContent() instanceof MimeMultipart){
            result = getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
        }
        return result;
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

}
