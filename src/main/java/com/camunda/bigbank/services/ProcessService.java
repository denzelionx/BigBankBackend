package com.camunda.bigbank.services;

import io.camunda.zeebe.client.ZeebeClient;
import lombok.extern.java.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Service
@Log
public class ProcessService {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessService.class);

    private final ZeebeClient zeebeClient;

    public ProcessService(final ZeebeClient zeebeClient) {
        this.zeebeClient = zeebeClient;
    }

    public <T> void startProcess(final String processId, final T variables) throws IOException {
        LOG.info("Starting process `{}` ", processId);
        log.info("Starting process `{}` " + processId);

        zeebeClient
                .newCreateInstanceCommand()
                .bpmnProcessId(processId)
                .latestVersion()
                .variables(variables)
                .send()
                .join(); // Wait for the result
    }

    public Properties loadProperties(String fileName) throws IOException {
        InputStream input = new FileInputStream(fileName);
        Properties prop = new Properties();
        prop.load(input);
        return prop;
    }
}
