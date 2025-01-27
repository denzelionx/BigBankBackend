package com.camunda.bigbank;

import com.camunda.bigbank.services.DocsService;
import com.camunda.bigbank.services.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class DocsGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(DocsGenerator.class);
    private static final String OUTPUT_FILE = "backend.adoc";

    private static final FileService fileService = new FileService();
    private static final DocsService docsService = new DocsService(fileService);

    /**
     * Main method to initiate the documentation generation process.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(final String[] args) {
        try {
            String asciiDocs = docsService.generateDocs();
            writeDocumentToFile(asciiDocs);
        } catch (IOException e) {
            LOG.error("Error creating documentation: ", e);
        }
    }

    /**
     * Writes the generated documentation content to a file in the project root directory.
     *
     * @param content The documentation content to write
     * @throws IOException If an error occurs during file operations
     */
    private static void writeDocumentToFile(final String content) throws IOException {
        Path projectRoot = fileService.findProjectRoot();
        File output = projectRoot.resolve(OUTPUT_FILE).toFile();

        try (FileWriter writer = new FileWriter(output)) {
            writer.write(content);
        }

        LOG.info("Generated AsciiDoc documentation: {}", output.getAbsolutePath());
    }
}