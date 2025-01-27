package com.camunda.bigbank.services;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.javadoc.Javadoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Service class for generating AsciiDoc documentation from Java source files.
 */
@Service
public class DocsService {

    private static final Logger LOG = LoggerFactory.getLogger(DocsService.class);
    private static final String BACKEND_SOURCE_PATH = "backend/src/main/java";

    private final Path sourceDir;
    private final FileService fileService;
    private final StringBuilder documentation;

    /**
     * Constructs a new DocsService with the specified file service.
     */
    public DocsService(final FileService fileService) {
        this.fileService = fileService;
        this.documentation = new StringBuilder();

        Path projectRoot = fileService.findProjectRoot();
        this.sourceDir = projectRoot.resolve(BACKEND_SOURCE_PATH);
    }

    /**
     * Generates AsciiDoc documentation for all Java files in the specified source directory.
     *
     * @return The generated documentation as a string
     * @throws IOException If there's an error reading files
     */
    public String generateDocs() throws IOException {
        if (!Files.exists(sourceDir)) {
            LOG.error("The provided source directory does not exist: {}", sourceDir);
            return "";
        }

        initializeDocument();
        final List<File> javaFiles = fileService.getFiles(sourceDir, ".java");
        processJavaFiles(javaFiles);
        finalizeDocument();
        return documentation.toString();
    }

    /**
     * Initializes the document with headers and introductory content.
     */
    private void initializeDocument() {
        documentation.append("= Big Bank Backend Documentation\n")
                .append(":doctype: book\n")
                .append(":toc: left\n")
                .append(":toclevels: 3\n")
                .append(":sectnums:\n")
                .append(":experimental:\n\n")
                .append("== Introduction\n\n")
                .append("[.lead]\n")
                .append("This document provides a comprehensive overview of the Big Bank backend application, ")
                .append("including its main components, services, and controllers.\n\n")
                .append(generateApplicationStructure());
    }

    /**
     * Generates the application structure section.
     *
     * @return The generated application structure as a string
     */
    private String generateApplicationStructure() {
        return "== Application Structure\n\n" +
                "The application is structured into the following main components:\n\n" +
                "[horizontal]\n" +
                "Controllers:: Handle incoming HTTP requests\n" +
                "Services:: Implement business logic\n" +
                "Repositories:: Manage data persistence\n" +
                "Entities:: Represents types of data\n" +
                "Workers:: Execute tasks\n\n";
    }

    /**
     * Processes all Java files and generates documentation for each.
     *
     * @param javaFiles List of Java files to process
     */
    private void processJavaFiles(final List<File> javaFiles) {
        documentation.append("== Components\n\n");
        for (final File file : javaFiles) {
            try {
                final CompilationUnit cu = StaticJavaParser.parse(file);
                final String className = file.getName().replace(".java", "");
                processClass(cu, className);
            } catch (IOException e) {
                LOG.error("Error processing file: {}", file.getName(), e);
            }
        }
    }

    /**
     * Processes a single class file based on its type.
     *
     * @param cu        The CompilationUnit representing the class
     * @param className The name of the class
     */
    private void processClass(final CompilationUnit cu, final String className) {
        if (className.endsWith("Controller")) {
            processController(cu, className);
        } else if (className.endsWith("Service")) {
            processService(cu, className);
        } else if (className.endsWith("Worker")) {
            processWorker(cu, className);
        } else {
            processOtherClass(cu, className);
        }
    }

    /**
     * Processes a controller class and generates its documentation.
     *
     * @param cu        The CompilationUnit representing the controller class
     * @param className The name of the controller class
     */
    private void processController(final CompilationUnit cu, final String className) {
        documentation.append("=== ").append(className).append("\n\n");
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(this::processControllerClass);
    }

    /**
     * Processes a single controller class declaration.
     *
     * @param classDeclaration The ClassOrInterfaceDeclaration of the controller
     */
    private void processControllerClass(final ClassOrInterfaceDeclaration classDeclaration) {
        classDeclaration.getJavadoc().ifPresent(javadoc ->
                documentation.append("[.lead]\n").append(createDoc(javadoc)).append("\n\n")
        );
        documentation.append(".Endpoints\n")
                .append("[%collapsible]\n")
                .append("====\n");
        processControllerMethods(classDeclaration);
        documentation.append("====\n\n");
    }

    /**
     * Processes a service class and generates its documentation.
     *
     * @param cu        The CompilationUnit representing the service class
     * @param className The name of the service class
     */
    private void processService(final CompilationUnit cu, final String className) {
        documentation.append("=== ").append(className).append("\n\n");
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(this::processServiceClass);
    }

    /**
     * Processes a single service class declaration.
     *
     * @param classDeclaration The ClassOrInterfaceDeclaration of the service
     */
    private void processServiceClass(final ClassOrInterfaceDeclaration classDeclaration) {
        classDeclaration.getJavadoc().ifPresent(javadoc ->
                documentation.append("[.lead]\n").append(createDoc(javadoc)).append("\n\n")
        );
        documentation.append(".Key Methods\n")
                .append("[%collapsible]\n")
                .append("====\n");
        processMethods(classDeclaration);
        documentation.append("====\n\n");
    }

    /**
     * Processes a worker class and generates its documentation.
     *
     * @param cu        The CompilationUnit representing the worker class
     * @param className The name of the worker class
     */
    private void processWorker(final CompilationUnit cu, final String className) {
        documentation.append("=== ").append(className).append("\n\n");
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(this::processWorkerClass);
    }

    /**
     * Processes a single worker class declaration.
     *
     * @param classDeclaration The ClassOrInterfaceDeclaration of the worker
     */
    private void processWorkerClass(final ClassOrInterfaceDeclaration classDeclaration) {
        classDeclaration.getJavadoc().ifPresent(javadoc ->
                documentation.append("[.lead]\n").append(createDoc(javadoc)).append("\n\n")
        );
        documentation.append(".Worker Methods\n")
                .append("[%collapsible]\n")
                .append("====\n");
        processMethods(classDeclaration);
        documentation.append("====\n\n");
    }

    /**
     * Processes any other type of class and generates its documentation.
     *
     * @param cu        The CompilationUnit representing the class
     * @param className The name of the class
     */
    private void processOtherClass(final CompilationUnit cu, final String className) {
        documentation.append("=== ").append(className).append("\n\n");
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(this::processOtherClassDeclaration);
    }

    /**
     * Processes a single class declaration for other types of classes.
     *
     * @param classDeclaration The ClassOrInterfaceDeclaration of the class
     */
    private void processOtherClassDeclaration(final ClassOrInterfaceDeclaration classDeclaration) {
        classDeclaration.getJavadoc().ifPresent(javadoc ->
                documentation.append(createDoc(javadoc)).append("\n\n")
        );
        documentation.append(".Methods\n")
                .append("[%collapsible]\n")
                .append("====\n");
        processMethods(classDeclaration);
        documentation.append("====\n\n");
    }

    /**
     * Processes the methods of a controller class and generates documentation for each endpoint.
     *
     * @param classDeclaration The ClassOrInterfaceDeclaration representing the controller class
     */
    private void processControllerMethods(final ClassOrInterfaceDeclaration classDeclaration) {
        for (final MethodDeclaration methodDeclaration : classDeclaration.getMethods()) {
            methodDeclaration.getAnnotations().stream()
                    .filter(a -> a.getNameAsString().endsWith("Mapping"))
                    .findFirst()
                    .ifPresent(annotation -> {
                        final String httpMethod = getHttpMethod(annotation);
                        final String path = getPath(annotation);

                        documentation.append("* `").append(httpMethod).append(" ").append(path).append("`");
                        methodDeclaration.getJavadoc().ifPresent(javadoc ->
                                documentation.append(": ").append(createDoc(javadoc))
                        );
                        documentation.append("\n");
                    });
        }
    }

    /**
     * Extracts the HTTP method from a mapping annotation.
     *
     * @param annotation The mapping annotation
     * @return The HTTP method as a string
     */
    private String getHttpMethod(final AnnotationExpr annotation) {
        String httpMethod = annotation.getNameAsString().replace("Mapping", "").toUpperCase();
        return httpMethod.isEmpty() ? "GET" : httpMethod; // Default to GET if just @RequestMapping is used
    }

    /**
     * Extracts the path from a mapping annotation.
     *
     * @param annotation The mapping annotation
     * @return The path as a string
     */
    private String getPath(final AnnotationExpr annotation) {
        return annotation.getChildNodes().stream()
                .filter(node -> node.toString().startsWith("value="))
                .map(node -> node.toString().replace("value=", "").replace("\"", ""))
                .findFirst()
                .orElse("");
    }

    /**
     * Processes the methods of a class and generates documentation for each.
     *
     * @param classDeclaration The ClassOrInterfaceDeclaration representing the class
     */
    private void processMethods(final ClassOrInterfaceDeclaration classDeclaration) {
        for (final MethodDeclaration methodDeclaration : classDeclaration.getMethods()) {
            final String methodName = methodDeclaration.getNameAsString();
            documentation.append("* `").append(methodName).append("(");
            documentation.append(getMethodParameters(methodDeclaration));
            documentation.append(")`");
            methodDeclaration.getJavadoc().ifPresent(javadoc ->
                    documentation.append(": ").append(createDoc(javadoc))
            );
            documentation.append("\n");
        }
    }

    /**
     * Gets the parameters of a method as a string.
     *
     * @param methodDeclaration The MethodDeclaration to get parameters from
     * @return A string representation of the method parameters
     */
    private String getMethodParameters(final MethodDeclaration methodDeclaration) {
        return methodDeclaration.getParameters().stream()
                .map(p -> p.getType().asString() + " " + p.getNameAsString())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    /**
     * Converts a Javadoc comment to AsciiDoc format.
     *
     * @param javadoc The Javadoc comment to convert
     * @return The AsciiDoc representation of the Javadoc comment
     */
    private String createDoc(final Javadoc javadoc) {
        return javadoc.getDescription().toText()
                .replace("@param", "Parameters:")
                .replace("@return", "Returns:")
                .replace("@throws", "Throws:");
    }

    /**
     * Finalizes the document by adding a conclusion section.
     */
    private void finalizeDocument() {
        documentation.append("== Conclusion\n\n")
                .append("[.lead]\n")
                .append("This documentation provides an overview of the Big Bank backend application structure and components. ")
                .append("For more detailed information, please refer to the inline code comments and Javadocs in the source code.\n");
    }
}