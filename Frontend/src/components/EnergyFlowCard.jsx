import React from 'react';

function formatVal(kW) {
  const abs = Math.abs(kW || 0);
  if (abs >= 1000) {
    return { val: (kW / 1000).toFixed(2), unit: 'MW' };
  }
  return { val: (kW || 0).toFixed(1), unit: 'kW' };
}

export default function EnergyFlowCard({ energyFlow }) {
  const solar = energyFlow?.solarGeneration ?? 0;
  const charging = energyFlow?.batteryCharging ?? 0;
  const discharging = energyFlow?.batteryDischarging ?? 0;
  const consumption = energyFlow?.gridConsumption ?? 0;
  const net = energyFlow?.netPowerFlow ?? 0;

  const isNetExporting = net >= 0;

  const solarFmt = formatVal(solar);
  const chargingFmt = formatVal(charging);
  const dischargingFmt = formatVal(discharging);
  const consumptionFmt = formatVal(consumption);
  const netFmt = formatVal(net);

  return (
    <div className="energy-flow-card">
      <div className="card-header">
        <div className="card-title">
          <span className="pulse-dot"></span>
          REAL-TIME MICROGRID ENERGY FLOW
        </div>
        <div className={`net-flow-badge ${isNetExporting ? 'surplus' : 'deficit'}`}>
          {isNetExporting ? '⚡ SURPLUS (SELF-SUFFICIENT)' : '⚡ GRID IMPORT ACTIVE'}
        </div>
      </div>

      <div className="flow-grid">
        {/* Node 1: Solar */}
        <div className="flow-node solar-node">
          <div className="node-icon">☀️</div>
          <div className="node-meta">
            <span className="node-label">Solar PV Array</span>
            <span className="node-value text-cyan">{solarFmt.val} <small>{solarFmt.unit}</small></span>
          </div>
          <div className="flow-indicator inflow">➔ To Bus</div>
        </div>

        {/* Node 2: Battery Storage */}
        <div className="flow-node battery-node">
          <div className="node-icon">🔋</div>
          <div className="node-meta">
            <span className="node-label">Storage (BESS)</span>
            {discharging > 0 ? (
              <span className="node-value text-red">-{dischargingFmt.val} <small>{dischargingFmt.unit} (Discharging)</small></span>
            ) : charging > 0 ? (
              <span className="node-value text-amber">+{chargingFmt.val} <small>{chargingFmt.unit} (Charging)</small></span>
            ) : (
              <span className="node-value text-emerald">0.0 <small>kW (Standby)</small></span>
            )}
          </div>
          <div className={`flow-indicator ${discharging > 0 ? 'inflow' : charging > 0 ? 'outflow' : 'idle'}`}>
            {discharging > 0 ? '➔ To Bus' : charging > 0 ? '➔ Absorbing' : 'Standby'}
          </div>
        </div>

        {/* Node 3: Microgrid Demand */}
        <div className="flow-node load-node">
          <div className="node-icon">🏙️</div>
          <div className="node-meta">
            <span className="node-label">Microgrid Demand</span>
            <span className="node-value text-purple">{consumptionFmt.val} <small>{consumptionFmt.unit}</small></span>
          </div>
          <div className="flow-indicator outflow">➔ Consumed</div>
        </div>

        {/* Node 4: Net Grid Balance */}
        <div className="flow-node grid-node">
          <div className="node-icon">🌐</div>
          <div className="node-meta">
            <span className="node-label">Net Balance</span>
            <span className={`node-value ${isNetExporting ? 'text-emerald' : 'text-amber'}`}>
              {net > 0 ? `+${netFmt.val}` : netFmt.val} <small>{netFmt.unit}</small>
            </span>
          </div>
          <div className="flow-indicator net">
            {isNetExporting ? 'Surplus' : 'Deficit'}
          </div>
        </div>
      </div>
    </div>
  );
}
