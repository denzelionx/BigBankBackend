package com.camunda.bigbank.entities;

import lombok.Getter;

@Getter
public class EmailConfig {
    private final String template;
    private final String subject;
    private final String recipient;

    public EmailConfig(String template, String subject) {
        this(template, subject, null);
    }

    public EmailConfig(String template, String subject, String recipient) {
        this.template = template;
        this.subject = subject;
        this.recipient = recipient;
    }
}
