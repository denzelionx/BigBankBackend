package com.camunda.bigbank.repositories;

import com.camunda.bigbank.entities.MortgageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MortgageRequestRepository extends JpaRepository<MortgageRequest, UUID> {

}