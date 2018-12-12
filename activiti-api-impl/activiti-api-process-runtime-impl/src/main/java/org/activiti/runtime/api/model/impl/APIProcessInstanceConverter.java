/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.runtime.api.model.impl;

import java.util.Optional;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;

public class APIProcessInstanceConverter extends ListConverter<org.activiti.engine.runtime.ProcessInstance, ProcessInstance>
        implements ModelConverter<org.activiti.engine.runtime.ProcessInstance, ProcessInstance> {

    @Override
    public ProcessInstance from(org.activiti.engine.runtime.ProcessInstance internalProcessInstance) {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId(internalProcessInstance.getId());
        processInstance.setName(internalProcessInstance.getName());
        processInstance.setDescription(internalProcessInstance.getDescription());
        processInstance.setProcessDefinitionId(internalProcessInstance.getProcessDefinitionId());
        processInstance.setInitiator(internalProcessInstance.getStartUserId());
        processInstance.setStartDate(internalProcessInstance.getStartTime());
        processInstance.setProcessDefinitionKey(internalProcessInstance.getProcessDefinitionKey());
        processInstance.setBusinessKey(internalProcessInstance.getBusinessKey());
        processInstance.setStatus(calculateStatus(internalProcessInstance));
        processInstance.setProcessDefinitionVersion(internalProcessInstance.getProcessDefinitionVersion());
        
        //To do: it is not the best way to search parentProcessId by this method, it will require extra queries!
        
        //Set parent ProcessInstance Id
        if(internalProcessInstance.getSuperExecutionId()!=null && ExecutionEntity.class.isInstance(internalProcessInstance)) {
            ExecutionEntity executionEntity = ExecutionEntity.class.cast(internalProcessInstance);
            
            if (Context.getCommandContext()==null) {
                  ProcessEngineConfigurationImpl impl = (ProcessEngineConfigurationImpl)
                        ProcessEngines.getDefaultProcessEngine().getProcessEngineConfiguration();

                        impl.getCommandExecutor().execute(new Command<Void> () {

                        @Override
                        public Void execute(CommandContext commandContext) {
                            Optional.ofNullable(executionEntity.getSuperExecution())
                                    .ifPresent(superExecution -> processInstance.setParentId(superExecution.getProcessInstanceId()));

                             return null;
                        }});

            } else {
                  Optional.ofNullable(executionEntity.getSuperExecution())
                          .ifPresent(superExecution -> processInstance.setParentId(superExecution.getProcessInstanceId()));
            }
        }
        return processInstance;
    }

    private ProcessInstance.ProcessInstanceStatus calculateStatus(org.activiti.engine.runtime.ProcessInstance internalProcessInstance) {
        if (internalProcessInstance.isSuspended()) {
            return ProcessInstance.ProcessInstanceStatus.SUSPENDED;
        } else if (internalProcessInstance.isEnded()) {
            return ProcessInstance.ProcessInstanceStatus.COMPLETED;
        }
        return ProcessInstance.ProcessInstanceStatus.RUNNING;
    }
}
