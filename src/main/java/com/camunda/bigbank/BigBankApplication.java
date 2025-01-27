package com.camunda.bigbank;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.camunda.zeebe.spring.client.annotation.Deployment;
import lombok.extern.java.Log;

@SpringBootApplication
@Log
@Deployment(resources = {"classpath*:**/*.bpmn", "classpath*:**/*.dmn", "classpath*:**/*.form"})
public class BigBankApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(BigBankApplication.class, args);
    }

    public void run(final String... args) {
        log.info("Start process instance...");
    }
}