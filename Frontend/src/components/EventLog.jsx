import React from 'react';
import { Radio } from 'lucide-react';

export default function EventLog({ events }) {
  return (
    <div className="event-log-panel">
      <div className="panel-title">
        <Radio size={16} color="#06b6d4" />
        <span>Live State Machine Events</span>
        <span style={{ marginLeft: 'auto', fontSize: '0.7rem', color: '#64748b' }}>
          {events.length} events
        </span>
      </div>

      <div className="log-scroll-area">
        {events.length === 0 ? (
          <div style={{ color: '#64748b', fontSize: '0.78rem', fontStyle: 'italic', padding: '12px 0' }}>
            Listening for state machine transitions...
          </div>
        ) : (
          events.map((event, index) => {
            const timeStr = event.timestamp
              ? new Date(event.timestamp).toLocaleTimeString()
              : new Date().toLocaleTimeString();

            return (
              <div key={index} className={`log-card ${event.newState}`}>
                <div className="log-header">
                  <span className="log-device">{event.deviceId}</span>
                  <span className="log-time">{timeStr}</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.76rem' }}>
                  <span className={`state-pill ${event.previousState}`} style={{ fontSize: '0.65rem', padding: '1px 6px' }}>
                    {event.previousState}
                  </span>
                  <span style={{ color: '#64748b' }}>→</span>
                  <span className={`state-pill ${event.newState}`} style={{ fontSize: '0.65rem', padding: '1px 6px' }}>
                    {event.newState}
                  </span>
                </div>
                <div className="log-reason">{event.reason}</div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
