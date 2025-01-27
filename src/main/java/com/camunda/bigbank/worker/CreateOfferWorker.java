package com.camunda.bigbank.worker;

import com.camunda.bigbank.entities.Customer;
import com.camunda.bigbank.entities.MortgageRequest;
import com.camunda.bigbank.services.CustomerService;
import com.camunda.bigbank.services.FileCreationService;
import com.camunda.bigbank.services.MortgageRequestService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.VariablesAsType;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Log
public class CreateOfferWorker {

    final MortgageRequestService mortgageRequestService;
    final CustomerService customerService;
    final private FileCreationService fileCreationService;

    public CreateOfferWorker(FileCreationService fileCreationService, final MortgageRequestService mortgageRequestService, final CustomerService customerService) {
        this.fileCreationService = fileCreationService;
        this.mortgageRequestService = mortgageRequestService;
        this.customerService = customerService;
    }

    /**
     * Creates a mortgage offer PDF and sets its path in process variables.
     *
     * @param variables Process variables, must include "id" of the mortgage request.
     * @param client    Camunda job client.
     * @param job       Activated Camunda job.
     */
    @JobWorker(autoComplete = false)
    public void createOffer(@VariablesAsType Map<String, Object> variables, final JobClient client, final ActivatedJob job) {
        log.info("Executing CreateOfferWorker.");

        Map<String, Object> data = new HashMap<>();
        MortgageRequest mortgageRequest = mortgageRequestService.getMortgageRequestById(UUID.fromString(variables.get("id").toString()));
        Customer customer = customerService.getCustomerByEmail(mortgageRequest.getCustomerEmail());

        data.putAll(mortgageRequest.toMap());
        data.putAll(customer.toMap());
        data.putAll(variables);

        try {
            File offerPdf = fileCreationService.createPDFFile("mortgage_Offer", data);
            data.put("offerPdfPath", offerPdf.getAbsolutePath());

            client.newCompleteCommand(job)
                    .variables(data)
                    .send()
                    .join();

        } catch (IOException e) {
            throw new RuntimeException("Could not create offer: " + e.getMessage());
        }
    }
}
