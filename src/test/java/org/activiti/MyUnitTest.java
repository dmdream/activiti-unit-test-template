package org.activiti;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.persistence.entity.VariableInstanceEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.ActivitiRule;
import org.activiti.engine.test.Deployment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyUnitTest {
    Log Log = LogFactory.getFactory()
                        .getInstance(MyUnitTest.class);

    @Rule
    public ActivitiRule activitiRule = new ActivitiRule();

    @Test
    @Deployment(resources = {"org/activiti/test/my-process.bpmn20.xml"})
    public void test() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        repositoryService.createDeployment()
                         .addClasspathResource("org/activiti/test/VacationRequest.bpmn20.xml")
                         .deploy();

        Log.info("Number of process definitions: " + repositoryService.createProcessDefinitionQuery()
                                                                      .count());

        // 4.3.2. Starting a process instance
        Map<String, Object> variables = new HashMap<>();
        variables.put("employeeName", "Kermit");
        variables.put("numberOfDays", new Integer(4));
        variables.put("vacationMotivation", "I'm really tired!");

        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("vacationRequest", variables);

        // Verify that we started a new process instance
        Log.info("Number of process instances: " + runtimeService.createProcessInstanceQuery()
                                                                 .count());

        // 4.3.3. Completing tasks
        // Fetch all tasks for the management group
        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = taskService.createTaskQuery()
                                      .taskCandidateGroup("management")
                                      .list();
        for (Task task : tasks) {
            Log.info("Task available: " + task.getName());
        }
        Task task = tasks.get(0);

        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("vacationApproved", "false");
        taskVariables.put("managerMotivation", "We have a tight deadline!");
        taskService.complete(task.getId(), taskVariables);
        // 4.3.4. Suspending and activating a process
        repositoryService.suspendProcessDefinitionByKey("vacationRequest");
        try {
            runtimeService.startProcessInstanceByKey("vacationRequest");
        } catch (ActivitiException e) {
            e.printStackTrace();
        }
        // 4.4. Query API
        List<Task> tasksList2 = taskService.createTaskQuery()
                                       .taskAssignee("kermit")
                                       .processVariableValueEquals("orderId", "0815")
                                       .orderByDueDate()
                                       .asc()
                                       .list();

        ManagementService managementService = processEngine.getManagementService();
        List<Task> tasksList3 = taskService.createNativeTaskQuery()
                                       .sql("SELECT count(*) FROM " + managementService.getTableName(Task.class) + " T WHERE T.NAME_ = #{taskName}")
                                       .parameter("taskName", "gonzoTask")
                                       .list();

        long count = taskService.createNativeTaskQuery()
                                .sql("SELECT count(*) FROM " + managementService.getTableName(Task.class) + " T1, "
                                             + managementService.getTableName(VariableInstanceEntity.class) + " V1 WHERE V1.TASK_ID_ = T1.ID_")
                                .count();
    }

}
