package com.camunda.bigbank.services;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import io.micrometer.common.util.StringUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/**
 * Service for sending emails using SendGrid.
 */
@Service
public class MailingService {

    private static final Logger LOG = LoggerFactory.getLogger(MailingService.class);

    private final SpringTemplateEngine templateEngine;
    private final SendGrid sendGrid;
    private final String fromEmail;

    /**
     * Constructs a new MailingService.
     *
     * @param sendGrid  the SendGrid client
     * @param fromEmail the sender's email address
     */
    public MailingService(final SendGrid sendGrid, @Value("${sendgrid.from-email}") String fromEmail, final SpringTemplateEngine templateEngine) {
        this.sendGrid = sendGrid;
        this.fromEmail = fromEmail;
        this.templateEngine = templateEngine;
    }

    /**
     * Sends an email using SendGrid.
     *
     * @param template  the template name
     * @param variables the variables for the template
     * @param subject   the email subject
     * @param to        the recipient's email address
     */
    public void sendMail(final String template, final Map<String, Object> variables, final String subject, final String to) {
        Context context = new Context();
        context.setVariables(variables);
        String body = templateEngine.process(template, context);

        Email from = new Email(fromEmail);
        Email toEmail = new Email(to);
        Content content = new Content("text/html", body);
        Mail mail = new Mail(from, subject, toEmail, content);

        addAttachment(mail, variables);

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);
            LOG.info("Email sent successfully. Status code: {}", response.getStatusCode());
        } catch (Exception e) {
            LOG.error("Error occurred when attempting to send mail: {}", e.getMessage());
        }
    }

    /**
     * Adds an attachment to the email if an attachment path is provided in the variables.
     *
     * @param mail      the Mail object to add the attachment to
     * @param variables the map of variables that may contain an attachment path
     */
    private void addAttachment(final Mail mail, final Map<String, Object> variables) {
        String attachmentPath = (String) variables.get("attachmentPath");
        if (StringUtils.isBlank(attachmentPath)) {
            return;
        }

        try {
            File attachmentFile = new File(attachmentPath);
            byte[] attachmentContent = FileUtils.readFileToByteArray(attachmentFile);
            String attachmentContentBase64 = Base64.getEncoder().encodeToString(attachmentContent);

            Attachments attachments = new Attachments();
            attachments.setContent(attachmentContentBase64);
            attachments.setType("application/pdf");
            attachments.setFilename("file.pdf");
            attachments.setDisposition("attachment");
            mail.addAttachments(attachments);

            LOG.info("Attachment added successfully: {}", attachmentFile.getName());
        } catch (IOException e) {
            LOG.error("Error reading attachment file: {}", e.getMessage());
        }
    }
}