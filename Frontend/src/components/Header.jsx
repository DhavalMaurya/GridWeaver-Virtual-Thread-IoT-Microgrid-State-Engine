import React from 'react';
import { Zap, CloudLightning, Activity, Cpu, AlertTriangle } from 'lucide-react';

export default function Header({
  connected,
  stormActive,
  onToggleStorm,
  currentScale,
  onScaleChange,
  alertCount = 0,
  onOpenAlerts
}) {
  const scaleOptions = [10, 100, 1000, 10000, 50000];

  return (
    <header className="app-header">
      <div className="brand-section">
        <div className="brand-logo">
          <Zap size={22} />
        </div>
        <div>
          <div className="brand-title">GridWeaver</div>
          <div className="brand-subtitle">Virtual Thread IoT Microgrid Platform • Phase 2</div>
        </div>
      </div>

      <div className="header-controls">
        <div className={`status-badge ${connected ? 'connected' : 'disconnected'}`}>
          <div className="status-dot"></div>
          <span>{connected ? 'WS LIVE' : 'WS CONNECTING'}</span>
        </div>

        <button
          className={`incident-nav-btn ${alertCount > 0 ? 'has-alerts' : ''}`}
          onClick={onOpenAlerts}
          title="Open Microgrid Safety & Incidents Center"
        >
          <AlertTriangle size={15} />
          <span>INCIDENTS</span>
          {alertCount > 0 && <span className="alert-count-pill">{alertCount}</span>}
        </button>

        <div className="scale-selector">
          <span style={{ fontSize: '0.72rem', color: '#64748b', fontWeight: 600, padding: '0 8px' }}>
            NODES:
          </span>
          {scaleOptions.map(count => (
            <button
              key={count}
              className={`scale-btn ${currentScale === count ? 'active' : ''}`}
              onClick={() => onScaleChange(count)}
            >
              {count >= 1000 ? `${count / 1000}K` : count}
            </button>
          ))}
        </div>

        <button
          className={`storm-btn ${stormActive ? 'active' : ''}`}
          onClick={onToggleStorm}
          title="Toggle high-load grid storm simulation"
        >
          <CloudLightning size={16} />
          <span>{stormActive ? 'STORM ACTIVE' : 'STORM MODE'}</span>
        </button>
      </div>
    </header>
  );
}
