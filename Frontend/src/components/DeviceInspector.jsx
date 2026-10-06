import React from 'react';
import { BatteryCharging, Sun, ShieldCheck, X } from 'lucide-react';

export default function DeviceInspector({ device, onClose }) {
  if (!device) {
    return null;
  }

  const isBattery = device.deviceType === 'BATTERY';
  const state = device.state || 'IDLE';

  let badgeLabel = state;
  let badgeClass = state;

  if (!isBattery) {
    const power = device.power || 0;
    if (!device.online) {
      badgeLabel = 'OFFLINE';
      badgeClass = 'OFFLINE';
    } else if (power === 0) {
      badgeLabel = 'ZERO OUTPUT';
      badgeClass = 'SOLAR_ZERO';
    } else if (power <= 0.8) {
      badgeLabel = 'STORM CLOUD';
      badgeClass = 'SOLAR_STORM';
    } else {
      badgeLabel = 'GENERATING';
      badgeClass = 'SOLAR_GEN';
    }
  }

  return (
    <div className="floating-inspector-card">
      <div className="inspector-header">
        <div className="inspector-title">
          {isBattery ? <BatteryCharging size={18} color="#06b6d4" /> : <Sun size={18} color="#06b6d4" />}
          <span>{device.id}</span>
          <span className={`state-pill ${badgeClass}`}>
            {badgeLabel}
          </span>
        </div>
        {onClose && (
          <button className="inspector-close-btn" onClick={onClose} title="Close inspector panel">
            <X size={16} />
          </button>
        )}
      </div>

      <div className="inspector-meta-row">
        <span>Type: <strong style={{ color: '#f8fafc' }}>{device.deviceType}</strong></span>
        <span>Zone: <strong style={{ color: '#f8fafc' }}>{device.zoneId || 'ZONE-1'}</strong></span>
      </div>

      {isBattery && (
        <div style={{ marginTop: '12px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', marginBottom: '4px' }}>
            <span style={{ color: '#94a3b8' }}>State of Charge (SoC)</span>
            <strong style={{ color: '#f8fafc', fontFamily: 'JetBrains Mono' }}>
              {(device.batteryPercentage || 0).toFixed(1)}%
            </strong>
          </div>
          <div className="load-progress-track">
            <div
              className="load-progress-fill"
              style={{
                width: `${Math.min(100, Math.max(0, device.batteryPercentage || 0))}%`,
                backgroundColor: (device.batteryPercentage || 0) < 20 ? '#ef4444' : '#10b981'
              }}
            ></div>
          </div>
        </div>
      )}

      <div className="inspector-grid">
        <div className="stat-item">
          <div className="stat-item-label">Power Output</div>
          <div className="stat-item-val" style={{ color: '#06b6d4' }}>
            {(device.power || 0).toFixed(2)} kW
          </div>
        </div>

        <div className="stat-item">
          <div className="stat-item-label">Status / Link</div>
          <div className="stat-item-val" style={{ color: device.online ? '#10b981' : '#ef4444' }}>
            {device.online ? 'ONLINE' : 'OFFLINE'}
          </div>
        </div>

        <div className="stat-item">
          <div className="stat-item-label">Voltage / Current</div>
          <div className="stat-item-val" style={{ fontSize: '0.8rem' }}>
            {(device.voltage || 0).toFixed(0)}V / {(device.current || 0).toFixed(1)}A
          </div>
        </div>

        {isBattery ? (
          <>
            <div className="stat-item">
              <div className="stat-item-label">Temperature</div>
              <div className="stat-item-val" style={{ color: (device.temperature || 0) > 50 ? '#ef4444' : '#f8fafc' }}>
                {(device.temperature || 0).toFixed(1)} °C
              </div>
            </div>
            <div className="stat-item">
              <div className="stat-item-label">Health</div>
              <div className="stat-item-val" style={{ color: device.health === 'GOOD' ? '#10b981' : '#f59e0b' }}>
                {device.health || 'GOOD'}
              </div>
            </div>
            <div className="stat-item">
              <div className="stat-item-label">Max Dispatch</div>
              <div className="stat-item-val" style={{ color: '#06b6d4' }}>
                {device.maxDischargeRate || 5.0} kW
              </div>
            </div>
          </>
        ) : (
          <div className="stat-item">
            <div className="stat-item-label">Efficiency</div>
            <div className="stat-item-val" style={{ color: '#10b981' }}>
              {(device.panelEfficiency || 21.5).toFixed(1)} %
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
