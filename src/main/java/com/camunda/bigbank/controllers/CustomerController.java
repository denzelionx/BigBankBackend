package com.camunda.bigbank.controllers;


import com.camunda.bigbank.entities.Customer;
import com.camunda.bigbank.services.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for handling customer requests.
 * Provides endpoints to add new customers and retrieve customer details.
 */
@RestController
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Constructs a new CustomerController with the specified services.
     *
     * @param customerService the service for managing customers
     */
    public CustomerController(final CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Adds a new customer to the repository.
     *
     * @param customerMap a map containing the customer details
     * @return a response entity with the email of the newly created customer
     */
    @PostMapping("/add")
    public ResponseEntity<String> addCustomer(@RequestBody Map<String, Object> customerMap) {
        try {
            Customer customer = new Customer(customerMap);
            customerService.addOrUpdateCustomer(customer);

            return ResponseEntity.status(HttpStatus.CREATED).body(customer.getEmail());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add customer: " + e.getMessage());
        }
    }

    /**
     * Retrieves a customer by their email.
     *
     * @param email the email of the customer to retrieve
     * @return a response entity containing the customer details as a map
     */
    @GetMapping("/{email}")
    public ResponseEntity<Map<String, Object>> getCustomer(@PathVariable("email") String email) {
        try {
            Customer customer = customerService.getCustomerByEmail(email);
            if (customer != null) {
                return ResponseEntity.ok(customer.toMap());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
