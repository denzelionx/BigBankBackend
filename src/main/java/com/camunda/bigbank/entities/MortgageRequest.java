package com.camunda.bigbank.entities;

import com.camunda.bigbank.RequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a mortgage request with applicant details.
 * This entity is used to persist mortgage request data in the database.
 */
@Entity
@Table(name = "mortgage_requests")
@Data
public class MortgageRequest {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    private String status = RequestStatus.PENDING.getStatus();
    private String customerEmail;

    private float oldHouseValue = 0.0f;
    private float oldHouseMortgage = 0.0f;
    private float savings = 0.0f;
    private float yearlyIncome = 0.0f;
    private float requestedAmount = 0.0f;
    private float houseValue = 0.0f;
    private boolean selfEmployment = false;
    private boolean indefiniteContract = false;
    private int desiredInterestRatePeriod = 0;
    private float baseRate = 0.0f;
    private String feedback;

    // Mortgage offer variables
    private float interestRate = 0.0f;
    private String interestRateExpirationDate;

    // Files as Base64 strings
    private String copyOfId;


    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    /**
     * Default constructor.
     */
    public MortgageRequest() {
        this.id = UUID.randomUUID();
    }

    /**
     * Constructs a new MortgageRequest from the provided map.
     * The map is expected to contain keys corresponding to the fields of this class.
     *
     * @param requestMap A map containing the initial values for the object's fields.
     */
    public MortgageRequest(final Map<String, Object> requestMap) {
        this.id = requestMap.get("id") != null ? UUID.fromString(requestMap.get("id").toString()) : UUID.randomUUID();
        updateFromMap(requestMap);
    }

    /**
     * Converts this MortgageRequest instance to a map where the keys are field names
     * and the values are the corresponding field values.
     *
     * @return a map containing the field names and their corresponding values
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", this.id.toString());
        map.put("status", this.status);
        map.put("customerEmail", this.customerEmail);
        map.put("oldHouseValue", this.oldHouseValue);
        map.put("oldHouseMortgage", this.oldHouseMortgage);
        map.put("savings", this.savings);
        map.put("yearlyIncome", this.yearlyIncome);
        map.put("requestedAmount", this.requestedAmount);
        map.put("houseValue", this.houseValue);
        map.put("selfEmployment", this.selfEmployment);
        map.put("indefiniteContract", this.indefiniteContract);
        map.put("desiredInterestRatePeriod", this.desiredInterestRatePeriod);
        map.put("baseRate", this.baseRate);
        map.put("feedback", this.feedback);
        map.put("copyOfId", this.copyOfId);
        map.put("interestRate", this.interestRate);
        map.put("interestRateExpirationDate", this.interestRateExpirationDate);
        return map;
    }

    /**
     * Updates the current MortgageRequest instance with values from the provided map.
     * This method allows for partial updates, only modifying fields that are present in the map.
     * The 'id' field is not updatable through this method.
     *
     * @param updates A map containing the fields to update and their new values.
     *                The keys should correspond to the field names of this class.
     *                It is assumed that the values in the map are of the correct type for each field.
     */
    public void updateFromMap(final Map<String, Object> updates) {
        updates.computeIfPresent("status", (k, v) ->
                this.status = (Strings.isBlank((String) v) ? RequestStatus.PENDING.getStatus() : v.toString()));
        updates.computeIfPresent("customerEmail", (k, v) -> this.customerEmail = v.toString());
        updates.computeIfPresent("oldHouseValue", (k, v) -> this.oldHouseValue = Float.parseFloat(v.toString()));
        updates.computeIfPresent("oldHouseMortgage", (k, v) -> this.oldHouseMortgage = Float.parseFloat(v.toString()));
        updates.computeIfPresent("savings", (k, v) -> this.savings = Float.parseFloat(v.toString()));
        updates.computeIfPresent("yearlyIncome", (k, v) -> this.yearlyIncome = Float.parseFloat(v.toString()));
        updates.computeIfPresent("requestedAmount", (k, v) -> this.requestedAmount = Float.parseFloat(v.toString()));
        updates.computeIfPresent("houseValue", (k, v) -> this.houseValue = Float.parseFloat(v.toString()));
        updates.computeIfPresent("selfEmployment", (k, v) -> this.selfEmployment = Boolean.parseBoolean(v.toString()));
        updates.computeIfPresent("indefiniteContract", (k, v) -> this.indefiniteContract = Boolean.parseBoolean(v.toString()));
        updates.computeIfPresent("desiredInterestRatePeriod", (k, v) -> this.desiredInterestRatePeriod = Integer.parseInt(v.toString()));
        updates.computeIfPresent("baseRate", (k, v) -> this.baseRate = Float.parseFloat(v.toString()));
        updates.computeIfPresent("feedback", (k, v) -> this.feedback = v.toString());
        updates.computeIfPresent("copyOfId", (k, v) -> this.copyOfId = v.toString());
        updates.computeIfPresent("interestRate", (k, v) -> this.interestRate = Float.parseFloat(v.toString()));
        updates.computeIfPresent("interestRateExpirationDate", (k, v) -> this.interestRateExpirationDate = v.toString());
    }
}
