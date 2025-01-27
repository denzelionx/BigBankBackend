package com.camunda.bigbank.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * This entity is used to persist a customer in the database.
 */
@Entity
@Table(name = "customers")
@Data
public class Customer {

    @Id
    @Column(unique = true, updatable = false, nullable = false)
    private String email;

    private String firstName;
    private String lastName;
    private int age;
    private String phone;
    private String address;
    private boolean vip;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    /**
     * Default constructor
     */
    public Customer() {
    }

    /**
     * Constructs a new Customer from the provided map.
     * The map is expected to contain keys corresponding to the fields of this class.
     *
     * @param map A map containing the initial values for the object's fields.
     */
    public Customer(final Map<String, Object> map) {
        this.email = map.get("email") != null ? map.get("email").toString() : null;
        this.firstName = map.get("firstName") != null ? map.get("firstName").toString() : null;
        this.lastName = map.get("lastName") != null ? map.get("lastName").toString() : null;
        this.age = map.get("age") != null ? (int) map.get("age") : 0;
        this.phone = map.get("phone") != null ? map.get("phone").toString() : null;
        this.address = map.get("address") != null ? map.get("address").toString() : null;
        this.vip = map.get("vip") != null && (boolean) map.get("vip");
    }

    /**
     * Converts this Customer instance to a map where the keys are field names
     * and the values are the corresponding field values.
     *
     * @return a map containing the field names and their corresponding values
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("email", this.email);
        map.put("firstName", this.firstName);
        map.put("lastName", this.lastName);
        map.put("age", this.age);
        map.put("phone", this.phone);
        map.put("address", this.address);
        map.put("vip", this.vip);
        return map;
    }
}
