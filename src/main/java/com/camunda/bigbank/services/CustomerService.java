package com.camunda.bigbank.services;

import com.camunda.bigbank.entities.Customer;
import com.camunda.bigbank.repositories.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * Service class for managing customer records.
 * Provides methods such as adding a new customer and retrieving an existing one.
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * Constructs a new CustomerService with the given repository.
     *
     * @param repository the customer repository.
     */
    public CustomerService(final CustomerRepository repository) {
        this.customerRepository = repository;
    }

    /**
     * Adds a new customer to the repository.
     *
     * @param customer the customer to add.
     */
    public void addOrUpdateCustomer(final Customer customer) {
        customerRepository.save(customer);
    }

    /**
     * Retrieves a customer by their email.
     * Throws a NoSuchElementException if the customer is not found.
     *
     * @param email the email of the customer to retrieve.
     * @return the retrieved customer.
     * @throws NoSuchElementException if no customer is found with the given email.
     */
    public Customer getCustomerByEmail(final String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("Customer not found with email: " + email));
    }
}
