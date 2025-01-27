package com.camunda.bigbank;

import lombok.Getter;

@Getter
public enum RequestStatus {
    PENDING("pending"),
    VERIFIED("verified"),
    APPROVED("approved"),
    REJECTED("rejected"),
    OFFER_SENT("offer-sent"),
    CHANGES_REQUESTED("changes-requested"),
    SUPPORT_REQUIRED("support-required");

    private final String status;

    RequestStatus(final String status) {
        this.status = status;
    }
}