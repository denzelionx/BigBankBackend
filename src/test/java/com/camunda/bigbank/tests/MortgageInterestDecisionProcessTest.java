package com.camunda.bigbank.tests;

import java.util.Map;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.camunda.bigbank.TestSupportService;
import com.camunda.bigbank.test_data.InterestRateTestData;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import static io.camunda.zeebe.process.test.assertions.BpmnAssert.assertThat;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;

@ZeebeProcessTest
public class MortgageInterestDecisionProcessTest {

    private ZeebeClient client;
    private final TestSupportService testSupportService = new TestSupportService();
    private final String PROCESS_ID = "Process_MortgageInterestDecisionProcess";

    @BeforeEach
    public void setup() throws IOException{
               // Deploy Decision Process Diagram and DRD Diagram
               testSupportService.deployAndAssertProcess(
                "processes/interest_rate/mortgage-and-interest-rate-decision-process.bpmn", 
                PROCESS_ID, 
                true, 
                client);
            testSupportService.deployAndAssertProcess(
                "processes/interest_rate/mortgage-and-interest-rate-decision.dmn", 
                "", 
                false, 
                client);
    }

    @DisplayName("Deploment of both diagrams Test")
    @Test
    public void givenDiagrams_whenDelpoying_thenDeployed(){
        // This is a empty test that is used to see if the deployment works that is part of setup. 
        // If all other tests fail we know it is not due to deployment.
    }

    @DisplayName("Mortgage and Interest Rate DMN diagrams and process test")
    @ParameterizedTest( name = "{0}")
    @MethodSource("com.camunda.bigbank.test_data.InterestRateTestDataFormatter#provideInterestRateTestData")
    public void givenInterestRateTestData_whenGoingThroughAllDiagrams_thenExpectedDiagramRowsAreTriggered(String name, InterestRateTestData data){
        //Given
        Map<String, Object> startingVariables = data.startingVariablesToMap();
        Map<String, Object> expectedResultsVariables = data.expectedResultsToMap();

        //When
         ProcessInstanceEvent processInstance = testSupportService.startInstance(PROCESS_ID, startingVariables, client);
        
        //Then
        // Only assert if the soldHouseValue is correct if we expect a value > 0
        if(expectedResultsVariables.get("soldHouseValue") != null && (int)expectedResultsVariables.get("soldHouseValue" ) > 0){
            assertThat(processInstance)
            .hasVariableWithValue("soldHouseValue", expectedResultsVariables.get("soldHouseValue"));
        }

        // Assert if personal values and risks are correct
        assertThat(processInstance)
            .hasVariableWithValue("personalValue", expectedResultsVariables.get("personalValue"))
            .hasVariableWithValue("mortgageRisk", expectedResultsVariables.get("mortgageRisk"))
            .hasVariableWithValue("inclusiveMortgageRisk", expectedResultsVariables.get("inclusiveMortgageRisk"));

        //check if the expected interest rate is a whole number (4 or 5)
        if((double)expectedResultsVariables.get("interestRate") % 1 ==0){
            // if it is, cast the double(primitive) to a Double(java.lang.double) which can be converted to an intValue(). 
            // Assert cannot compare double 4.0 with int 4 therefore we need to convert to int. 
            // double(primitive) cannot be cast to int but Double(java.lang.double) can. 
            Double interestRate = (double)expectedResultsVariables.get("interestRate");
            assertThat(processInstance).hasVariableWithValue("interestRate", interestRate.intValue());
        }else{
            // if it is not, then we can assert do its own magic
            assertThat(processInstance).hasVariableWithValue("interestRate", expectedResultsVariables.get("interestRate"));
        }

        assertThat(processInstance).isCompleted();
    }
}
