import React, { useState } from 'react';

export default function AlertPanel({ alerts = [], onResolveAlert, onClearResolvedAlerts, isOpen, onClose }) {
  const [filterActive, setFilterActive] = useState(true);

  const displayedAlerts = alerts.filter(a => (filterActive ? !a.resolved : true));
  const activeCount = alerts.filter(a => !a.resolved).length;

  if (!isOpen) return null;

  return (
    <div className="alert-panel-backdrop" onClick={onClose}>
      <div className="alert-panel-drawer" onClick={e => e.stopPropagation()}>
        <div className="alert-drawer-header">
          <div className="drawer-title-group">
            <span className="drawer-icon">🚨</span>
            <div>
              <h3>Incidents & Safety Alerts</h3>
              <p className="drawer-subtitle">{activeCount} active incident{activeCount === 1 ? '' : 's'} requiring attention</p>
            </div>
          </div>
          <button className="close-btn" onClick={onClose}>✕</button>
        </div>

        <div className="alert-filter-bar">
          <button
            className={`filter-pill ${filterActive ? 'active' : ''}`}
            onClick={() => setFilterActive(true)}
          >
            Active Incidents ({activeCount})
          </button>
          <button
            className={`filter-pill ${!filterActive ? 'active' : ''}`}
            onClick={() => setFilterActive(false)}
          >
            All Incidents ({alerts.length})
          </button>

          {alerts.some(a => a.resolved) && (
            <button
              className="clear-resolved-btn"
              onClick={onClearResolvedAlerts}
              title="Clear resolved incident history"
            >
              Clear Resolved
            </button>
          )}
        </div>

        <div className="alert-list-container">
          {displayedAlerts.length === 0 ? (
            <div className="alert-empty-state">
              <div className="empty-icon">✅</div>
              <p className="empty-title">All Microgrid Systems Nominal</p>
              <p className="empty-desc">No active faults, thermal breaches, or critical load anomalies detected.</p>
            </div>
          ) : (
            displayedAlerts.map(alert => {
              const isCrit = alert.severity === 'CRITICAL';
              const isWarn = alert.severity === 'WARNING';
              const badgeClass = isCrit ? 'sev-crit' : isWarn ? 'sev-warn' : 'sev-info';

              return (
                <div key={alert.id} className={`alert-card ${alert.resolved ? 'resolved' : ''} ${badgeClass}`}>
                  <div className="alert-card-top">
                    <div className="alert-badge-group">
                      <span className={`severity-badge ${badgeClass}`}>{alert.severity}</span>
                      <span className="alert-type-tag">{alert.type?.replace(/_/g, ' ')}</span>
                      {alert.deviceId && <span className="alert-device-tag">Node: {alert.deviceId}</span>}
                    </div>
                    <span className="alert-time">
                      {alert.timestamp ? new Date(alert.timestamp).toLocaleTimeString() : ''}
                    </span>
                  </div>

                  <p className="alert-message">{alert.message}</p>

                  <div className="alert-card-actions">
                    {alert.resolved ? (
                      <span className="resolved-stamp">✓ Resolved {alert.resolvedAt ? new Date(alert.resolvedAt).toLocaleTimeString() : ''}</span>
                    ) : (
                      <button
                        className="resolve-incident-btn"
                        onClick={() => onResolveAlert(alert.id)}
                      >
                        Acknowledge & Resolve
                      </button>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}
