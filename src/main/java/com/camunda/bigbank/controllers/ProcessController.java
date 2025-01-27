package com.camunda.bigbank.controllers;

import com.camunda.bigbank.Constants;
import com.camunda.bigbank.services.ProcessService;
import lombok.extern.java.Log;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.camunda.bigbank.entities.Customer;
import com.camunda.bigbank.entities.MortgageRequest;
import com.camunda.bigbank.services.CustomerService;
import com.camunda.bigbank.services.MortgageRequestService;
import com.camunda.bigbank.wireMock.WireMockStubs;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for handling process requests.
 * Provides endpoints to start new process.
 */
@Log
@RestController
@RequestMapping("/process")
public class ProcessController {

    private final ProcessService processService;
    private final WireMockStubs wireMockSubs;  
    private final MortgageRequestService mortgageRequestService;
    private final CustomerService customerService;

    public ProcessController(final ProcessService processService, final WireMockStubs wireMockSubs,final MortgageRequestService mortgageRequestService, 
    final CustomerService customerService) {
        this.processService = processService;
        this.wireMockSubs = wireMockSubs;
        this.mortgageRequestService = mortgageRequestService;
        this.customerService = customerService;
    }

    @PostMapping("/start")
    public void startProcess(@RequestBody Object mortgageId) {
        try {
            Map<String, Object> sortedMap = new HashMap<String, Object>();
            sortedMap.put("status", "pending");
            sortedMap.put("id", UUID.fromString(mortgageId.toString()));

            //TODO: Might want to more add parameters here
           
            processService.startProcess(Constants.PROCESS_ID_OFFER_CREATION, sortedMap);

            MortgageRequest mortgageRequest = mortgageRequestService.getMortgageRequestById(UUID.fromString(mortgageId.toString()));
            Customer customer = customerService.getCustomerByEmail(mortgageRequest.getCustomerEmail());
            wireMockSubs.registerOfferStubs(customer);
            
        } catch (Exception e) {

            log.info("message exception" + e.getMessage());
        }
    }
}
