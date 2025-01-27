package com.camunda.bigbank.worker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.extern.java.Log;


@Component
@Log
public class RetrieveBKRWorker {
    @JobWorker(type = "retrieveBKRFiles", autoComplete = false)
    public void retrieveBKRFiles(final JobClient client, final ActivatedJob job) {
        log.info("Executing RetrieveBKRWorker.");

        final Map<String, Object> map = job.getVariablesAsMap();
        final String customerName = map.get("customerName").toString().replaceAll("\\s", "");

        final String urlString = MessageFormat.format("http://localhost:8089/RequestBKR_{0}", customerName);

        if (map.get("showError") != null && Boolean.TRUE.equals(map.get("showError"))) {
            if(map.get("bkrRatingRetries") == null)
            {
                map.put("bkrRatingRetries", 0);
            }
            simulateUnableToRetrieveFilesError(client, job, map);
            return;
        }

        try {
            URL url = new URI(urlString).toURL(); 
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                log.info("BKR rating recieved from API.");
                String s = response.toString();
                JSONObject jSONObject = new JSONObject(s);
                String rating = jSONObject.getJSONObject("BKR").getString("rating");
                
                map.put("ratingIsPositive", rating);

                log.info("Rating added.");
        
                client.newCompleteCommand(
                    job.getKey())
                    .variables(map)
                    .send();     
            }
            else{
                log.log(Level.WARNING, "Connection with BKR API failed. Response code: {0}", responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Simulates an error for demo purposes.
     *
     * @param client The JobClient.
     * @param job    The ActivatedJob containing job details.
     */
    private void simulateUnableToRetrieveFilesError(final JobClient client, final ActivatedJob job, final Map<String, Object> variables) {
        log.info("Simulating error...");
        client.newThrowErrorCommand(job.getKey())
                .errorCode("UnableToRetrieveFiles")
                .errorMessage("Could not retrieve files.")
                .variables(variables)
                .send()
                .exceptionally(t -> {
                    throw new RuntimeException("Could not throw BPMN error: " + t.getMessage(), t);
                });
    }
}