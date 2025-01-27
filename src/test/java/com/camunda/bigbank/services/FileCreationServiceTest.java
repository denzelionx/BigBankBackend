package com.camunda.bigbank.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FileCreationServiceTest {

    private FileCreationService fileCreationService;
    private TemporaryService temporaryService;

    @BeforeEach
    public void setUp() throws IOException {
        // Setup template engine
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateEngine.setTemplateResolver(templateResolver);

        // Initialization
        temporaryService = new TemporaryService();
        fileCreationService = new FileCreationService(templateEngine, temporaryService);
    }

    @AfterEach
    public void tearDown() throws IOException {
        temporaryService.clean();
    }

    @Test
    @DisplayName("Given the 'mortgage offer' template, when creating HTML, the result should be an HTML file containing the expected elements.")
    public void shouldCreateOfferInHtml() throws IOException {
        // Given
        String templateName = "mortgage_Offer";
        Map<String, Object> data = createDummyClient();

        // When
        File result = fileCreationService.createHTMLFile(templateName, data);

        // Then
        assertTrue(result.isFile(), "Result should be a file");
        String content = new String(Files.readAllBytes(result.toPath()));
        assertTrue(content.contains("John Doe"));
        assertTrue(content.contains("250000"));
        assertTrue(content.contains("3.5"));
        assertTrue(content.contains("360"));
        assertTrue(content.contains("26"));
        assertTrue(content.contains("0699228857"));
        assertTrue(content.contains("Some address 420"));
        assertTrue(content.contains("info@johndoe.nl"));
    }

    @Test
    @DisplayName("Given the 'mortgage offer' template, when creating a PDF, the result should be an PDF file.")
    public void shouldCreateOfferInPdf() throws IOException {
        // Given
        String templateName = "mortgage_Offer";
        Map<String, Object> data = createDummyClient();

        // When
        File result = fileCreationService.createPDFFile(templateName, data);

        // Then
        assertTrue(result.isFile(), "Result should be a file");
        assertTrue(result.length() > 0, "PDF file should not be empty");
    }

    /**
     * @return a dummy client for testing purposes.
     */
    public Map<String, Object> createDummyClient() {
        Map<String, Object> data = new HashMap<>();
        data.put("firstName", "John");
        data.put("lastName", "Doe");
        data.put("offerAmount", "250000");
        data.put("interestRate", "3.5");
        data.put("interestRatePeriod", "360");
        data.put("age", "26");
        data.put("phone", "0699228857");
        data.put("address", "Some address 420");
        data.put("email", "info@johndoe.nl");
        return data;
    }
}