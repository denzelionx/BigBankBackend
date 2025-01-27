package com.camunda.bigbank.tests;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.camunda.bigbank.RequestStatus;
import com.camunda.bigbank.TestSupportService;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import static io.camunda.zeebe.process.test.assertions.BpmnAssert.assertThat;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;

@ZeebeProcessTest
public class OfferCreationProcessTest {

    private ZeebeClient client;
    private ZeebeTestEngine engine;
    private final TestSupportService testSupportService = new TestSupportService();

    private final String PROCESS_ID = "Process_OfferCreation";
    private final String REJECTION_PROCESS_ID = "Process_RejectionEvaluationProcess";
    private final String ADDITIONAL_INFORMATION_PROCESS_ID = "Process_AdditionalInformationRequestProcess";

    private final int defaultTimeOut = 1;

     /**
     * The setup function that deploys all requered diagrams for the tests.
     * More diagrams are deployed outside the one we are actually testing. This is because they are call activities within the process we are testing therefore we will need to go through them or end at one of their nodes.
     * @throws IOException 
     */
    @BeforeEach
    public void setup() throws IOException{   
        //Deploy offer creation process   
        testSupportService.deployAndAssertProcess(
            "processes/offer/offer-creation-process.bpmn", 
            PROCESS_ID, 
            true, 
            client);
        testSupportService.deployAndAssertProcess(
            "processes/offer/mortgage-offer-approval-form.form", 
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

    /**
     * This is a empty test that is used to see if the deployment part of the setup works. 
     */
    @DisplayName("Deploment of all diagrams Test")
    @Test
    public void givenDiagrams_whenDelpoying_thenDeployed(){
    }

    /**
     * Test the Happy path of the offer creation process diagram.
     * @throws Exception
     */
    @DisplayName("Test Happy Path")
    @Test public void givenOfferCreationProcess_whenAllIsHappy_thenHappyPathSucceeded() throws Exception{

        Map<String, Object> startingVariables = CreateStartingVars();

        // Map used for the getClientData activity
        Map<String,Object> checkInfomResultMap =  Map.of(
            "status",RequestStatus.VERIFIED.getStatus()
        );

        // Map used to assert the result of the determine amount script task
        Map<String,Object> expectedCalculationResults = Map.of(
            "offerAmount", 1500000      
        );

        // Map used for the employeeApproval
        Map<String, Object> employeeApprovalResultMap = Map.of(
            "OfferApproved", "approved"
        );

         // Start process
        ProcessInstanceEvent processInstance = testSupportService.startInstance(PROCESS_ID, startingVariables, client);

        // Mock the Check info task
        testSupportService.waitForMockTaskToBeCompleted(
            "Activity_GetClientData", 
            "getClientInfoWorker", 
            1,
            checkInfomResultMap,
            processInstance,
            client,
            engine, 
            defaultTimeOut);

        // Assert that the test went past the getClientData activity
        assertThat(processInstance).hasPassedElement("Activity_GetClientData");
        assertThat(processInstance).hasPassedElement("Gateway_DataComplete");
        assertThat(processInstance).hasPassedElement("Flow_DataComplete");
        assertThat(processInstance).hasPassedElement("Gateway_ParallelDataComplete");

        //Mock GetOwnerFromLandRegistry
        testSupportService.waitForMockTaskToBeCompleted(
            "Activity_GetOwnerName", 
            "io.camunda:http-json:1", 
            1, 
            startingVariables, 
            processInstance, 
            client, 
            engine, 
            defaultTimeOut);

        // Assert that the test went past the getOwner rest connector            
        assertThat(processInstance).hasPassedElement("Flow_PassedGetOwner");
        assertThat(processInstance).hasPassedElement("Gateway_RetrieveBKR");

        // Mock retrieveBKRFiles activity
        testSupportService.waitForMockTaskToBeCompleted(
            "Activity_RetrieveBKR", 
            "retrieveBKRFiles", 
            1, 
            startingVariables, 
            processInstance, 
            client, 
            engine, 
            defaultTimeOut);
            
        // Assert that the test went past the retrieveBKR activity            
        assertThat(processInstance).hasPassedElement("Flow_PassedRetrieveBKR");
        
        // Assert that the test went past the DetermineAmount script task
        assertThat(processInstance).hasPassedElement("Activity_DetermineAmount");
        assertThat(processInstance).hasPassedElement("Flow_PassedDetermineAmount");

        for(Map.Entry<String, Object> entry : expectedCalculationResults.entrySet()){
            assertThat(processInstance)
                .hasVariableWithValue(entry.getKey(), entry.getValue());
        }

        // Mock Decide on mortgage approval activity
        testSupportService.waitForMockUserTaskToBeCompleted(
            "Activity_DecisionByJunior",
            1, 
            employeeApprovalResultMap, 
            processInstance, 
            client, 
            engine, 
            defaultTimeOut);

        // Assert that the test went past the decision task
        assertThat(processInstance).hasPassedElement("Activity_DecisionByJunior");
        assertThat(processInstance).hasPassedElement("Flow_afterDecision");
        assertThat(processInstance).hasPassedElement("Gateway_Approval");
        assertThat(processInstance).hasPassedElement("Flow_Approved");

        // Mock create offer activity
        testSupportService.waitForMockTaskToBeCompleted(
            "Activity_CreateOffer", 
            "createOffer", 
            1, 
            startingVariables,
            processInstance,
            client,
            engine, 
            defaultTimeOut);

    
        // Assert that the test went past create offer
        assertThat(processInstance).hasPassedElement("Activity_CreateOffer");
        assertThat(processInstance).hasPassedElement("Flow_afterCreateOffer");


        // Mock send offer to client activity
        testSupportService.waitForMockTaskToBeCompleted(
            "Message_SendOfferToClient", 
            "sendMail", 
            1, 
            startingVariables,
            processInstance,
            client,
            engine, 
            defaultTimeOut);
        
        // Assert that process passed the Send to client offer activity
        assertThat(processInstance).hasPassedElement("Message_SendOfferToClient");
        assertThat(processInstance).hasPassedElement("Flow_AfterSendOffer");

        // Assert that the process has finished
        assertThat(processInstance)
            .hasPassedElement("Event_SendToNotary")
            .isCompleted();
    }

    /**
     * Test for the junior flow of the approval gateway.
     * @throws Exception
     */
    @DisplayName("Approval from Junior")
    @Test
    public void givenRequestedAmountBelowTreshhold_whenDecidingApprovalLevel_thenAssignJunior() throws Exception{
        Map<String,Object> startingVariables = CreateStartingVars();
        startingVariables.put("oldHouseValue",10);
        startingVariables.put("savings",10);
        startingVariables.put("yearlyIncome",10); 

        // Map used for the employeeApproval
        Map<String, Object> employeeApprovalResultMap = Map.of(
            "OfferApproved", "approved"
        );
        
        // When process starts before the gateway dictating which employee approval task is required
        ProcessInstanceEvent processInstance = testSupportService.startInstanceBefore(PROCESS_ID, startingVariables, "Activity_DetermineAmount", client);

        // Then
        engine.waitForIdleState(Duration.ofSeconds(1));

        // Assert that it passed gateway and the less then 750k flow
        assertThat(processInstance).hasPassedElement("Gateway_Over750K");
        assertThat(processInstance).hasPassedElement("Flow_LessThen750K");
        
        // Mock Decide on mortgage approval activity
        testSupportService.waitForMockUserTaskToBeCompleted(
            "Activity_DecisionByJunior",
            1, 
            employeeApprovalResultMap, 
            processInstance, 
            client, 
            engine, 
            defaultTimeOut);

        // Assert that it passed gateway and the less then 750k flow
        assertThat(processInstance).hasPassedElement("Gateway_Over750K");
        assertThat(processInstance).hasPassedElement("Flow_LessThen750K");

       // When the process is waiting at the Create offer worker the test is completed
       assertThat(processInstance).isWaitingAtElements("Activity_CreateOffer");
    }

    /**
     * Test all the gateways in the offer-creation-process.bpmn diagram.
     * @param testName The name you want to give the test.
     * @param startNodeID The Id of the node you want to start at.
     * @param decisionVariables Variables used in the gateway.
     * @param gateWayID The Id of the gateway you want to test.
     * @param desiredFlowID The Id of the flow that should be followed.
     * @throws Exception
     */
    @DisplayName("Offer Creation Process GateWay test")
    @ParameterizedTest( name = "{0}")
    @MethodSource("provideGateWayTestData")
    public void givenDecisionData_whenGoingThroughGateWay_thenCorrectPathDecided(String testName, String startNodeID, Map<String,Object> decisionVariables, String gateWayID, String desiredFlowID) throws Exception{
        testSupportService.testGateWayFlowInSeperateProcess(testName, startNodeID, decisionVariables, gateWayID, desiredFlowID, PROCESS_ID, client, engine);
    }

    /**
     * Provides data for the {@link givenDecisionData_whenGoingThroughGateWay_thenCorrectPathDecided} test method
     * @return A stream with test data
     */
    private static Stream<Arguments> provideGateWayTestData(){
        return Stream.of(
            Arguments.of(
                "GateWay: Client Information Compleet?, Flow: Client Information Complete",
                "Gateway_DataComplete",
                Map.of(
                    "status", "verified"
                ),
                "Gateway_DataComplete",
                "Flow_DataComplete"),
            Arguments.of(
                "GateWay: Client Information Compleet?, Flow: Client Information Not Complete",
                "Gateway_DataComplete",
                Map.of(
                    "status", "unverified"
                ),
                "Gateway_DataComplete",
                "Flow_DataNotComplete"),
             Arguments.of(
                "GateWay: Offer Approved?, Flow: Offer Approved",
                "Gateway_AfterDecision",
                Map.of(
                    "OfferApproved", "approved"
                ),
                "Gateway_Approval",
                "Flow_Approved"),
            Arguments.of(
                "GateWay: Offer Approved?, Flow: Offer Declined",
                "Gateway_AfterDecision",
                Map.of(
                    "OfferApproved", "rejected"
                ),
                "Gateway_Approval",
                "Flow_Declined"),     
            Arguments.of(
                "GateWay: RejectionUnfounded?, Flow: Rejection was unfounded",
                "Gateway_RejectionUnfounded",
                Map.of(
                    "OfferApproved", true
                ),
                "Gateway_RejectionUnfounded",
                "Flow_RejectionUnfounded"),     
            Arguments.of(
                "GateWay: RejectionUnfounded?, Flow: Rejection was founded",
                "Gateway_RejectionUnfounded",
                Map.of(
                    "OfferApproved", false
                ),
                "Gateway_RejectionUnfounded",
                "Flow_RejectionFounded"),     
            Arguments.of(
                "GateWay: Additional Info Provided?, Flow: Info not provided",
                "Gateway_AdditionalInfo",
                Map.of(
                    "additionalInfoProvided", false
                ),
                "Gateway_AdditionalInfo",
                "Flow_AdditionalInfoNotProvided"),     
            Arguments.of(
                "GateWay: Additional Info Provided?, Flow: Info provided",
                "Gateway_AdditionalInfo",
                Map.of(
                    "additionalInfoProvided", true
                ),
                "Gateway_AdditionalInfo",
                "Flow_AdditionalInfoProvided"),     
            Arguments.of(
                "GateWay: Was Rejection Correct after Additional Info Provided?, Flow: Rejection Correct",
                "Gateway_RejectionAfterInfoNotProvided",
                Map.of(
                    "RejectionWasCorrect", true
                ),
                "Gateway_RejectionAfterInfoNotProvided",
                "Flow_RejectionCorrect"),     
            Arguments.of(
                "GateWay: Was Rejection Correct after Additional Info Provided?, Flow: Rejection Correct",
                "Gateway_RejectionAfterInfoNotProvided",
                Map.of(
                    "RejectionWasCorrect", false
                ),
                "Gateway_RejectionAfterInfoNotProvided",
                "Flow_RejectionIncorrect")
        );
    }

    /**
     * Creates the variables needed to start the {@link givenOfferCreationProcess_whenAllIsHappy_thenHappyPathSucceeded} test.
     * @return A Map<String, Object> with needed variables 
     */
    private Map<String, Object> CreateStartingVars(){
        //Map Variables
        Map<String, Object> startingVariables = new HashMap<String, Object>();
        startingVariables.put("oldHouseValue",400000);
        startingVariables.put("savings",100000);
        startingVariables.put("yearlyIncome",100000); 
        startingVariables.put("vip",false);
        startingVariables.put("requestedAmount",600000);
        startingVariables.put("houseValue",700000);
        startingVariables.put("age",69);
        startingVariables.put("selfEmployed",false);
        startingVariables.put("indefiniteContract",false);
        startingVariables.put("interestRatePeriod",5);
        startingVariables.put("email", "test@test.com");
        startingVariables.put("customerName", "TjestTesterson");
        startingVariables.put("firstName", "Tjest");
        startingVariables.put("lastName", "Testerson");
        startingVariables.put( "address", "wegisweg");    
        startingVariables.put("phone",  "06123456789");
        startingVariables.put("email", "test@test.com");
        startingVariables.put( "interestRate", 4.5454);  
        return startingVariables;  
    }
}