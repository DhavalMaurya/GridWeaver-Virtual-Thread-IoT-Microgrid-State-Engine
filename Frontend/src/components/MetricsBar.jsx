import React from 'react';
import { Server, Wifi, ArrowDownUp, RefreshCw, Gauge } from 'lucide-react';

export default function MetricsBar({ metrics }) {
  const gridLoad = metrics?.gridLoad || 0;

  // Determine progress bar color based on grid load thresholds
  let loadColor = '#10b981'; // Green for normal
  if (gridLoad > 80) loadColor = '#ef4444'; // Red for high discharge
  else if (gridLoad < 40) loadColor = '#f59e0b'; // Amber for charging

  return (
    <div className="metrics-strip">
      {/* 1. Total Devices */}
      <div className="metric-card">
        <div className="metric-header">
          <span className="metric-label">Total Devices</span>
          <Server size={16} className="metric-icon" />
        </div>
        <div className="metric-value">{metrics?.totalDevices || 0}</div>
        <div className="metric-sub">{metrics?.activeSimulatorTasks || 0} active virtual tasks</div>
      </div>

      {/* 2. Online Devices */}
      <div className="metric-card">
        <div className="metric-header">
          <span className="metric-label">Online Devices</span>
          <Wifi size={16} className="metric-icon" />
        </div>
        <div className="metric-value" style={{ color: '#10b981' }}>
          {metrics?.onlineDevices || 0}
        </div>
        <div className="metric-sub">
          {metrics?.totalDevices ? Math.round(((metrics.onlineDevices || 0) / metrics.totalDevices) * 100) : 100}% availability
        </div>
      </div>

      {/* 3. Telemetry Ingestion Rate */}
      <div className="metric-card">
        <div className="metric-header">
          <span className="metric-label">Telemetry / Sec</span>
          <ArrowDownUp size={16} className="metric-icon" />
        </div>
        <div className="metric-value" style={{ color: '#06b6d4' }}>
          {(metrics?.telemetryPerSec || 0).toLocaleString()}
        </div>
        <div className="metric-sub">throughput per second</div>
      </div>

      {/* 4. State Changes Rate */}
      <div className="metric-card">
        <div className="metric-header">
          <span className="metric-label">State Changes / Sec</span>
          <RefreshCw size={16} className="metric-icon" />
        </div>
        <div className="metric-value" style={{ color: '#f59e0b' }}>
          {(metrics?.stateChangesPerSec || 0).toLocaleString()}
        </div>
        <div className="metric-sub">Spring State transitions</div>
      </div>

      {/* 5. Grid Load % */}
      <div className="metric-card">
        <div className="metric-header">
          <span className="metric-label">Simulated Grid Load</span>
          <Gauge size={16} className="metric-icon" />
        </div>
        <div className="metric-value" style={{ color: loadColor }}>
          {gridLoad.toFixed(1)}%
        </div>
        <div className="load-progress-track">
          <div
            className="load-progress-fill"
            style={{
              width: `${Math.min(100, Math.max(0, gridLoad))}%`,
              backgroundColor: loadColor
            }}
          ></div>
        </div>
      </div>
    </div>
  );
}
