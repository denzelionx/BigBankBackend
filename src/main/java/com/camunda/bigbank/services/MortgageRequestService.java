package com.camunda.bigbank.services;

import com.camunda.bigbank.entities.MortgageRequest;
import com.camunda.bigbank.repositories.MortgageRequestRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Service class for managing mortgage requests.
 * Provides methods such as adding a new mortgage request and retrieving an existing one.
 */
@Service
public class MortgageRequestService {

    private final MortgageRequestRepository mortgageRequestRepository;

    /**
     * Constructs a new MortgageRequestService with the given repository.
     *
     * @param repository the mortgage request repository.
     */
    public MortgageRequestService(final MortgageRequestRepository repository) {
        this.mortgageRequestRepository = repository;
    }

    /**
     * Adds a new mortgage request to the repository.
     *
     * @param request the mortgage request to add.
     */
    public void addOrUpdateMortgageRequest(final MortgageRequest request) {
        mortgageRequestRepository.save(request);
    }

    public void deleteMortgageRequest(final UUID requestId) {
        mortgageRequestRepository.deleteById(requestId);
    }

    /**
     * Retrieves a mortgage request by its ID.
     * Throws a NoSuchElementException if the mortgage request is not found.
     *
     * @param id the UUID of the mortgage request to retrieve.
     * @return the retrieved mortgage request.
     * @throws NoSuchElementException if no mortgage request is found with the given ID.
     */
    public MortgageRequest getMortgageRequestById(final UUID id) {
        return mortgageRequestRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("MortgageRequest not found with id: " + id));
    }
}
