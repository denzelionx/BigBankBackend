package com.camunda.bigbank.services;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Helps with files & directories.
 */
@Service
public class FileService {

    /**
     * Locates the project root directory by finding the parent directory that contains
     * both 'frontend' and 'backend' directories.
     *
     * @return The project root path
     */
    public Path findProjectRoot() {
        Path path = Paths.get("");
        Path currentPath = path.toAbsolutePath();

        while (currentPath.getParent() != null) {
            Path parent = currentPath.getParent();
            Path frontendPath = parent.resolve("frontend");
            Path backendPath = parent.resolve("backend");

            if (frontendPath.toFile().isDirectory() && backendPath.toFile().isDirectory()) {
                return parent;
            }
            currentPath = parent;
        }
        
        return path.toAbsolutePath();
    }

    /**
     * Retrieves all files of a certain extension from the specified source directory.
     *
     * @param sourceDir The directory to search for files
     * @param extension The extension to filter on
     * @return A list of Java files
     * @throws IOException If there's an error reading the directory
     */
    public List<File> getFiles(final Path sourceDir, final String extension) throws IOException {
        try (Stream<Path> walk = Files.walk(sourceDir)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(extension))
                    .map(Path::toFile)
                    .toList();
        }
    }
}
