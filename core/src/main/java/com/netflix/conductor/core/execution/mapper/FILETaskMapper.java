 /*
  * Copyright 2018 Netflix, Inc.
  * <p>
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * <p>
  * http://www.apache.org/licenses/LICENSE-2.0
  * <p>
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package com.netflix.conductor.core.execution.mapper;

 import com.netflix.conductor.common.metadata.tasks.Task;
 import com.netflix.conductor.common.metadata.tasks.TaskDef;
 import com.netflix.conductor.common.metadata.workflow.TaskType;
 import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
 import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
 import com.netflix.conductor.common.run.Workflow;
 import com.netflix.conductor.core.execution.ParametersUtils;
 import com.netflix.conductor.core.execution.TerminateWorkflowException;
 import com.netflix.conductor.dao.MetadataDAO;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.util.Collections;
 import java.util.List;
 import java.util.Map;
 import java.util.Objects;
 import java.util.Optional;

 /**
  * An implementation of {@link TaskMapper} to map a {@link WorkflowTask} of type {@link TaskType#HTTP}
  * to a {@link Task} of type {@link TaskType#HTTP} with {@link Task.Status#SCHEDULED}
  */
 public class FILETaskMapper implements TaskMapper {

     public static final Logger logger = LoggerFactory.getLogger(com.netflix.conductor.core.execution.mapper.FILETaskMapper.class);

     private final ParametersUtils parametersUtils;
     private final MetadataDAO metadataDAO;

     public FILETaskMapper(ParametersUtils parametersUtils, MetadataDAO metadataDAO) {
         this.parametersUtils = parametersUtils;
         this.metadataDAO = metadataDAO;
     }

     /**
      * This method maps a {@link WorkflowTask} of type {@link TaskType#HTTP}
      * to a {@link Task} in a {@link Task.Status#SCHEDULED} state
      *
      * @param taskMapperContext: A wrapper class containing the {@link WorkflowTask}, {@link WorkflowDef}, {@link Workflow} and a string representation of the TaskId
      * @return a List with just one HTTP task
      * @throws TerminateWorkflowException In case if the task definition does not exist
      */
     @Override
     public List<Task> getMappedTasks(TaskMapperContext taskMapperContext) throws TerminateWorkflowException {

         logger.debug("TaskMapperContext {} in FileTaskMapper", taskMapperContext);

         WorkflowTask taskToSchedule = taskMapperContext.getTaskToSchedule();
         taskToSchedule.getInputParameters().put("asyncComplete", taskToSchedule.isAsyncComplete());
         Workflow workflowInstance = taskMapperContext.getWorkflowInstance();
         String taskId = taskMapperContext.getTaskId();
         int retryCount = taskMapperContext.getRetryCount();
         
         TaskDef taskDefinition = Optional.ofNullable(taskMapperContext.getTaskDefinition())
                 .orElseGet(() -> Optional.ofNullable(metadataDAO.getTaskDef(taskToSchedule.getName()))
                		 .orElse(null));

         Map<String, Object> input = parametersUtils.getTaskInputV2(taskToSchedule.getInputParameters(), workflowInstance, taskId, taskDefinition);
         Boolean asynComplete = (Boolean)input.get("asyncComplete");

         Task fileTask = new Task();
         fileTask.setTaskType(taskToSchedule.getType());
         fileTask.setTaskDefName(taskToSchedule.getName());
         fileTask.setReferenceTaskName(taskToSchedule.getTaskReferenceName());
         fileTask.setWorkflowInstanceId(workflowInstance.getWorkflowId());
         fileTask.setWorkflowType(workflowInstance.getWorkflowName());
         fileTask.setCorrelationId(workflowInstance.getCorrelationId());
         fileTask.setScheduledTime(System.currentTimeMillis());
         fileTask.setTaskId(taskId);
         fileTask.setInputData(input);
         fileTask.getInputData().put("asyncComplete", asynComplete);
         fileTask.setStatus(Task.Status.SCHEDULED);
         fileTask.setRetryCount(retryCount);
         fileTask.setCallbackAfterSeconds(taskToSchedule.getStartDelay());
         fileTask.setWorkflowTask(taskToSchedule);
         fileTask.setWorkflowPriority(workflowInstance.getPriority());
         if (Objects.nonNull(taskDefinition)) {
             fileTask.setRateLimitPerFrequency(taskDefinition.getRateLimitPerFrequency());
             fileTask.setRateLimitFrequencyInSeconds(taskDefinition.getRateLimitFrequencyInSeconds());
             fileTask.setIsolationGroupId(taskDefinition.getIsolationGroupId());
             fileTask.setDomain(taskDefinition.getExecutionNameSpace());
         }
         return Collections.singletonList(fileTask);
     }
 }
