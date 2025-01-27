package com.camunda.bigbank.worker;


import com.camunda.bigbank.RequestStatus;
import com.camunda.bigbank.entities.Customer;
import com.camunda.bigbank.entities.MortgageRequest;
import com.camunda.bigbank.services.CustomerService;
import com.camunda.bigbank.services.MortgageRequestService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.VariablesAsType;
import lombok.extern.java.Log;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Log
public class CheckProvidedClientInformationWorker {

    private final MortgageRequestService mortgageService;
    private final CustomerService customerService;

    /**
     * Constructs a new CheckProvidedClientInformationWorker with the specified services.
     *
     * @param mortgageService the service for managing mortgage requests.
     * @param customerService the service for managing customers.
     */
    public CheckProvidedClientInformationWorker(final MortgageRequestService mortgageService, final CustomerService customerService) {
        this.mortgageService = mortgageService;
        this.customerService = customerService;
    }

    /**
     * Checks the provided information and updates the mortgage request status and feedback accordingly.
     *
     * @param variables A map containing the client and request data.
     * @param client    The JobClient for completing the job.
     * @param job       The ActivatedJob containing job details.
     */
    @JobWorker(autoComplete = false)
    public void checkProvidedClientInformationWorker(@VariablesAsType Map<String, Object> variables, final JobClient client, final ActivatedJob job) {
        log.info("Checking provided client information...");

        if (variables.get("shouldVerificationError") != null && Boolean.TRUE.equals(variables.get("shouldVerificationError"))) {
            simulateError(client, job, variables);
            return;
        }

        MortgageRequest request = new MortgageRequest(variables);
        Customer customer = customerService.getCustomerByEmail(request.getCustomerEmail());

        List<String> messages = checkCustomerData(customer.toMap());
        if (messages.isEmpty()) {
            request.setStatus(RequestStatus.VERIFIED.getStatus());
        } else {
            request.setStatus(RequestStatus.CHANGES_REQUESTED.getStatus());
            request.setFeedback(Strings.concat("Missing information: ", StringUtils.join(messages, ", ")));
        }

        mortgageService.addOrUpdateMortgageRequest(request);

        variables.putAll(customer.toMap());
        variables.putAll(request.toMap());
        client.newCompleteCommand(job.getKey())
                .variables(variables)
                .send()
                .join();
    }

    /**
     * Simulates an error for demo purposes.
     *
     * @param client The JobClient.
     * @param job    The ActivatedJob containing job details.
     */
    private void simulateError(final JobClient client, final ActivatedJob job, final Map<String, Object> variables) {
        log.info("Simulating error...");
        client.newThrowErrorCommand(job.getKey())
                .errorCode("verification-error")
                .errorMessage("Could not evaluate data.")
                .variables(variables)
                .send()
                .exceptionally(t -> {
                    throw new RuntimeException("Could not throw BPMN error: " + t.getMessage(), t);
                });
    }

    /**
     * Checks the customer data for required fields.
     *
     * @param data A map containing the customer data.
     * @return A list of missing required fields, or an empty list if all fields are present.
     */
    private List<String> checkCustomerData(final Map<String, Object> data) {
        String[] requiredFields = new String[]{"email", "firstName", "lastName", "phone", "address", "age"};

        if (data == null || data.isEmpty()) {
            return Arrays.stream(requiredFields).toList();
        }

        List<String> messages = new ArrayList<>();
        for (String field : requiredFields) {
            if (data.get(field) == null || StringUtils.isEmpty(data.get(field).toString())) {
                messages.add(field);
            }
        }

        return messages;
    }
}
