package com.camunda.bigbank.worker;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.camunda.bigbank.RequestStatus;
import com.camunda.bigbank.entities.MortgageRequest;
import com.camunda.bigbank.services.CustomerService;
import com.camunda.bigbank.services.MortgageRequestService;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.extern.java.Log;

@Component
@Log
public class SetExperationDateWorker {

    final MortgageRequestService mortgageRequestService;
    final CustomerService customerService;

    public SetExperationDateWorker(final MortgageRequestService mortgageRequestService, final CustomerService customerService){
        this.mortgageRequestService = mortgageRequestService;
        this.customerService = customerService;
    }

    @JobWorker(type="setExperationDateWorker",autoComplete = false)
    public void setExperationDateWorker(final JobClient client, final ActivatedJob job){
       
        final Map<String, Object> map = job.getVariablesAsMap();

        MortgageRequest mortgageRequest = mortgageRequestService.getMortgageRequestById(UUID.fromString(map.get("id").toString()));
        LocalDateTime time = LocalDateTime.now();
        time = time.plusHours(2);
        map.put("interestRateExpirationDate", time);

        mortgageRequest.setStatus(RequestStatus.VERIFIED.getStatus());
        mortgageRequest.setInterestRateExpirationDate(time.toString());
        mortgageRequestService.addOrUpdateMortgageRequest(mortgageRequest);

        client.newCompleteCommand(
            job.getKey())
            .variables(map)
            .send(); 
    }
}
