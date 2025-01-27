package com.camunda.bigbank.worker;

import com.camunda.bigbank.entities.EmailConfig;
import com.camunda.bigbank.services.MailingService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.VariablesAsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MailWorker {

    private static final Logger log = LoggerFactory.getLogger(MailWorker.class);
    private static final Map<String, EmailConfig> EMAIL_CONFIGS = initializeEmailConfigs();

    private final MailingService mailingService;

    public MailWorker(final MailingService mailingService) {
        this.mailingService = mailingService;
    }

    /**
     * Initializes email configurations.
     *
     * @return Map of email types to their configurations
     */
    private static Map<String, EmailConfig> initializeEmailConfigs() {
        Map<String, EmailConfig> configs = new HashMap<>();
        configs.put("notifyMortgageRequestReceived", new EmailConfig("mortgage_request_email", "Mortgage Request Confirmation"));
        configs.put("additionalInformation", new EmailConfig("additional_info_request_email", "Additional information needed for your mortgage application."));
        configs.put("noReaction", new EmailConfig("no_reply_email", "Additional information needed for your mortgage application."));
        configs.put("reminder", new EmailConfig("reminder_info_email", "Reminder: Additional information needed for your mortgage application."));
        configs.put("reevaluateRules", new EmailConfig("reevaluate_rules_email", "Reevaluate the Rules", "bigbank@shvdg.nl"));
        configs.put("referCustomerSupport", new EmailConfig("refer_customer_support_email", "Help of customer support required", "bigbank@shvdg.nl"));
        configs.put("notifyMortgageRequestRejection", new EmailConfig("mortgage_request_rejected_email", "Update on your mortgage request"));
        configs.put("sendTechSupportMail", new EmailConfig("Send_Mail_To_Tech_Support", "Problem with BKR Api call in offer process"));
        configs.put("informClientAboutOffer", new EmailConfig("offer_created_email", "Mortgage offer created"));
        configs.put("sendOfferConfirmation", new EmailConfig("mortgage_offer_confirmation_email", "Offer Confirmation"));
        return configs;
    }

    /**
     * Processes an email job.
     *
     * @param variables Job variables
     * @param client    Job client
     * @param job       Activated job
     */
    @JobWorker(type = "sendMail", autoComplete = false)
    public void processEmailJobWorker(final @VariablesAsType Map<String, Object> variables, final JobClient client, final ActivatedJob job) {
        log.info("Processing email job");
        try {
            String emailType = getEmailType(variables);
            EmailConfig emailConfig = getEmailConfig(emailType);
            String recipient = determineRecipient(emailConfig, variables);

            sendEmail(emailConfig, variables, recipient);
            completeJob(client, job, emailType);
        } catch (Exception e) {
            failJob(client, job, e.getMessage());
        }
    }

    /**
     * Retrieves email type from variables.
     *
     * @param variables Job variables
     * @return Email type
     * @throws Exception if email type is not provided
     */
    private String getEmailType(final Map<String, Object> variables) throws Exception {
        String emailType = (String) variables.getOrDefault("EmailType", null);
        if (emailType == null) {
            throw new Exception("No email type provided");
        }
        return emailType;
    }

    /**
     * Retrieves email configuration for given email type.
     *
     * @param emailType Email type
     * @return Email configuration
     * @throws Exception if email type is unknown
     */
    private EmailConfig getEmailConfig(final String emailType) throws Exception {
        EmailConfig config = EMAIL_CONFIGS.get(emailType);
        if (config == null) {
            throw new Exception("Unknown email type: " + emailType);
        }
        return config;
    }

    /**
     * Determines the recipient of the email.
     *
     * @param config    Email configuration
     * @param variables Job variables
     * @return Recipient email address
     */
    private String determineRecipient(final EmailConfig config, final Map<String, Object> variables) {
        return config.getRecipient() != null ? config.getRecipient() :
                (String) variables.getOrDefault("customerEmail", null);
    }

    /**
     * Sends an email using the mailing service.
     *
     * @param config    Email configuration
     * @param variables Job variables
     * @param recipient Recipient email address
     */
    private void sendEmail(final EmailConfig config, final Map<String, Object> variables, final String recipient) {
        mailingService.sendMail(config.getTemplate(), variables, config.getSubject(), recipient);
    }

    /**
     * Completes the job after successful email sending.
     *
     * @param client    Job client
     * @param job       Activated job
     * @param emailType Email type
     */
    private void completeJob(final JobClient client, final ActivatedJob job, final String emailType) {
        log.info("Email sent of type: {}", emailType);
        client.newCompleteCommand(job.getKey()).send().join();
    }

    /**
     * Fails the job in case of an error.
     *
     * @param client       Job client
     * @param job          Activated job
     * @param errorMessage Error message
     */
    private void failJob(final JobClient client, final ActivatedJob job, final String errorMessage) {
        log.error("Failed to process email job: {}", errorMessage);
        client.newFailCommand(job)
                .retries(job.getRetries() - 1)
                .errorMessage(errorMessage)
                .send()
                .join();
    }
}