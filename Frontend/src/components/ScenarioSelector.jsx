import React from 'react';

const SCENARIOS = [
  {
    id: 'NORMAL',
    label: 'Normal Grid',
    icon: '🍃',
    desc: 'Load ~55%, solar nominal, storage standby',
    color: '#10b981'
  },
  {
    id: 'HIGH_DEMAND',
    label: 'High Demand',
    icon: '🔥',
    desc: 'Load surges to ~88%, storage dispatches to protect grid',
    color: '#f59e0b'
  },
  {
    id: 'STORM',
    label: 'Severe Storm',
    icon: '⚡',
    desc: 'Load ~92%, 80% solar drop, maximum dispatch',
    color: '#ef4444'
  },
  {
    id: 'SOLAR_LOSS',
    label: 'Solar Loss',
    icon: '🌑',
    desc: 'Complete PV array disconnect (0 kW), storage compensates',
    color: '#8b5cf6'
  },
  {
    id: 'RECOVERY',
    label: 'Off-Peak Recovery',
    icon: '🔄',
    desc: 'Load ~35%, solar peak, rapid battery recharging',
    color: '#06b6d4'
  }
];

export default function ScenarioSelector({ activeMode, onSelectMode }) {
  return (
    <div className="scenario-selector-card">
      <div className="scenario-header">
        <span className="scenario-title">⚡ SIMULATION SCENARIO DISPATCHER</span>
        <span className="active-mode-badge" style={{ borderColor: SCENARIOS.find(s => s.id === activeMode)?.color }}>
          Active: <strong>{activeMode || 'NORMAL'}</strong>
        </span>
      </div>

      <div className="scenario-button-group">
        {SCENARIOS.map(scenario => {
          const isActive = activeMode === scenario.id;
          return (
            <button
              key={scenario.id}
              className={`scenario-btn ${isActive ? 'active' : ''}`}
              style={{
                borderColor: isActive ? scenario.color : 'rgba(255, 255, 255, 0.1)',
                boxShadow: isActive ? `0 0 12px ${scenario.color}40` : 'none'
              }}
              onClick={() => onSelectMode(scenario.id)}
              title={scenario.desc}
            >
              <span className="scenario-btn-icon">{scenario.icon}</span>
              <span className="scenario-btn-label">{scenario.label}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
