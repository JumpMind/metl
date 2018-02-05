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

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.FileInfo;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.MailSession;
import org.jumpmind.util.FormatUtils;

public class EmailWriter extends AbstractComponentRuntime {

    final static String SUBJECT = "subject";
    final static String BODY = "body";
    final static String FROM_LINE = "from.line";
    final static String TO_LINE = "to.line";
    final static String CC_LINE = "cc.line";
    final static String BCC_LINE = "bcc.line";
    final static String SUB_TYPE = "sub.type";
    final static String BODY_CHARSET = "body.charset";
    final static String SOURCE_STEP_EMAIL_ADDRESS = "source.step.email.addresses";
    final static String SOURCE_STEP_EMAIL_ADDRESS_TYPE = "source.step.email.addresses.type";
    final static String ONE_EMAIL_PER_RECIPIENT = "one.email.per.recipient";
    final static String INCLUDE_ATTACHMENT = "include.attachment";
    final static String ATTACHMENT_FILE_PATH = "attachment.file.path";

    final static String VALUE_SOURCE_STEP_EMAIL_ADDRESS_TYPE_TO = "TO";
    final static String VALUE_SOURCE_STEP_EMAIL_ADDRESS_TYPE_CC = "CC";
    final static String VALUE_SOURCE_STEP_EMAIL_ADDRESS_TYPE_BCC = "BCC";

    boolean recipientsReady = false;

    List<Message> queuedMessages = new ArrayList<>();

    List<String> recipients = new ArrayList<>();
    
    List<String> attachmentFiles = new ArrayList<>();

    MailSession mailSession;

    String subType;

    String bodyCharSet;

    @Override
    public void start() {
        super.start();
        recipientsReady = false;

        mailSession = getResourceRuntime() != null ? getResourceRuntime().reference() : null;
        if (mailSession == null) {
            info("Using global mail session because a resource was not configured");
            mailSession = new MailSession(context.getGlobalSettings());
        }
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback,
            boolean unitOfWorkBoundaryReached) {
        try {
            addToRecipients(inputMessage);
            queueMessageIfNecessary(inputMessage);
            processMessages(inputMessage, unitOfWorkBoundaryReached, callback);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    protected void queueMessageIfNecessary(Message inputMessage) {
        String sourceStepId = properties.get(SOURCE_STEP_EMAIL_ADDRESS);
        if (!recipientsReady
                && !inputMessage.getHeader().getOriginatingStepId().equals(sourceStepId)) {
            queuedMessages.add(inputMessage);
        }
    }

    protected void addToRecipients(Message inputMessage) {
        String sourceStepId = properties.get(SOURCE_STEP_EMAIL_ADDRESS);
        if (isBlank(sourceStepId)) {
            recipientsReady = true;
        } else if (inputMessage.getHeader().getOriginatingStepId().equals(sourceStepId)) {
            if (inputMessage instanceof ControlMessage) {
                recipientsReady = true;
            } else if (inputMessage instanceof TextMessage) {
                List<String> payload = ((TextMessage) inputMessage).getPayload();
                for (String emailAddress : payload) {
                    recipients.add(emailAddress);
                }
            } else if (inputMessage instanceof EntityDataMessage) {
                List<EntityData> payload = ((EntityDataMessage) inputMessage).getPayload();
                for (EntityData entityData : payload) {
                    if (entityData.size() > 0) {
                        Object firstValue = entityData.values().iterator().next();
                        if (firstValue != null) {
                            recipients.add(firstValue.toString());
                        }
                    }
                }
            }
        }
    }

    protected void processMessages(Message inputMessage, boolean unitOfWorkBoundaryReached, ISendMessageCallback callback)
            throws MessagingException {
        if (recipientsReady) {
            for (Message message : queuedMessages) {
                processMessage(message, unitOfWorkBoundaryReached, callback);
            }
            queuedMessages.clear();
            processMessage(inputMessage, unitOfWorkBoundaryReached, callback);
        }
    }

    protected void processMessage(Message inputMessage, boolean unitOfWorkBoundaryReached, ISendMessageCallback callback)
            throws MessagingException {
        String runWhen = properties.get(RUN_WHEN);
        if ((PER_UNIT_OF_WORK.equals(runWhen) && unitOfWorkBoundaryReached)
                || (PER_MESSAGE.equals(runWhen) && !(inputMessage instanceof ControlMessage)) || 
                PER_ENTITY.equals(runWhen)) {
            StringBuilder to = new StringBuilder(properties.get(TO_LINE, ""));
            StringBuilder from = new StringBuilder(properties.get(FROM_LINE, ""));
            StringBuilder cc = new StringBuilder(properties.get(CC_LINE, ""));
            StringBuilder bcc = new StringBuilder(properties.get(BCC_LINE, ""));
            String recipientType = properties.get(SOURCE_STEP_EMAIL_ADDRESS_TYPE);
            String subject = properties.get(SUBJECT, "");
            String body = properties.get(BODY, "");
            StringBuilder recipientBuilder = null;
            if (VALUE_SOURCE_STEP_EMAIL_ADDRESS_TYPE_TO.equals(recipientType)) {
                recipientBuilder = to;
            } else if (VALUE_SOURCE_STEP_EMAIL_ADDRESS_TYPE_CC.equals(recipientType)) {
                recipientBuilder = cc;
            } else if (VALUE_SOURCE_STEP_EMAIL_ADDRESS_TYPE_BCC.equals(recipientType)) {
                recipientBuilder = bcc;
            }

            StringBuilder attachmentFilePath = new StringBuilder(properties.get(ATTACHMENT_FILE_PATH,""));
            
            boolean oneMessagePerRecipient = properties.is(ONE_EMAIL_PER_RECIPIENT);
            for (String recipient : recipients) {
                recipientBuilder.append(",").append(recipient);
                if (oneMessagePerRecipient) {
                    sendEmail(inputMessage, to.toString(), cc.toString(), bcc.toString(),
                            from.toString(), subject, body, callback, attachmentFilePath.toString());

                    recipientBuilder.replace(recipientBuilder.length() - recipient.length() - 1,
                            recipientBuilder.length(), "");
                }
            }

            if (!oneMessagePerRecipient) {
                sendEmail(inputMessage, to.toString(), cc.toString(), bcc.toString(),
                        from.toString(), subject, body, callback, attachmentFilePath.toString());
            }
        }
    }

    protected void sendEmail(Message inputMessage, String to, String cc, String bcc, String from,
            String subject, String body, ISendMessageCallback callback, String attachmentFilePath) throws MessagingException {
        String runWhen = properties.get(RUN_WHEN);
        to = resolveParamsAndHeaders(to, inputMessage);
        cc = resolveParamsAndHeaders(cc, inputMessage);
        bcc = resolveParamsAndHeaders(bcc, inputMessage);
        from = resolveParamsAndHeaders(from, inputMessage);
        subject = resolveParamsAndHeaders(subject, inputMessage);
        body = resolveParamsAndHeaders(body, inputMessage);
        attachmentFilePath = resolveParamsAndHeaders(attachmentFilePath, inputMessage);
        if (PER_ENTITY.equals(runWhen)) {
            if (inputMessage instanceof TextMessage) {
                List<String> payload = ((TextMessage) inputMessage).getPayload();
                for (String text : payload) {
                    sendEmail(to.toString(), cc.toString(), bcc.toString(), from.toString(),
                            FormatUtils.replaceToken(subject, "text", text, true),
                            FormatUtils.replaceToken(body, "text", text, true), callback, attachmentFilePath.toString());
                }
            } else if (inputMessage instanceof EntityDataMessage) {
                List<EntityData> payload = ((EntityDataMessage) inputMessage).getPayload();
                for (EntityData entityData : payload) {
                    Model model = getInputModel();
                    Map<String, String> row = null;
                    if (model != null) {
                        row = model.toStringMap(entityData, true);
                    } else {
                        throw new MisconfiguredException(
                                "The input model is required if " + PER_ENTITY + " is selected");
                    }
                    sendEmail(to, cc, bcc, from, FormatUtils.replaceTokens(subject, row, true),
                            FormatUtils.replaceTokens(body, row, true), callback, attachmentFilePath);
                }
            }
        } else {
            sendEmail(to, cc, bcc, from, subject, body, callback, attachmentFilePath);
        }
    }

    protected void sendEmail(String to, String cc, String bcc, String from, String subject,
            String body, ISendMessageCallback callback, String attachmentFilePath) throws MessagingException {
        Transport transport = null;
        try {
        	bodyCharSet = properties.get(BODY_CHARSET, "utf-8");
        	if (bodyCharSet.isEmpty()) {
        		bodyCharSet = "utf-8";
        	}
        	subType = properties.get(SUB_TYPE, "plain");
        	if (subType.isEmpty()) {
        		subType = "plain";
        	}
            transport = mailSession.getTransport();
            MimeMessage mailMessage = new MimeMessage(mailSession.getSession());
            mailMessage.setSentDate(new Date());
            mailMessage.setRecipients(RecipientType.BCC, bcc);
            mailMessage.setRecipients(RecipientType.CC, cc);
            mailMessage.setRecipients(RecipientType.TO, to);
            mailMessage.setFrom(new InternetAddress(from));
            mailMessage.setSubject(subject);

            // If including an attachment
            BodyPart msgBodyPart = new MimeBodyPart();
            Multipart multipart = new MimeMultipart();
            if(properties.is(INCLUDE_ATTACHMENT)) {
            	attachmentFiles = Arrays.asList(attachmentFilePath.split("\\|"));
            	msgBodyPart.setText(body);
                multipart.addBodyPart(msgBodyPart);
                for (String filename : attachmentFiles) {
                	info("Attaching file: %s", filename.trim());
                    msgBodyPart = new MimeBodyPart();
                    DataSource source = new FileDataSource(filename.trim());
                    msgBodyPart.setDataHandler(new DataHandler(source));

                    msgBodyPart.setFileName(filename.trim());
                    multipart.addBodyPart(msgBodyPart);
        		}
                mailMessage.setContent(multipart);
            }
            else {
            	mailMessage.setText(body, bodyCharSet, subType);
            }

          	transport.sendMessage(mailMessage, mailMessage.getAllRecipients());
            
            Map<String,Serializable> header = new LinkedHashMap<>();
            header.put("to", to);
            header.put("cc", cc);
            header.put("bcc", bcc);
            header.put("from", from);
            header.put("subject", subject);
            callback.sendTextMessage(header, body);

        } finally {
            mailSession.closeTransport();
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

}
