package com.camunda.bigbank;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import static io.camunda.zeebe.process.test.assertions.BpmnAssert.assertThat;

public class TestSupportService {
    /**
     * Helper function to deploy a Camunda document and assert if it has been deployed
     * @param resourcePath - The direct path to the resource in a resource folder
     * @param processId - The ID of the process (only needed for BPMN Diagrams)
     * @param isBPMNFile - Wether it is a BPMN Diagram
     * @param client - The used ZeebeClient
     * @throws IOException - Expcetion when it couldnt be deployed
     */
    public void deployAndAssertProcess(final String resourcePath, final String processId, final Boolean isBPMNFile, final ZeebeClient client) throws IOException {
        // Deploy specified file
        DeploymentEvent deploymentEvent = client.newDeployResourceCommand()
                .addResourceFromClasspath(resourcePath)
                .send()
                .join();
        
        // If it is a BPMN we can check if there is a succesful deployment event and if the process ID is present
        if(isBPMNFile) assertThat(deploymentEvent).containsProcessesByBpmnProcessId(processId);
        // If it is not a BPMN we can only check if there is a succesful deployment event       
        else  assertThat(deploymentEvent);
    }

    
    /**
     * Helper function to start process instance
     * <p>
     * This helper function starts a process instance correctly for you and then asserts if it started. 
     * @param id - the ID of the Bpnm diagram
     * @param variables - the start variables the diagram needs
     * @param client - the Zeebe client you want this to run on
     * @return <b>ProcessInstanceEvent</b> - the specific process that was started 
     */
    public ProcessInstanceEvent startInstance(String id, Map<String,Object> variables, ZeebeClient client ){
        ProcessInstanceEvent processInstance = client.newCreateInstanceCommand()
            .bpmnProcessId(id)
            .latestVersion()
            .variables(variables)
            .send()
            .join();
        
        BpmnAssert.assertThat(processInstance).isStarted();

        return processInstance;
    }

    /**
     * Helper function to start process instance at specific step
     * <p>
     * This helper function starts a specific process instance on the step specified by the variabels then asserts if it started.
     * @param id - the ID of the Bpmn diagram
     * @param variables - the variables that the diagram should have at this point in time
     * @param startPoint - the ID of the specific step you want to start at (note it will run this step as it starts it right before calling this step)
     * @param client - the Zeebe client you want this to run on
     * @return <b>ProcessInstanceEvent</b> - the specific process that was started 
     */
    public ProcessInstanceEvent startInstanceBefore(String id, Map<String, Object> variables, String startPoint, ZeebeClient client){
        ProcessInstanceEvent processInstance = client.newCreateInstanceCommand()
            .bpmnProcessId(id)
            .latestVersion()
            .variables(variables)
            .startBeforeElement(startPoint)
            .send()
            .join();

        BpmnAssert.assertThat(processInstance).isStarted();

        return processInstance;
    }

    /**
     * Helper function to call a specific jobworker
     * <p>
     * This helper function will start a specified job worker to run band wait for a second to run that worker.
     * @param type - the job type of the worker
     * @param count - the max amount of jobs it will activate 
     * @param handler - the specified job handler 
     * @param client - the Zeebe client you want this to run on
     * @param engine - the Zeebe test engine you are using
     * @throws Exception - Any expceptions thrown by handler or engine.wait. 
     */
    public void completeJob(String type, int count, JobHandler handler, ZeebeClient client, ZeebeTestEngine engine) throws Exception{
        ActivateJobsResponse activatedJobsResponse = client.newActivateJobsCommand()
            .jobType(type)
            .maxJobsToActivate(count)
            .send()
            .join();

        List<ActivatedJob> activatedJobs = activatedJobsResponse.getJobs();
        
        if(activatedJobs.size() != count){
            fail("No job activated for type " + type);
        }

        for (ActivatedJob job:activatedJobs) {
            handler.handle(client, job);
        }
    
        engine.waitForIdleState(Duration.ofSeconds(1));
    }

    
    /**
     * Will wait for the engine to be at an idle state, then finish the specified job.
     * <p>
     * It will fake the job it is wating for completing it with the specified variabels. It will also assert if the engine has passed said job and if the variables have been set.
     * <p>
     * This function is used to test if the process and a jobworker can correcly communicate with eachother. The jobworker should be tested in a seperate test.
     * @param workerID - ID of the task (in bpnm the ID)
     * @param taskType - The type of the task (in bpmn Task Definition -> Type)
     * @param amountOfJobsToActivate - the Maximum amount of jobs of this time we would like to complete.
     * @param variables - The variables that the jobworker would normally set.
     * @param processInstance - the specific process instance (used for asserting)
     * @param client - the ZeebeClient used by your test
     * @param engine - The ZeebeTestEngine used by your test
     * @param timeOut - The amount of time, in seconds, we will wait for the engine to become idle before throwing an exeption
     * @throws Exception - Any exeption that can be thrown by assesment and waiting on idle states
     */
    public void waitForMockTaskToBeCompleted(String workerID, String taskType, int amountOfJobsToActivate, Map<String, Object> variables, ProcessInstanceEvent processInstance, ZeebeClient client, ZeebeTestEngine engine, int timeOut) throws Exception{
        engine.waitForIdleState(Duration.ofSeconds(timeOut));
        assertThat(processInstance).isWaitingAtElements(workerID);

        ActivateJobsResponse activateJobsResponse = client.newActivateJobsCommand()
        .jobType(taskType)
        .maxJobsToActivate(amountOfJobsToActivate)
        .send().join();

        completeActivatedJobs(
            activateJobsResponse,
            amountOfJobsToActivate,
            taskType,
            workerID,
            variables,
            processInstance,
            client,
            engine,
            timeOut
        );
    }

    /**
     * Will wait for the engine to be at an idle state, then finish the specified User Task.
     * <p>
     * It will fake the job it is waiting for completing it with the specified variabels. It will also assert if the engine has passed said job and if the variables have been set.
     * <p>
     * This function is used to test if the process and a jobworker can correcly communicate with eachother. The jobworker should be tested in a seperate test.
     * @param workerID - ID of the task (in bpnm the ID)
     * @param amountOfJobsToActivate - the Maximum amount of jobs of this time we would like to complete.
     * @param variables - The variables that the jobworker would normally set.
     * @param processInstance - the specific process instance (used for asserting)
     * @param client - the ZeebeClient used by your test
     * @param engine - The ZeebeTestEngine used by your test
     * @param timeOut - The amount of time, in seconds, we will wait for the engine to become idle before throwing an exeption
     * @throws Exception - Any exeption that can be thrown by assesment and waiting on idle states
     */
    public void waitForMockUserTaskToBeCompleted(String workerID, int amountOfJobsToActivate, Map<String, Object> variables, ProcessInstanceEvent processInstance, ZeebeClient client, ZeebeTestEngine engine, int timeOut) throws Exception{
        engine.waitForIdleState(Duration.ofSeconds(timeOut));
        assertThat(processInstance).isWaitingAtElements(workerID);

        ActivateJobsResponse activateJobsResponse = client.newActivateJobsCommand()
        .jobType("io.camunda.zeebe:userTask")
        .maxJobsToActivate(amountOfJobsToActivate)
        .send().join();

        completeActivatedJobs(
            activateJobsResponse,
            amountOfJobsToActivate,
            "io.camunda.zeebe:userTask",
            workerID,
            variables,
            processInstance,
            client,
            engine,
            timeOut
        );
    }

    public void waitForMockOutboundRestConnectorTaskToBeCompleted(String workerID, int amountOfJobsToActivate, Map<String, Object> variables, ProcessInstanceEvent processInstance, ZeebeClient client, ZeebeTestEngine engine, int timeOut) throws Exception{
        engine.waitForIdleState(Duration.ofSeconds(timeOut));
        assertThat(processInstance).isWaitingAtElements(workerID);

        ActivateJobsResponse activateJobsResponse = client.newActivateJobsCommand()
        .jobType("io.camunda:http-json:1")
        .maxJobsToActivate(amountOfJobsToActivate)
        .send().join();

        completeActivatedJobs(
            activateJobsResponse,
            amountOfJobsToActivate,
            "io.camunda:http-json:1",
            workerID,
            variables,
            processInstance,
            client,
            engine,
            timeOut
        );
    }


    private void completeActivatedJobs(
        ActivateJobsResponse activateJobsResponse,
        int amountOfJobsToActivate,
        String taskType,
        String workerID,
        Map<String, Object> variables,
        ProcessInstanceEvent processInstance,
        ZeebeClient client,
        ZeebeTestEngine engine,
        int timeOut
    ) throws Exception{
        List<ActivatedJob> activatedJobs = activateJobsResponse.getJobs();
        if(activatedJobs.size() != amountOfJobsToActivate){
            fail("Different amount of Jobs than Expected from type " + taskType + "\n Expect amount is ["+ amountOfJobsToActivate + "] \n Actual amount is [" +activatedJobs.size() +  "]");
        }

        for (ActivatedJob job:activatedJobs) {
            assertEquals(workerID, job.getElementId());
            if(variables != null && variables.size() >0){
                client.newCompleteCommand(job)
                    .variables(variables)
                    .send()
                    .join();
            }else{
                client.newCompleteCommand(job)
                .send()
                .join();
            }
        }
        
        engine.waitForIdleState(Duration.ofSeconds(timeOut));

        assertThat(processInstance)
            .hasPassedElement(workerID);
            
        if(variables != null){
            for(Map.Entry<String, Object> entry : variables.entrySet()){
                assertThat(processInstance)
                    .hasVariableWithValue(entry.getKey(), entry.getValue());
            }
        }
    }


    public void testGateWayFlowInSeperateProcess(String testName, String startNodeID, Map<String,Object> decisionVariables, String gateWayID, String desiredFlowID, String processID, ZeebeClient client, ZeebeTestEngine engine) throws Exception{
        //When
        ProcessInstanceEvent processInstance = startInstanceBefore(processID, decisionVariables, startNodeID, client);

        //Then
        engine.waitForIdleState(Duration.ofSeconds(1));

        // Assert that it passed the GateWau and the specified flow
        assertThat(processInstance).hasPassedElement(gateWayID);
        assertThat(processInstance).hasPassedElement(desiredFlowID);
    }
}
