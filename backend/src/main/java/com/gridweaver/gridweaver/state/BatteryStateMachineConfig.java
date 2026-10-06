package com.gridweaver.gridweaver.state;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
public class BatteryStateMachineConfig extends EnumStateMachineConfigurerAdapter<BatteryState, BatteryEvent> {

    @Override
    public void configure(StateMachineConfigurationConfigurer<BatteryState, BatteryEvent> config) throws Exception {
        config.withConfiguration()
                .autoStartup(true);
    }

    @Override
    public void configure(StateMachineStateConfigurer<BatteryState, BatteryEvent> states) throws Exception {
        states.withStates()
                .initial(BatteryState.IDLE)
                .states(EnumSet.allOf(BatteryState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<BatteryState, BatteryEvent> transitions) throws Exception {
        transitions
                // IDLE transitions
                .withExternal()
                    .source(BatteryState.IDLE).target(BatteryState.CHARGING).event(BatteryEvent.START_CHARGING)
                    .and()
                .withExternal()
                    .source(BatteryState.IDLE).target(BatteryState.DISCHARGING).event(BatteryEvent.START_DISCHARGING)
                    .and()
                .withExternal()
                    .source(BatteryState.IDLE).target(BatteryState.FAULT).event(BatteryEvent.FAULT_DETECTED)
                    .and()

                // CHARGING transitions
                .withExternal()
                    .source(BatteryState.CHARGING).target(BatteryState.IDLE).event(BatteryEvent.STOP)
                    .and()
                .withExternal()
                    .source(BatteryState.CHARGING).target(BatteryState.DISCHARGING).event(BatteryEvent.START_DISCHARGING)
                    .and()
                .withExternal()
                    .source(BatteryState.CHARGING).target(BatteryState.FAULT).event(BatteryEvent.FAULT_DETECTED)
                    .and()

                // DISCHARGING transitions
                .withExternal()
                    .source(BatteryState.DISCHARGING).target(BatteryState.IDLE).event(BatteryEvent.STOP)
                    .and()
                .withExternal()
                    .source(BatteryState.DISCHARGING).target(BatteryState.CHARGING).event(BatteryEvent.START_CHARGING)
                    .and()
                .withExternal()
                    .source(BatteryState.DISCHARGING).target(BatteryState.FAULT).event(BatteryEvent.FAULT_DETECTED)
                    .and()

                // FAULT transitions
                .withExternal()
                    .source(BatteryState.FAULT).target(BatteryState.IDLE).event(BatteryEvent.FAULT_CLEARED);
    }
}
