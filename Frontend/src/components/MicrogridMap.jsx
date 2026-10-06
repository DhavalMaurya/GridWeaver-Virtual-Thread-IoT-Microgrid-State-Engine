import React, { useEffect, useRef, useState, useMemo } from 'react';
import L from 'leaflet';
import { Filter, Layers, Zap, Sun, Battery } from 'lucide-react';
import DeviceInspector from './DeviceInspector';

export default function MicrogridMap({ devices = [], selectedDevice, onSelectDevice }) {
  const mapContainerRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markersLayerRef = useRef(null);

  // Map filters state
  const [filterType, setFilterType] = useState('ALL');
  const [filterState, setFilterState] = useState('ALL');
  const [filterZone, setFilterZone] = useState('ALL');
  const [showAllNodes, setShowAllNodes] = useState(false);

  // Dynamically derive available zones from registered devices
  const availableZones = useMemo(() => {
    const set = new Set();
    devices.forEach(d => {
      if (d.zoneId) set.add(d.zoneId);
    });
    return Array.from(set).sort();
  }, [devices]);

  // Filtered devices subset
  const filteredDevices = useMemo(() => {
    return devices.filter(device => {
      if (filterType !== 'ALL' && device.deviceType !== filterType) return false;
      if (filterZone !== 'ALL' && device.zoneId !== filterZone) return false;
      if (filterState !== 'ALL') {
        if (device.deviceType === 'SOLAR_PANEL') return false;
        if ((device.state || 'IDLE') !== filterState) return false;
      }
      return true;
    });
  }, [devices, filterType, filterZone, filterState]);

  // Spatial sampling for map visual clarity at 1,000 to 10,000 node scale
  const renderedDevices = useMemo(() => {
    const MAX_MAP_NODES = 300;
    if (showAllNodes || filteredDevices.length <= MAX_MAP_NODES) {
      return filteredDevices;
    }

    const priorityList = [];
    const regularList = [];

    filteredDevices.forEach(d => {
      if (d.state === 'FAULT' || (selectedDevice && selectedDevice.id === d.id)) {
        priorityList.push(d);
      } else {
        regularList.push(d);
      }
    });

    const budget = Math.max(0, MAX_MAP_NODES - priorityList.length);
    if (budget === 0) return priorityList;

    const step = Math.max(1, Math.floor(regularList.length / budget));
    const sampledRegular = [];
    for (let i = 0; i < regularList.length && sampledRegular.length < budget; i += step) {
      sampledRegular.push(regularList[i]);
    }

    return [...priorityList, ...sampledRegular];
  }, [filteredDevices, selectedDevice]);

  // Compute Zone statistics HUD
  const zoneStats = useMemo(() => {
    const targetDevices = filterZone === 'ALL'
      ? devices
      : devices.filter(d => d.zoneId === filterZone);

    let solarCount = 0;
    let batteryCount = 0;
    let totalSolarPower = 0;
    let totalBatteryPower = 0;

    targetDevices.forEach(d => {
      if (d.deviceType === 'SOLAR_PANEL') {
        solarCount++;
        totalSolarPower += (d.power || 0);
      } else {
        batteryCount++;
        totalBatteryPower += (d.power || 0);
      }
    });

    return {
      total: targetDevices.length,
      solarCount,
      batteryCount,
      totalSolarPower,
      totalBatteryPower
    };
  }, [devices, filterZone]);

  // Initialize Map
  useEffect(() => {
    if (!mapContainerRef.current || mapInstanceRef.current) return;

    const map = L.map(mapContainerRef.current, {
      center: [37.760, -122.380], // Bay Area regional view center
      zoom: 10,
      zoomControl: true,
      attributionControl: false
    });

    // 100% Free OpenStreetMap Tiles (Zero API key required, Zero watermarks)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      subdomains: 'abc'
    }).addTo(map);

    const markersLayer = L.layerGroup().addTo(map);
    markersLayerRef.current = markersLayer;
    mapInstanceRef.current = map;

    // Ensure Leaflet smoothly computes full canvas dimensions
    setTimeout(() => {
      map.invalidateSize();
    }, 100);

    const resizeObserver = new ResizeObserver(() => {
      map.invalidateSize();
    });
    if (mapContainerRef.current) {
      resizeObserver.observe(mapContainerRef.current);
    }

    return () => {
      resizeObserver.disconnect();
      map.remove();
      mapInstanceRef.current = null;
    };
  }, []);

  const prevZoneRef = useRef(filterZone);
  const prevSelectedDeviceIdRef = useRef(selectedDevice?.id || null);

  // Safety check: auto-reset zone filter if selected zone is not in available zones
  useEffect(() => {
    if (filterZone !== 'ALL' && availableZones.length > 0 && !availableZones.includes(filterZone)) {
      setFilterZone('ALL');
    }
  }, [availableZones, filterZone]);

  // Pan and zoom map when zone filter changes
  useEffect(() => {
    if (!mapInstanceRef.current) return;
    if (prevZoneRef.current === filterZone) return;
    prevZoneRef.current = filterZone;

    const zoneTargets = {
      'ZONE-1': { center: [37.950, -122.530], zoom: 11 }, // Marin
      'ZONE-2': { center: [37.775, -122.420], zoom: 12 }, // Central SF
      'ZONE-3': { center: [37.805, -122.250], zoom: 11 }, // East Bay
      'ZONE-4': { center: [37.520, -122.280], zoom: 11 }, // Peninsula
      'ZONE-5': { center: [37.335, -121.890], zoom: 11 }, // Silicon Valley / San Jose
      'ZONE-6': { center: [38.290, -122.450], zoom: 11 }, // Sonoma & Napa
      'ZONE-7': { center: [37.900, -122.060], zoom: 11 }, // Contra Costa
      'ZONE-8': { center: [36.974, -122.030], zoom: 11 }  // Santa Cruz
    };

    if (filterZone === 'ALL') {
      mapInstanceRef.current.flyTo([37.650, -122.200], 9, { duration: 0.8 });
    } else if (zoneTargets[filterZone]) {
      const target = zoneTargets[filterZone];
      mapInstanceRef.current.flyTo(target.center, target.zoom, { duration: 0.8 });
    } else {
      // Dynamic fallback for scaled zones: compute centroid from registered devices in that zone
      const zoneDevs = devices.filter(d => d.zoneId === filterZone);
      if (zoneDevs.length > 0) {
        const avgLat = zoneDevs.reduce((sum, d) => sum + d.latitude, 0) / zoneDevs.length;
        const avgLon = zoneDevs.reduce((sum, d) => sum + d.longitude, 0) / zoneDevs.length;
        mapInstanceRef.current.flyTo([avgLat, avgLon], 11, { duration: 0.8 });
      }
    }
  }, [filterZone, devices]);

  // Pan to selected device (only when a new device is clicked/selected)
  useEffect(() => {
    const currentId = selectedDevice?.id || null;
    if (currentId !== prevSelectedDeviceIdRef.current) {
      prevSelectedDeviceIdRef.current = currentId;
      if (selectedDevice && mapInstanceRef.current) {
        mapInstanceRef.current.flyTo([selectedDevice.latitude, selectedDevice.longitude], 14, {
          animate: true,
          duration: 0.6
        });
      }
    }
  }, [selectedDevice]);

  // Render Markers
  useEffect(() => {
    if (!markersLayerRef.current) return;

    markersLayerRef.current.clearLayers();

    renderedDevices.forEach(device => {
      const isSelected = selectedDevice && selectedDevice.id === device.id;
      const isSolar = device.deviceType === 'SOLAR_PANEL';

      let fillColor = '#64748b';
      let strokeColor = '#334155';

      if (isSolar) {
        fillColor = '#06b6d4'; // Cyan for solar
        strokeColor = isSelected ? '#ffffff' : '#0891b2';
      } else {
        const state = device.state || 'IDLE';
        if (state === 'IDLE') {
          fillColor = '#10b981'; // Green
          strokeColor = isSelected ? '#ffffff' : '#059669';
        } else if (state === 'CHARGING') {
          fillColor = '#f59e0b'; // Amber
          strokeColor = isSelected ? '#ffffff' : '#d97706';
        } else if (state === 'DISCHARGING') {
          fillColor = '#ef4444'; // Red
          strokeColor = isSelected ? '#ffffff' : '#dc2626';
        } else if (state === 'FAULT') {
          fillColor = '#dc2626'; // Deep red
          strokeColor = '#ffffff';
        }
      }

      const marker = L.circleMarker([device.latitude, device.longitude], {
        radius: isSelected ? 9 : (isSolar ? 6 : 7),
        fillColor: fillColor,
        color: isSelected ? '#ffffff' : strokeColor,
        weight: isSelected ? 2.5 : 1.2,
        opacity: 1,
        fillOpacity: isSolar ? 0.9 : 0.85
      });

      const power = device.power || 0;
      const powerStr = power >= 1000 ? `${(power / 1000).toFixed(2)} MW` : `${power.toFixed(2)} kW`;
      const stateStr = isSolar
        ? (power === 0 ? 'ZERO OUTPUT' : (power <= 0.8 ? 'STORM CLOUD' : 'GENERATING'))
        : `${device.state || 'IDLE'} (${(device.batteryPercentage || 0).toFixed(1)}% SoC)`;

      marker.bindTooltip(
        `<strong>${device.id}</strong><br/>Type: ${isSolar ? 'Solar PV' : 'BESS'}<br/>Status: ${stateStr}<br/>Zone: ${device.zoneId || 'ZONE-1'}<br/>Power: ${powerStr}`,
        { direction: 'top', offset: [0, -6] }
      );

      marker.on('click', () => {
        onSelectDevice(device);
      });

      markersLayerRef.current.addLayer(marker);
    });
  }, [renderedDevices, selectedDevice, onSelectDevice]);

  return (
    <div className="map-pane">
      <div ref={mapContainerRef} style={{ width: '100%', height: '100%' }} />

      {/* Advanced Filter Toolbar */}
      <div className="map-filter-toolbar">
        <div className="filter-group">
          <Layers size={14} className="filter-icon" />
          <span className="filter-label">ZONE:</span>
          <select
            className="zone-select"
            value={filterZone}
            onChange={e => setFilterZone(e.target.value)}
          >
            <option value="ALL">All Zones ({devices.length})</option>
            {availableZones.map(z => (
              <option key={z} value={z}>
                {z} ({devices.filter(d => d.zoneId === z).length})
              </option>
            ))}
          </select>
        </div>

        <div className="filter-divider"></div>

        <div className="filter-group">
          <span className="filter-label">TYPE:</span>
          <div className="type-buttons">
            <button
              className={`filter-btn ${filterType === 'ALL' ? 'active' : ''}`}
              onClick={() => setFilterType('ALL')}
            >
              All
            </button>
            <button
              className={`filter-btn ${filterType === 'BATTERY' ? 'active' : ''}`}
              onClick={() => setFilterType('BATTERY')}
            >
              🔋 Battery
            </button>
            <button
              className={`filter-btn ${filterType === 'SOLAR_PANEL' ? 'active' : ''}`}
              onClick={() => setFilterType('SOLAR_PANEL')}
            >
              ☀️ Solar
            </button>
          </div>
        </div>

        {filterType !== 'SOLAR_PANEL' && (
          <>
            <div className="filter-divider"></div>
            <div className="filter-group">
              <span className="filter-label">STATE:</span>
              <div className="state-buttons">
                {['ALL', 'IDLE', 'CHARGING', 'DISCHARGING', 'FAULT'].map(st => (
                  <button
                    key={st}
                    className={`filter-btn ${filterState === st ? 'active' : ''}`}
                    onClick={() => setFilterState(st)}
                  >
                    {st}
                  </button>
                ))}
              </div>
            </div>
          </>
        )}

        {filteredDevices.length > 300 && (
          <>
            <div className="filter-divider"></div>
            <button
              className={`sampling-badge-btn ${showAllNodes ? 'all-active' : ''}`}
              onClick={() => setShowAllNodes(!showAllNodes)}
              title={showAllNodes ? "Click to enable 300-node sampling for high map performance" : "Click to display all 10,000 nodes on the map"}
            >
              {showAllNodes
                ? `🌐 Showing ALL ${filteredDevices.length} Nodes`
                : `📍 Showing ${renderedDevices.length} / ${filteredDevices.length} Nodes (Click for ALL)`}
            </button>
          </>
        )}
      </div>

      {/* Floating Solar / Battery Device Inspector Overlay */}
      <DeviceInspector device={selectedDevice} onClose={() => onSelectDevice(null)} />

      {/* Zone Telemetry HUD (Bottom Right to prevent filter toolbar collision) */}
      <div className="zone-hud bottom-right">
        <div className="hud-title">
          <Zap size={13} />
          <span>{filterZone === 'ALL' ? 'MICROGRID GLOBAL VIEW' : `ZONE ${filterZone} DISPATCH`}</span>
        </div>
        <div className="hud-metrics">
          <div className="hud-item">
            <span className="hud-label">Nodes</span>
            <span className="hud-val">{zoneStats.total}</span>
          </div>
          <div className="hud-item">
            <span className="hud-label">Solar PV</span>
            <span className="hud-val text-cyan">
              {zoneStats.totalSolarPower >= 1000
                ? `${(zoneStats.totalSolarPower / 1000).toFixed(2)} MW`
                : `${zoneStats.totalSolarPower.toFixed(1)} kW`}
            </span>
          </div>
          <div className="hud-item">
            <span className="hud-label">BESS</span>
            <span className="hud-val text-amber">
              {zoneStats.totalBatteryPower >= 1000
                ? `${(zoneStats.totalBatteryPower / 1000).toFixed(2)} MW`
                : `${zoneStats.totalBatteryPower.toFixed(1)} kW`}
            </span>
          </div>
        </div>
      </div>

      {/* Map Legend */}
      <div className="map-legend">
        <div className="legend-item">
          <div className="legend-dot" style={{ background: '#10b981' }}></div>
          <span>IDLE</span>
        </div>
        <div className="legend-item">
          <div className="legend-dot" style={{ background: '#f59e0b' }}></div>
          <span>CHARGING</span>
        </div>
        <div className="legend-item">
          <div className="legend-dot" style={{ background: '#ef4444' }}></div>
          <span>DISCHARGING</span>
        </div>
        <div className="legend-item">
          <div className="legend-dot" style={{ background: '#dc2626' }}></div>
          <span>FAULT</span>
        </div>
        <div className="legend-item">
          <div className="legend-dot" style={{ background: '#06b6d4' }}></div>
          <span>SOLAR PV</span>
        </div>
      </div>
    </div>
  );
}
