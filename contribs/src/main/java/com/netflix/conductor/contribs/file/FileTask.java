/*
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.conductor.contribs.file;


import java.io.FileWriter;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.Task.Status;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.core.execution.WorkflowExecutor;
import com.netflix.conductor.core.execution.tasks.WorkflowSystemTask;

/**
 * @author Prabhav
 * Task that enables writing to file
 */
@Singleton
public class FileTask extends WorkflowSystemTask {
	
	private static final Logger logger = LoggerFactory.getLogger(FileTask.class);
	
	public static final String BODY = "body";
	public static final String FILE_NAME = "filename";
	static final String MISSING_REQUEST = "Missing FILE request. Task input MUST have a '" + BODY + "' key";
	
	protected Configuration config;

	@Inject
	public FileTask(Configuration config) {
		super("FILE");
		this.config = config;
	}

	@Override
	public void start(Workflow workflow, Task task, WorkflowExecutor executor) {
		Object request = task.getInputData().get(BODY);
		String filename = (String) task.getInputData().get(FILE_NAME);
		logger.info("Request: {}", request);
		task.setWorkerId(config.getServerId());
		if(request == null) {
			task.setReasonForIncompletion(MISSING_REQUEST);
			task.setStatus(Status.FAILED);
			return;
		}
		try(FileWriter fw = new FileWriter(System.getProperty("filetask.location") + filename)) {
			fw.write(String.valueOf(request));
		}catch(Exception e) {
			logger.error("Failed to invoke file task: {} in workflow: {}", task.getTaskId(), task.getWorkflowInstanceId(), e);
			task.setStatus(Status.FAILED);
			task.setReasonForIncompletion("Failed to invoke file task due to: " + e.toString());
			task.getOutputData().put("response", e.toString());
		}
	}
	
	@Override
	public boolean execute(Workflow workflow, Task task, WorkflowExecutor executor) {
		return false;
	}
	
	@Override
	public void cancel(Workflow workflow, Task task, WorkflowExecutor executor) {
		task.setStatus(Status.CANCELED);
	}
	
	@Override
	public boolean isAsync() {
		return true;
	}
	
	@Override
	public int getRetryTimeInSecond() {
		return 60;
	}
	
}
