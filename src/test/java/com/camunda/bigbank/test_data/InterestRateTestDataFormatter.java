package com.camunda.bigbank.test_data;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.junit.jupiter.params.provider.Arguments;

public class InterestRateTestDataFormatter{

    private final static String TEST_FILES_FOLDER_NAME = "interest_rate_datasets";

    /**
     * This is a specfic function to get data sets from resources to be used in test. This is called by @ParameterizedTest@MethodSource("com.camunda.bigbank.test_data.InterestRateTestDataFormatter#provideInterestRateTestData")
     * That cause visual studio code to throw a waring for the function being unused (which is not true) therefore we supprese that waring
     * For Junit it needs to be a static function and Junit will run regardless of it being private but we make it private so we are not tempted to use this on runtime.
     * @return - an array list of data classes used for testing
     */
    @SuppressWarnings("unused")
    private static ArrayList<Arguments> provideInterestRateTestData(){
        ArrayList<Arguments> testDatas = new ArrayList<Arguments>();

        // We are getting the folder path and feading that into Files.Walk that allows us to get all files in folder.
        try(Stream<Path> paths = Files.walk(Paths.get(InterestRateTestDataFormatter.class.getClassLoader().getResource(TEST_FILES_FOLDER_NAME).toURI()))){
            // Filter all paths in the stream to only have actual files (java considers folders files) that and with .json and put those as files in list.
            List<File> files = paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json")).map(Path::toFile).toList();

            // Read through the files and convert them to data sets.
            for(File file : files){
                InputStream pathItemStream = Files.newInputStream(file.toPath());
                InterestRateTestData data = getDataFromStream(pathItemStream);
                Map<String, Object> expectedResults = data.expectedResultsToMap();
                String testName =  
                            "Expected Mortgage Risk: " + expectedResults.get("mortgageRisk") + 
                            " || Expected Inclusive Mortgage Risk: " + expectedResults.get("inclusiveMortgageRisk") +
                            " || Expected InterestRate: " + expectedResults.get("interestRate") +
                            " || File: "+ file.getName() ;
                testDatas.add(Arguments.of(testName, data));
            }             
        }catch (Exception e) {
            throw new IllegalArgumentException("Error:" + e.getMessage());
        }

        return testDatas;
    }

    /**
     * Reads through an inputstream and convert that via Json to InterestRateTestData;
     * @param inputStream
     * @return
     * @throws Exception
     */
    private static InterestRateTestData getDataFromStream(InputStream inputStream) throws Exception{
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String jsonText = readAll(reader);
        JSONObject json = new JSONObject(jsonText);
 
        InterestRateTestData testData = new InterestRateTestData(json);
        return testData;
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        int cp;
        while ((cp = reader.read()) != -1) {
          builder.append((char) cp);
        }
        return builder.toString();
      }
}