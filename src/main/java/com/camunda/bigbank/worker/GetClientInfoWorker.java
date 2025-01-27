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
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Log
public class GetClientInfoWorker {

    final MortgageRequestService mortgageRequestService;
    final CustomerService customerService;

    public GetClientInfoWorker(final MortgageRequestService mortgageRequestService, final CustomerService customerService) {
        this.mortgageRequestService = mortgageRequestService;
        this.customerService = customerService;
    }

    @JobWorker(type = "getClientInfoWorker", autoComplete = false)
    public void getClientInfoWorker(@VariablesAsType Map<String, Object> variables, final JobClient client, final ActivatedJob job) {
        log.info("Providing client information...");

        MortgageRequest mortgageRequest = mortgageRequestService.getMortgageRequestById(UUID.fromString(variables.get("id").toString()));
        Customer customer = customerService.getCustomerByEmail(mortgageRequest.getCustomerEmail());

        mortgageRequest.setStatus(RequestStatus.VERIFIED.getStatus());
        variables.put("oldHouseValue", mortgageRequest.getHouseValue() - mortgageRequest.getOldHouseMortgage());
        variables.put("interestRatePeriod", mortgageRequest.getDesiredInterestRatePeriod());
        variables.put("yearlyIncome", mortgageRequest.getYearlyIncome());
        variables.put("requestedAmount", mortgageRequest.getRequestedAmount());
        variables.put("interestRate", mortgageRequest.getInterestRate());
        variables.put("houseValue", mortgageRequest.getHouseValue());
        variables.put("clientAge", customer.getAge());
        variables.put("indefiniteContract", mortgageRequest.isIndefiniteContract());
        variables.put("savings", mortgageRequest.getSavings());
        variables.put("status", mortgageRequest.getStatus());
        variables.put("vip", customer.isVip());
        variables.put("selfEmployed", mortgageRequest.isSelfEmployment());
        variables.put("customerName", (customer.getFirstName() + customer.getLastName()).replaceAll("\\s", ""));
        variables.put("address", (customer.getAddress()).replaceAll("\\s", ""));

        client.newCompleteCommand(job)
                .variables(variables)
                .send();
    }
}
