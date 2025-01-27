package com.camunda.bigbank.services;

import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This component is responsible for cleaning up a temporary directory upon application startup.
 */
@Getter
@Component
@Service
public class TemporaryService {

    private static final Logger log = LoggerFactory.getLogger(TemporaryService.class);

    /**
     * The name of the temporary directory to be created and cleaned.
     */
    private static final String TEMP_DIR_NAME = "bigbank-temp";

    private Path tempDir;

    /**
     * Upon instantiating, a temporary directory is created, or cleaned if present already.
     *
     * @throws IOException if an I/O error occurs during the cleanup process.
     */
    public TemporaryService() throws IOException {
        initializeAndCleanupTempDirectory();
    }

    /**
     * Initializes and cleans up the temporary directory.
     * This method is triggered automatically when the Spring Boot application is fully started and ready.
     *
     * @throws IOException if an I/O error occurs during the cleanup process.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeAndCleanupTempDirectory() throws IOException {
        initializeTempDirectory();
        clean();
    }

    /**
     * Initializes the temporary directory.
     *
     * @throws IOException if an I/O error occurs during directory creation.
     */
    private void initializeTempDirectory() throws IOException {
        tempDir = Files.createTempDirectory(TEMP_DIR_NAME);
        log.info("Temporary directory created: {}", tempDir);
    }

    /**
     * Cleans up the temporary directory by deleting all its contents.
     *
     * @throws IOException if an I/O error occurs during the cleanup process.
     */
    public void clean() throws IOException {
        if (tempDir != null) {
            log.info("Cleaning up temporary directory: {}", tempDir);
            FileUtils.cleanDirectory(tempDir.toFile());
        } else {
            log.warn("Temporary directory not initialized. Skipping cleanup.");
        }
    }
}
