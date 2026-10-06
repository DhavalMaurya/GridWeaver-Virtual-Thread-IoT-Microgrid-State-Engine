package com.gridweaver.gridweaver.state;

import com.gridweaver.gridweaver.dto.StateChangeEvent;
import com.gridweaver.gridweaver.model.Battery;
import com.gridweaver.gridweaver.service.DeviceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatteryStateService {

    private final StateMachineFactory<BatteryState, BatteryEvent> stateMachineFactory;
    private final DeviceRegistry deviceRegistry;
    private final ApplicationEventPublisher eventPublisher;

    private final ConcurrentHashMap<String, StateMachine<BatteryState, BatteryEvent>> stateMachines = new ConcurrentHashMap<>();

    public synchronized StateMachine<BatteryState, BatteryEvent> getOrCreateStateMachine(String deviceId, BatteryState initialState) {
        return stateMachines.computeIfAbsent(deviceId, id -> {
            StateMachine<BatteryState, BatteryEvent> sm = stateMachineFactory.getStateMachine(id);
            try {
                if (initialState != null && initialState != BatteryState.IDLE) {
                    sm.getStateMachineAccessor().doWithAllRegions(access ->
                            access.resetStateMachine(new DefaultStateMachineContext<>(initialState, null, null, null)));
                }
                sm.startReactively().block();
            } catch (Exception e) {
                log.error("Failed to initialize state machine for device {}: {}", id, e.getMessage());
            }
            return sm;
        });
    }

    public boolean fireEvent(String deviceId, BatteryEvent event, String reason, Double gridLoad) {
        Battery battery = (Battery) deviceRegistry.findById(deviceId)
                .filter(d -> d instanceof Battery)
                .orElse(null);

        if (battery == null) {
            log.warn("Cannot fire event {} for unknown battery {}", event, deviceId);
            return false;
        }

        BatteryState previousState = battery.getState() != null ? battery.getState() : BatteryState.IDLE;
        StateMachine<BatteryState, BatteryEvent> sm = getOrCreateStateMachine(deviceId, previousState);

        // Synchronize transitions on the state machine instance to maintain strict state integrity
        synchronized (sm) {
            boolean accepted = sm.sendEvent(MessageBuilder.withPayload(event)
                    .setHeader("deviceId", deviceId)
                    .setHeader("reason", reason)
                    .build());

            if (!accepted) {
                log.debug("State transition rejected by state machine for {}: {} with event {}", deviceId, previousState, event);
                return false;
            }

            BatteryState newState = sm.getState().getId();
            if (newState != previousState) {
                battery.setState(newState);
                deviceRegistry.save(battery);
                deviceRegistry.recordStateChange();

                log.info("Battery state changed: {} [{}] -> [{}] (Reason: {})", deviceId, previousState, newState, reason);

                StateChangeEvent changeEvent = StateChangeEvent.builder()
                        .eventType("BATTERY_STATE_CHANGED")
                        .deviceId(deviceId)
                        .previousState(previousState)
                        .newState(newState)
                        .reason(reason)
                        .gridLoad(gridLoad)
                        .timestamp(Instant.now())
                        .batteryPercentage(battery.getBatteryPercentage())
                        .temperature(battery.getTemperature())
                        .power(battery.getPower())
                        .build();

                eventPublisher.publishEvent(changeEvent);
                return true;
            }
        }

        return false;
    }
}
