package com.camunda.bigbank.tests;

// Java importers
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

// JUnit Importers
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

// Our importers 
import com.camunda.bigbank.RequestStatus;
import com.camunda.bigbank.TestSupportService;

// Zeebe Client importers 
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;

// Zeepe Process Test Importers
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import static io.camunda.zeebe.process.test.assertions.BpmnAssert.assertThat;

@ZeebeProcessTest
class InterestRateRequestProcessTest {

    private ZeebeClient client;
    private ZeebeTestEngine engine;

    private final TestSupportService testSupportService = new TestSupportService();
    
    private final String PROCESS_ID = "Process_InterestRateRequest";
    private final String REJECTION_PROCESS_ID = "Process_RejectionEvaluationProcess";
    private final String ADDITIONAL_INFORMATION_PROCESS_ID = "Process_AdditionalInformationRequestProcess";
    
    private final int defaultTimeOut = 1;

    /**
     * The setup function that deploys all requered diagrams for the tests.
     * <p>
     * More diagrams are deployed outside the one we are actually testing. This is because they are call activities within the process we are testing therefore we will need to go through them or end at one of their nodes.
     */
    @BeforeEach
    public void setup() throws IOException{                
        // Deploy Main Process Diagram and Form
        testSupportService.deployAndAssertProcess(
            "processes/interest_rate/interest-rate-request-process.bpmn", 
            PROCESS_ID, 
            true, 
            client);
        testSupportService.deployAndAssertProcess(
            "processes/interest_rate/mortgage-request.form", 
            "",
            false, 
            client);

        // Deploy Decision Process Diagram and DRD Diagram
        testSupportService.deployAndAssertProcess(
            "processes/interest_rate/mortgage-and-interest-rate-decision-process.bpmn", 
            "Process_MortgageInterestDecisionProcess", 
            true, 
            client);
        testSupportService.deployAndAssertProcess(
            "processes/interest_rate/mortgage-and-interest-rate-decision.dmn", 
            "", 
            false, 
            client);

        // Deploy Rejection Process Diagram and Form
        testSupportService.deployAndAssertProcess(
            "processes/common/rejection-evaluation-process.bpmn", 
            REJECTION_PROCESS_ID, 
            true, 
            client);
        testSupportService.deployAndAssertProcess(
            "processes/common/RejectionEvaluation.form", 
            "",
            false, 
            client);

        // Deploy Addition Information Process Diagram
        testSupportService.deployAndAssertProcess(
            "processes/common/additional-infromation-request-process.bpmn", 
            ADDITIONAL_INFORMATION_PROCESS_ID, 
            true, 
            client);
        
    }

    @DisplayName("Deploment of all diagrams Test")
    @Test
    public void givenDiagrams_whenDelpoying_thenDeployed(){
        // This is a empty test that is used to see if the deployment works that is part of setup. 
        // If all other tests fail we know it is not due to deployment.
    }

    private Map<String,Object> createBaseStartingVars(){
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("oldHouseValue",400000);
        vars.put("oldHouseMortgage",120000);
        vars.put("savings",100000);
        vars.put("yearlyIncome",100000); 
        vars.put("vip",false);
        vars.put("requestedAmount",600000);
        vars.put("houseValue",700000);
        vars.put("age",31);
        vars.put("selfEmployment",false);
        vars.put("indefiniteContract",false);
        vars.put("desiredInterestRatePeriod",5);
        vars.put("baseRate",5);
        vars.put("email", "test@test.com");
        vars.put("firstName", "Tjest");
        vars.put("lastName", "Testerson");
        vars.put("phone",  "06123456789");
        vars.put( "address", "testerlaan 69");
        vars.put("id",1);

        return vars;
    }

    @DisplayName("Happy Path Test")
    @Test
    public void givenInterestRateProcess_whenAllIsHappy_thenHappyPathSucceeded() throws Exception{
        // Given all information setup for happy path
        // Mapping starting, paramerets and expected Variables
        Map<String, Object> startingVariables = createBaseStartingVars();

        Map<String,Object> checkInfomResultMap =  Map.of("status",RequestStatus.VERIFIED.getStatus());

        Map<String,Object> expectedCalculationResults = Map.of(
            "soldHouseValue", 280000,
            "personalValue", 480000,
            "mortgageRisk", "Low",
            "inclusiveMortgageRisk", "Medium",
            "interestRate", 4.5454        
        );  

        Map<String, Object> employeeApprovalResultMap = Map.of("decisionInfo", Map.of("decision","Approved"));

        Map<String, Object> experationDateResultMap = Map.of("interestRateExpirationDate",LocalDateTime.now().toString());

        // When starting the process
        ProcessInstanceEvent processInstance = testSupportService.startInstance(PROCESS_ID, startingVariables, client);

        // Then
        // Mock 
        testSupportService.waitForMockTaskToBeCompleted(
            "Task_SendRequestConformation", 
            "sendMail", 
            1, 
            null, 
            processInstance, 
            client, 
            engine, 
            defaultTimeOut);

        // Mock the Check info task
        testSupportService.waitForMockTaskToBeCompleted(
            "Task_CheckProvidedClientInformation", 
            "checkProvidedClientInformationWorker", 
            1, 
            checkInfomResultMap, 
            processInstance, 
            client, 
            engine, 
            defaultTimeOut);

        // Assert that it passed through the call activity to calculate risk and interest rate and check if variables are set
        assertThat(processInstance).hasPassedElement("Task_CalculateInterestRate");
        for(Map.Entry<String, Object> entry : expectedCalculationResults.entrySet()){
            assertThat(processInstance)
                .hasVariableWithValue(entry.getKey(), entry.getValue());
        }

        // Assert that is passed through the gateway for deciding which user tasks it needs to go to
        assertThat(processInstance).hasPassedElement("Gateway_HigherThank750k");
        assertThat(processInstance).hasPassedElement("Flow_LowerThan750K");

        // Mock employee approval task
        testSupportService.waitForMockUserTaskToBeCompleted(
            "Task_ApprovalEmployee", 
            1, 
            employeeApprovalResultMap, 
            processInstance, 
            client, 
            engine, 
            defaultTimeOut);

        // Assert that you passed the gate way
        assertThat(processInstance).hasPassedElement("Gateway_InterestRateApproved");
        assertThat(processInstance).hasPassedElement("Flow_InterestRateApproved");
        
        // Mock set creation Date task
        testSupportService.waitForMockTaskToBeCompleted(
            "Task_SetExperationDate", 
            "setExperationDateWorker", 
            1, 
            experationDateResultMap, 
            processInstance, 
            client, 
            engine, 
            defaultTimeOut);

        // Mock Rest connector
        testSupportService.waitForMockOutboundRestConnectorTaskToBeCompleted(
            "Connector_OfferSendStatus", 
            defaultTimeOut, 
            null, 
            processInstance, 
            client, 
            engine, 
            defaultTimeOut);

    
        // Mock Send Offer To Client
        testSupportService.waitForMockTaskToBeCompleted(
            "Task_SentInterestRateOfferToClient", 
            "sendOfferToClientWorker", 
            1, 
            null, 
            processInstance, 
            client, 
            engine, 
            defaultTimeOut);

        // Check if process has finished
        assertThat(processInstance)
            .isCompleted();
    }

    @DisplayName("Approval Asignee for Senior Check")
    @Test
    public void givenRequestedAmountAboveTreshhold_whenDecidingApprovalEmployee_thenAssigneeIsSenior() throws Exception{
        // Given the mortgage has been calculated 
        Map<String,Object> startingVariables = createBaseStartingVars();
        startingVariables.put("soldHouseValue", 280000);
        startingVariables.put("personalValue", 480000);
        startingVariables.put("mortgageRisk", "Low");
        startingVariables.put("inclusiveMortgageRisk", "Medium");
        startingVariables.put("interestRate", 4.5454);   
        // Change amount to higher than 750K
        startingVariables.put("requestedAmount", 760000); 

        Map<String, Object> employeeApprovalResultMap = Map.of("decisionInfo", Map.of("decision","Approved"));
        
        // When process starts before the gateway dictating which employee approval task is required
        ProcessInstanceEvent processInstance = testSupportService.startInstanceBefore(PROCESS_ID, startingVariables, "Gateway_HigherThank750k", client);

        // Then
        engine.waitForIdleState(Duration.ofSeconds(1));

        // Assert that it passed gateway and the yes flow
        assertThat(processInstance).hasPassedElement("Gateway_HigherThank750k");
        assertThat(processInstance).hasPassedElement("Flow_HigherThan750K");
        
        // Mock employee approval task
        testSupportService.waitForMockUserTaskToBeCompleted(
            "Task_ApprovalSeniorEmployee", 
            1, 
            employeeApprovalResultMap, 
            processInstance, 
            client, 
            engine, 
            defaultTimeOut);

        // Assert that you passed the gate way
        assertThat(processInstance).hasPassedElement("Gateway_InterestRateApproved");
        assertThat(processInstance).hasPassedElement("Flow_InterestRateApproved");

        // When the process is at the set experation date the test is concluded
        assertThat(processInstance).isWaitingAtElements("Task_SetExperationDate");
    }

    @DisplayName("GateWay test")
    @ParameterizedTest( name = "{0}")
    @MethodSource("provideGateWayTestData")
    public void givenDecisionData_whenGoingThroughGateWay_thenCorrectPathDecided(String testName, String startNodeID, Map<String,Object> decisionVariables, String gateWayID, String desiredFlowID) throws Exception{
        testSupportService.testGateWayFlowInSeperateProcess(testName, startNodeID, decisionVariables, gateWayID, desiredFlowID, PROCESS_ID, client, engine);
    }

    private static Stream<Arguments> provideGateWayTestData(){
        return Stream.of(
            Arguments.of(
                "GateWay: Client Information Compleet?, Flow: Client Information Complete",
                "Merger_VerifyRequest",
                Map.of(
                    "status", "verified"
                ),
                "Gateway_ClientInformationCompleet",
                "Flow_ClientInfromationCompleet"),
            Arguments.of(
                "GateWay: Client Information Compleet?, Flow: Client Information Incompleet",
                "Merger_VerifyRequest",
                Map.of(
                    "status", "unverified"
                ),
                "Gateway_ClientInformationCompleet",
                "Flow_ClientInformationIncompleet"),
            Arguments.of(
                "GateWay: Additional Info Profided?, Flow: Yes",
                "Gateway_AdditionalInfoProvided",
                Map.of(
                    "additionalInfoProvided", true
                ),
                "Gateway_AdditionalInfoProvided",
                "Flow_AdditionalInfoProvided"),
            Arguments.of(
                "GateWay: Additional Info Profided?, Flow: No",
                "Gateway_AdditionalInfoProvided",
                Map.of(
                    "additionalInfoProvided", false
                ),
                "Gateway_AdditionalInfoProvided",
                "Flow_AddtionalInfoNotProvided"),
            Arguments.of(
                "GateWay: Additional info Rejection Correct?, Flow: rejection correct",
                "Gateway_WasAdditionalInformationRejectionCorrect",
                Map.of(
                    "rejectionWasCorrect", true
                ),
                "Gateway_WasAdditionalInformationRejectionCorrect",
                "Flow_AdditionalInformationRejectionCorrect"),
            Arguments.of(
                "GateWay: Additional info Rejection Correct?, Flow: rejection incorrect",
                "Gateway_WasAdditionalInformationRejectionCorrect",
                Map.of(
                    "rejectionWasCorrect", false
                ),
                "Gateway_WasAdditionalInformationRejectionCorrect",
                "Flow_AdditionalInformationRejectionIncorrect"),
            Arguments.of(
                "GateWay: Employee Approved?, Flow: approved",
                "Merger_EmployeApproval",
                Map.of(
                    "decisionInfo", Map.of("decision","Approved")
                    ),
                "Gateway_InterestRateApproved",
                "Flow_InterestRateApproved"),
            Arguments.of(
                "GateWay: Employee Approved?, Flow: rejected",
                "Merger_EmployeApproval",
                Map.of(
                    "decisionInfo", Map.of("decision","Nope")
                    ),
                "Gateway_InterestRateApproved",
                "Flow_InterestRateNotApproved"),
            Arguments.of(
                "GateWay: Interest rate Rejection Correct?, Flow: rejection correct",
                "Gateway_WasInterestRejectionCorrect",
                Map.of(
                    "rejectionWasCorrect", true
                ),
                "Gateway_WasInterestRejectionCorrect",
                "Flow_InterestRateRejectionCorrect"),
            Arguments.of(
                "GateWay: Interest rate Rejection Correct?, Flow: rejection incorrect",
                "Gateway_WasInterestRejectionCorrect",
                Map.of(
                    "rejectionWasCorrect", false
                ),
                "Gateway_WasInterestRejectionCorrect",
                "Flow_InterestRateRejectionIncorrect")             
        );
    }


}
