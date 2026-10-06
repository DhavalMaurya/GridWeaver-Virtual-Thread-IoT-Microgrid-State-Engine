import React, { useState, useEffect, useCallback, useRef } from 'react';
import Header from './components/Header';
import MetricsBar from './components/MetricsBar';
import MicrogridMap from './components/MicrogridMap';
import DeviceInspector from './components/DeviceInspector';
import EventLog from './components/EventLog';
import EnergyFlowCard from './components/EnergyFlowCard';
import ScenarioSelector from './components/ScenarioSelector';
import AlertPanel from './components/AlertPanel';

const API_BASE = 'http://localhost:8081/api';
const WS_URL = 'ws://localhost:8081/ws/dashboard';

export default function App() {
  const [connected, setConnected] = useState(false);
  const [metrics, setMetrics] = useState({
    totalDevices: 0,
    onlineDevices: 0,
    telemetryPerSec: 0,
    stateChangesPerSec: 0,
    gridLoad: 55.0,
    activeSimulatorTasks: 0
  });

  const [energyFlow, setEnergyFlow] = useState({
    solarGeneration: 0,
    batteryCharging: 0,
    batteryDischarging: 0,
    gridConsumption: 0,
    netPowerFlow: 0
  });

  const [devices, setDevices] = useState([]);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [events, setEvents] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [isAlertPanelOpen, setIsAlertPanelOpen] = useState(false);
  const [stormActive, setStormActive] = useState(false);
  const [activeMode, setActiveMode] = useState('NORMAL');
  const [currentScale, setCurrentScale] = useState(10000);

  const wsRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const hasInitialSelected = useRef(false);

  // Fetch initial devices list from REST API
  const fetchDevices = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/devices`);
      if (res.ok) {
        const data = await res.json();
        setDevices(data);
        if (data.length > 0 && !hasInitialSelected.current) {
          setSelectedDevice(data[0]);
          hasInitialSelected.current = true;
        }
      }
    } catch (err) {
      console.warn('REST devices fetch failed (waiting for backend):', err.message);
    }
  }, []);

  // Fetch simulator status and active scenario
  const fetchSimulatorStatus = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/simulator/status`);
      if (res.ok) {
        const data = await res.json();
        setStormActive(data.stormActive || false);
        setActiveMode(data.activeMode || 'NORMAL');
      }
    } catch (err) {
      // Backend not yet reached
    }
  }, []);

  // Fetch active alerts
  const fetchAlerts = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/alerts`);
      if (res.ok) {
        const data = await res.json();
        setAlerts(data);
      }
    } catch (err) {
      console.warn('Alerts fetch failed:', err.message);
    }
  }, []);

  // Fetch energy flow
  const fetchEnergyFlow = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/grid/energy-flow`);
      if (res.ok) {
        const data = await res.json();
        setEnergyFlow(data);
      }
    } catch (err) {
      // Backend not yet reached
    }
  }, []);

  const pendingDeviceUpdatesRef = useRef(new Map());

  // Batch flush queued device updates every 300ms to keep 50,000 node updates buttery smooth
  useEffect(() => {
    const interval = setInterval(() => {
      if (pendingDeviceUpdatesRef.current.size === 0) return;
      const updates = pendingDeviceUpdatesRef.current;
      pendingDeviceUpdatesRef.current = new Map();

      setDevices((prevDevices) => {
        if (!prevDevices || prevDevices.length === 0) return prevDevices;
        let changed = false;
        const nextDevices = prevDevices.map((dev) => {
          const update = updates.get(dev.id);
          if (update) {
            changed = true;
            return {
              ...dev,
              state: update.newState,
              batteryPercentage: update.batteryPercentage,
              temperature: update.temperature,
              power: update.power
            };
          }
          return dev;
        });
        return changed ? nextDevices : prevDevices;
      });
    }, 300);

    return () => clearInterval(interval);
  }, []);

  // WebSocket connection lifecycle
  useEffect(() => {
    let active = true;

    function connectWs() {
      try {
        const ws = new WebSocket(WS_URL);
        wsRef.current = ws;

        ws.onopen = () => {
          if (!active) return;
          setConnected(true);
          console.log('[WebSocket] Connected to GridWeaver backend.');
          fetchDevices();
          fetchSimulatorStatus();
          fetchAlerts();
          fetchEnergyFlow();
        };

        ws.onmessage = (message) => {
          if (!active) return;
          try {
            const data = JSON.parse(message.data);

            if (data.eventType === 'GRID_METRICS_UPDATE' || data.eventType === 'INITIAL_METRICS') {
              setMetrics({
                totalDevices: data.totalDevices,
                onlineDevices: data.onlineDevices,
                telemetryPerSec: data.telemetryPerSec,
                stateChangesPerSec: data.stateChangesPerSec,
                gridLoad: data.gridLoad,
                activeSimulatorTasks: data.activeSimulatorTasks
              });

              if (data.energyFlow) {
                setEnergyFlow(data.energyFlow);
              }
            } else if (data.eventType === 'BATTERY_STATE_CHANGED') {
              // Add to event log ticker
              setEvents((prev) => [data, ...prev.slice(0, 49)]);

              // Queue for batch update
              pendingDeviceUpdatesRef.current.set(data.deviceId, data);

              // Update inspector if this device is selected
              setSelectedDevice((curr) => {
                if (curr && curr.id === data.deviceId) {
                  return {
                    ...curr,
                    state: data.newState,
                    batteryPercentage: data.batteryPercentage,
                    temperature: data.temperature,
                    power: data.power
                  };
                }
                return curr;
              });
            } else if (data.eventType === 'ALERT_CREATED') {
              const newAlert = data.alert;
              setAlerts((prev) => {
                const filtered = prev.filter((a) => a.id !== newAlert.id);
                return [newAlert, ...filtered];
              });
            } else if (data.eventType === 'ALERT_RESOLVED') {
              const resolvedAlert = data.alert;
              setAlerts((prev) =>
                prev.map((a) => (a.id === resolvedAlert.id ? resolvedAlert : a))
              );
            }
          } catch (err) {
            console.error('Failed to parse WebSocket message:', err);
          }
        };

        ws.onclose = () => {
          if (!active) return;
          setConnected(false);
          console.log('[WebSocket] Disconnected. Retrying in 2 seconds...');
          reconnectTimeoutRef.current = setTimeout(connectWs, 2000);
        };

        ws.onerror = () => {
          ws.close();
        };
      } catch (err) {
        reconnectTimeoutRef.current = setTimeout(connectWs, 2000);
      }
    }

    connectWs();
    fetchDevices();
    fetchAlerts();

    return () => {
      active = false;
      if (wsRef.current) wsRef.current.close();
      if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current);
    };
  }, [fetchDevices, fetchSimulatorStatus, fetchAlerts, fetchEnergyFlow]);

  // Periodic device poll to keep new scale devices synced (only when WS is disconnected)
  useEffect(() => {
    if (connected) return; // Skip periodic REST polling when WebSocket is live to save CPU
    const interval = setInterval(fetchDevices, 5000);
    return () => clearInterval(interval);
  }, [connected, fetchDevices]);

  // Scenario Mode Handler
  const handleSelectScenario = async (mode) => {
    try {
      setActiveMode(mode);
      setStormActive(mode === 'STORM');
      const res = await fetch(`${API_BASE}/simulator/mode?mode=${mode}`, { method: 'POST' });
      if (res.ok) {
        const data = await res.json();
        setActiveMode(data.activeMode);
        setStormActive(data.stormActive);
      }
    } catch (err) {
      console.error('Failed to change scenario mode:', err);
    }
  };

  // Storm toggle button handler
  const handleToggleStorm = async () => {
    const nextMode = stormActive ? 'NORMAL' : 'STORM';
    handleSelectScenario(nextMode);
  };

  // Scale change handler (10, 100, 1K, 5K, 10K)
  const handleScaleChange = async (count) => {
    try {
      setCurrentScale(count);
      const res = await fetch(`${API_BASE}/simulator/scale?count=${count}`, { method: 'POST' });
      if (res.ok) {
        setTimeout(async () => {
          try {
            const devRes = await fetch(`${API_BASE}/devices`);
            if (devRes.ok) {
              const newDevs = await devRes.json();
              setDevices(newDevs);
              if (newDevs.length > 0) {
                setSelectedDevice((prev) => {
                  if (!prev) return newDevs[0];
                  const match = newDevs.find((d) => d.id === prev.id);
                  return match || newDevs[0];
                });
              }
            }
          } catch (ignored) {}
        }, 500);
      }
    } catch (err) {
      console.error('Failed to scale simulator:', err);
    }
  };

  // Resolve Alert Handler
  const handleResolveAlert = async (alertId) => {
    try {
      const res = await fetch(`${API_BASE}/alerts/${alertId}/resolve`, { method: 'POST' });
      if (res.ok) {
        setAlerts((prev) =>
          prev.map((a) => (a.id === alertId ? { ...a, resolved: true, resolvedAt: new Date().toISOString() } : a))
        );
      }
    } catch (err) {
      console.error('Failed to resolve alert:', err);
    }
  };

  // Clear Resolved Alerts Handler
  const handleClearResolvedAlerts = () => {
    setAlerts((prev) => prev.filter((a) => !a.resolved));
  };

  const activeAlertCount = alerts.filter((a) => !a.resolved).length;

  return (
    <div className="app-container">
      {/* Top Header */}
      <Header
        connected={connected}
        stormActive={stormActive}
        onToggleStorm={handleToggleStorm}
        currentScale={currentScale}
        onScaleChange={handleScaleChange}
        alertCount={activeAlertCount}
        onOpenAlerts={() => setIsAlertPanelOpen(true)}
      />

      {/* 5 Focused KPI Metric Cards */}
      <MetricsBar metrics={metrics} />

      {/* Phase 2: Scenario Dispatcher & Energy Flow Ribbon (ABOVE MAP) */}
      <div className="command-ribbon">
        <ScenarioSelector activeMode={activeMode} onSelectMode={handleSelectScenario} />
        <EnergyFlowCard energyFlow={energyFlow} />
      </div>

      {/* Main Microgrid Workspace (100% Full-Width Interactive GIS Map) */}
      <div className="dashboard-main full-width">
        <MicrogridMap
          devices={devices}
          selectedDevice={selectedDevice}
          onSelectDevice={setSelectedDevice}
        />
      </div>

      {/* Live State Machine Event Log Section (BELOW MAP) */}
      <div className="event-log-container">
        <EventLog events={events} />
      </div>

      {/* Slide-over Incidents & Alerts Drawer */}
      <AlertPanel
        isOpen={isAlertPanelOpen}
        onClose={() => setIsAlertPanelOpen(false)}
        alerts={alerts}
        onResolveAlert={handleResolveAlert}
        onClearResolvedAlerts={handleClearResolvedAlerts}
      />
    </div>
  );
}
