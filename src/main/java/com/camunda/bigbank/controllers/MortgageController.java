package com.camunda.bigbank.controllers;

import com.camunda.bigbank.Constants;
import com.camunda.bigbank.RequestStatus;
import com.camunda.bigbank.entities.MortgageRequest;
import com.camunda.bigbank.services.MessageService;
import com.camunda.bigbank.services.MortgageRequestService;
import com.camunda.bigbank.services.ProcessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import static com.camunda.bigbank.Constants.MESSAGE_ADDITIONAL_INFO_RECEIVED;

/**
 * REST controller for handling mortgage requests.
 * Provides endpoints to start mortgage processes and retrieve mortgage request details.
 */
@RestController
@RequestMapping("/mortgage")
public class MortgageController {

    private final MortgageRequestService mortgageService;
    private final ProcessService processService;
    private final MessageService messageService;

    /**
     * Constructs a new MortgageController with the specified services.
     *
     * @param mortgageService the service for managing mortgage requests
     * @param processService  the service for managing process instances
     */
    public MortgageController(final MortgageRequestService mortgageService, final ProcessService processService, final MessageService messageService) {
        this.mortgageService = mortgageService;
        this.processService = processService;
        this.messageService = messageService;
    }

    /**
     * Adds a new mortgage request to the repository and initiates the 'mortgage request' process.
     *
     * @param variables a map containing the variables for the request.
     * @return a response entity with the UUID of the newly created request.
     */
    @PostMapping("/request")
    public ResponseEntity<String> createMortgageRequest(@RequestBody Map<String, Object> variables) {
        try {
            MortgageRequest mortgageRequest = new MortgageRequest(variables);
            variables.putAll(mortgageRequest.toMap());

            mortgageService.addOrUpdateMortgageRequest(mortgageRequest);
            processService.startProcess(Constants.PROCESS_ID_MORTGAGE_REQUEST, variables);

            return ResponseEntity.status(HttpStatus.CREATED).body(mortgageRequest.getId().toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create and start mortgage process: " + e.getMessage());
        }
    }

    /**
     * Updates existing mortgage request data.
     *
     * @param variables a map containing the variables for the request.
     * @return a response entity with the UUID of the updated request.
     */
    @PutMapping("/request")
    public ResponseEntity<String> updateMortgageRequest(@RequestBody Map<String, Object> variables) {
        try {
            UUID id = UUID.fromString(variables.get("id").toString());
            MortgageRequest request = mortgageService.getMortgageRequestById(id);
            if (request == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mortgage request with not found");
            }

            request.updateFromMap(variables);
            mortgageService.addOrUpdateMortgageRequest(request);

            // Both the customer and employee can update a request, however, we only want to respond to new information if it comes from the customer.
            if (!("employee".equals(variables.get("updateBy"))) && (RequestStatus.CHANGES_REQUESTED.getStatus().equals(request.getStatus()))) {
                Map<String, Object> messageVariables = Map.of("additionalInfoProvided", true);
                messageService.sendMessage(MESSAGE_ADDITIONAL_INFO_RECEIVED, request.getId().toString(), messageVariables);
            }

            return ResponseEntity.status(HttpStatus.OK).body(request.getId().toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update mortgage process: " + e.getMessage());
        }
    }

    /**
     * deletes existing mortgage request data.
     *
     * @param id the ID for the mortgage to be deleted
     * @return a response entity with the message about the result of the deletion.
     */
    @DeleteMapping("/request/{id}")
    public ResponseEntity<String> deleteMortgageRequest(@PathVariable("id") UUID id) {
        try {
            mortgageService.deleteMortgageRequest(id);
            return ResponseEntity.status(HttpStatus.OK).body("Mortgage has been successfully deleted");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update mortgage process: " + e.getMessage());
        }
    }

    /**
     * Retrieves a mortgage request by its ID.
     *
     * @param id the UUID of the mortgage request to retrieve
     * @return a response entity containing the mortgage request details as a map.
     */
    @GetMapping("/request/{id}")
    public ResponseEntity<Map<String, Object>> getMortgageRequest(@PathVariable("id") UUID id) {
        try {
            MortgageRequest mortgageRequest = mortgageService.getMortgageRequestById(id);
            if (mortgageRequest != null) {
                return ResponseEntity.ok(mortgageRequest.toMap());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
