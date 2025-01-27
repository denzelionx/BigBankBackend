package com.camunda.bigbank.services;

import io.camunda.zeebe.client.ZeebeClient;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for sending messages.
 */
@Service
@Log
public class MessageService {

    private final ZeebeClient zeebeClient;

    /**
     * Creates a MessageService.
     *
     * @param zeebeClient The client used to communicate with.
     */
    public MessageService(final ZeebeClient zeebeClient) {
        this.zeebeClient = zeebeClient;
    }

    /**
     * This method publishes a message to the engine.
     *
     * @param name          The name of the message as defined in the BPMN model.
     * @param correlationId The correlation key used to target specific process instances, and should match the correlation key defined in the process.
     * @param variables     A map of variables to be passed along with the message.
     * @throws RuntimeException if there's an error sending the message
     */
    public void sendMessage(final String name, final String correlationId, final Map<String, Object> variables) {
        zeebeClient.newPublishMessageCommand()
                .messageName(name)
                .correlationKey(correlationId)
                .variables(variables)
                .send()
                .join();
    }
}
