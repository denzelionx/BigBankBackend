package com.camunda.bigbank.worker;


import com.camunda.bigbank.entities.MortgageRequest;
import com.camunda.bigbank.services.MailingService;
import com.camunda.bigbank.services.MortgageRequestService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Log
public class SendOfferToClientWorker {


    private final MortgageRequestService mortgageService;
    private final MailingService mailingService;

    /**
     * Constructs a new MortgageController with the specified services.
     *
     * @param mortgageService the service for managing mortgage requests
     * @param mailingService  the service for managing emails
     */
    public SendOfferToClientWorker(final MortgageRequestService mortgageService, final MailingService mailingService) {
        this.mortgageService = mortgageService;
        this.mailingService = mailingService;
    }

    /***
     * This worker is listening to the Send offer to client Task in the intrest-rate-request-process
     *
     * TODO: Make it save the current proposal, display it on the site where the customer can approve or reject the offer
     *
     * @param client Client the task is running on
     * @param job Current job being executed
     */
    @JobWorker(type = "sendOfferToClientWorker", autoComplete = false)
    public void runsendOfferToClientWorkerTask(final JobClient client, final ActivatedJob job) {
        log.info("Executing sendOfferToClientWorker");

        Map<String, Object> hashmap = job.getVariablesAsMap();
        hashmap.put("status", "approved");
        log.info("status updated");

        // Update information in the database
        MortgageRequest mortgageRequest = mortgageService.getMortgageRequestById(UUID.fromString(hashmap.get("id").toString()));
        mortgageRequest.setInterestRate(Float.parseFloat(hashmap.get("interestRate").toString()));
        mortgageRequest.setInterestRateExpirationDate(hashmap.get("interestRateExpirationDate").toString());
        mortgageService.addOrUpdateMortgageRequest(mortgageRequest);
        log.info("Database updated with offer data");

        // Add link to hashmap
        hashmap.put("offerUrl", hashmap.get("url") + "/mortgage/approval/" + mortgageRequest.getId());

        // Send email with information of offer
        mailingService.sendMail("mortgage_offer_available_email", hashmap, "Interest Rate Reviced", mortgageRequest.getCustomerEmail());

        client.newCompleteCommand(job.getKey()).send()
                .thenApply(jobResponse -> {
                    log.info("We are job responsing my man!!");
                    log.info("-----------------------------------------------------------------------------------------------");
                    log.info(jobResponse.toString());
                    log.info("-----------------------------------------------------------------------------------------------");
                    return jobResponse;
                })
                .exceptionally(t -> {
                    throw new RuntimeException("Could not complete job: " + t.getMessage(), t);
                });
    }
}
