package com.matheus.procurement.task;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachine
public class TaskStateMachineConfig extends EnumStateMachineConfigurerAdapter<TaskStatus, TaskEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<TaskStatus, TaskEvent> states) throws Exception {
        states
        .withStates()
        .initial(TaskStatus.PENDING)
        .states(EnumSet.allOf(TaskStatus.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<TaskStatus, TaskEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(TaskStatus.PENDING).target(TaskStatus.RUNNING).event(TaskEvent.START)
                .and()
                .withExternal()
                .source(TaskStatus.RUNNING).target(TaskStatus.COMPLETED).event(TaskEvent.SUCCEED)
                .and()
                .withExternal()
                .source(TaskStatus.RUNNING).target(TaskStatus.FAILED).event(TaskEvent.FAIL);
    }


}
