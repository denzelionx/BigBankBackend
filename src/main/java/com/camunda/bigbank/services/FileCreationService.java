package com.camunda.bigbank.services;

import com.lowagie.text.Document;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class FileCreationService {

    private final SpringTemplateEngine templateEngine;
    private final TemporaryService temporaryService;

    /**
     * Constructs a new FileCreationService with the provided SpringTemplateEngine.
     *
     * @param templateEngine   The SpringTemplateEngine to be used for processing templates.
     * @param temporaryService The TempService to be used for  storing the temporary files.
     */
    public FileCreationService(final SpringTemplateEngine templateEngine, final TemporaryService temporaryService) {
        this.templateEngine = templateEngine;
        this.temporaryService = temporaryService;
    }

    /**
     * Generates a unique file name and returns the full path in the temporary directory.
     *
     * @param extension The file extension to be used.
     * @return The full path of the file in the temporary directory.
     */
    public Path getTemporaryFilePath(final String extension) {
        String fileName = getUniqueFileName(extension);
        return temporaryService.getTempDir().resolve(fileName);
    }

    /**
     * Generates a unique file name.
     *
     * @param extension The file extension to be used.
     * @return A unique file name.
     */
    private String getUniqueFileName(final String extension) {
        String dateTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
        return String.format("file_%s_%s.%s", dateTime, UUID.randomUUID(), extension);
    }

    /**
     * Creates HTML content from a given template and data.
     *
     * @param template The name of the template to be processed.
     * @param data     A map containing the variables to be used in the template.
     * @return The processed HTML content as a String.
     * @throws IOException If there's an error processing the template.
     */
    public String createHTML(final String template, final Map<String, Object> data) throws IOException {
        Context context = new Context();
        context.setVariables(data);
        return templateEngine.process(template, context);
    }

    /**
     * Creates an HTML file from a given template and data.
     *
     * @param template The name of the template to be processed.
     * @param data     A map containing the variables to be used in the template.
     * @return The File object representing the created HTML file.
     * @throws IOException If there's an error processing the template or writing the file.
     */
    public File createHTMLFile(final String template, final Map<String, Object> data) throws IOException {
        String htmlString = createHTML(template, data);
        File newHtmlFile = getTemporaryFilePath("html").toFile();
        FileUtils.writeStringToFile(newHtmlFile, htmlString);
        return newHtmlFile;
    }

    /**
     * Creates a PDF file from a given template and data.
     *
     * @param template The name of the template to be processed.
     * @param data     A map containing the variables to be used in the template.
     * @return The File object representing the created PDF file.
     * @throws IOException If there's an error processing the template, converting to PDF, or writing the file.
     */
    public File createPDFFile(final String template, final Map<String, Object> data) throws IOException {
        String htmlString = createHTML(template, data);
        File newPdfFile = getTemporaryFilePath("pdf").toFile();
        try (FileOutputStream outputStream = new FileOutputStream(newPdfFile)) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);

            document.open();
            HTMLWorker htmlWorker = new HTMLWorker(document);
            htmlWorker.parse(new StringReader(htmlString));
            document.close();
        }
        return newPdfFile;
    }
}
