class WebMap {
    constructor(language, darkMode, backgroundColor, sizeToggleCallback) {
        this.valid = true;
        this.tileUrlOverrideStorageKey = "hkbuseta.map.tileUrlOverride";
        this.tileUrlOverride = localStorage.getItem(this.tileUrlOverrideStorageKey) || "";
        this.lastTileLanguage = language;
        this.lastTileDarkMode = darkMode;
        this.lastTileBackgroundColor = backgroundColor;
        this.mapElement = document.createElement("div");
        this.mapId = "map_" + Math.floor(Math.random() * Math.floor(1000000));
        this.mapElement.id = this.mapId;
        this.mapElement.style.display = "none";
        this.mapElement.style.position = "absolute";
        this.mapElement.classList.add("prevent-select");
        document.body.appendChild(this.mapElement);

        this.map = L.map(this.mapId).setView([22.2906812,114.1732862], 13);

        this.tileLayers = L.layerGroup().addTo(this.map);
        setTimeout(() => this.reloadTiles(language, darkMode, backgroundColor), 10);

        this.layer = L.layerGroup().addTo(this.map);
        this.map.createPane('routeDirections');
        this.map.getPane('routeDirections').style.zIndex = 450;
        this.map.getPane('routeDirections').style.pointerEvents = 'none';
        this.polylinesList = [];
        this.polylinesOutlineList = [];
        this.routeArrowSections = [];
        this.routeArrowMarkers = [];
        this.routeArrowUpdateFrame = null;

        this.stopMarkersList = [];

        this.resizeCallback = () => {
            this.map.invalidateSize({ pan: false });
            this.scheduleRouteDirectionArrowUpdate();
        };
        window.addEventListener("resize", this.resizeCallback);
        this.routeArrowUpdateCallback = () => this.scheduleRouteDirectionArrowUpdate();
        this.map.on('moveend zoomend', this.routeArrowUpdateCallback);

        this.sizeToggleCallback = sizeToggleCallback;
        this.sizeToggleIsLarge = false;

        this.sizeToggleContainer = L.DomUtil.create('div', 'leaflet-bar leaflet-control');
        this.sizeToggleContainer.style.display = "none";
        this.sizeToggleButton = L.DomUtil.create('a', 'leaflet-size-toggle', this.sizeToggleContainer);
        this.sizeToggleButton.innerText = "↧";
        this.sizeToggleButton.href = '#';
        L.DomEvent.disableClickPropagation(this.sizeToggleContainer);
        L.DomEvent.on(this.sizeToggleButton, 'click', L.DomEvent.stopPropagation)
            .on(this.sizeToggleButton, 'click', L.DomEvent.preventDefault)
            .on(this.sizeToggleButton, 'click', () => {
                this.sizeToggleIsLarge = !this.sizeToggleIsLarge;
                this.sizeToggleButton.innerText = this.sizeToggleIsLarge ? "↥" : "↧";
                this.sizeToggleCallback(this.sizeToggleIsLarge);
            });
        const SizeToggleControl = L.Control.extend({
            options: { position: 'topleft' },
            onAdd: () => this.sizeToggleContainer
        });
        this.map.addControl(new SizeToggleControl());

        this.tileUrlControlContainer = L.DomUtil.create('div', 'leaflet-bar leaflet-control leaflet-tile-url-control');
        this.tileUrlButton = L.DomUtil.create('a', 'leaflet-tile-url-button', this.tileUrlControlContainer);
        this.tileUrlButton.innerHTML = '<svg class="leaflet-tile-url-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M20.5 3l-.16.03L15 5.1 9 3 3.36 4.9c-.21.07-.36.25-.36.48V20.5c0 .28.22.5.5.5l.16-.03L9 18.9l6 2.1 5.64-1.9c.21-.07.36-.25.36-.48V3.5c0-.28-.22-.5-.5-.5zM15 19l-6-2.11V5l6 2.11V19z"></path></svg>';
        this.tileUrlButton.href = '#';
        this.tileUrlPanel = L.DomUtil.create('div', 'leaflet-tile-url-panel', this.tileUrlControlContainer);
        this.tileUrlPanel.style.display = "none";
        this.tileUrlLabel = L.DomUtil.create('label', '', this.tileUrlPanel);
        this.tileUrlInput = L.DomUtil.create('input', '', this.tileUrlPanel);
        this.tileUrlInput.type = "url";
        this.tileUrlInput.inputMode = "url";
        this.tileUrlInput.placeholder = "https://.../{z}/{x}/{y}.png";
        this.tileUrlInput.value = this.tileUrlOverride;
        this.tileUrlActions = L.DomUtil.create('div', 'leaflet-tile-url-actions', this.tileUrlPanel);
        this.tileUrlApplyButton = L.DomUtil.create('button', '', this.tileUrlActions);
        this.tileUrlApplyButton.type = "button";
        this.tileUrlResetButton = L.DomUtil.create('button', '', this.tileUrlActions);
        this.tileUrlResetButton.type = "button";
        this.setTileUrlControlLanguage(language);
        L.DomEvent.disableClickPropagation(this.tileUrlControlContainer);
        L.DomEvent.disableScrollPropagation(this.tileUrlControlContainer);
        L.DomEvent.on(this.tileUrlButton, 'click', L.DomEvent.stopPropagation)
            .on(this.tileUrlButton, 'click', L.DomEvent.preventDefault)
            .on(this.tileUrlButton, 'click', () => this.toggleTileUrlPanel());
        L.DomEvent.on(this.tileUrlApplyButton, 'click', () => this.applyTileUrlOverride());
        L.DomEvent.on(this.tileUrlResetButton, 'click', () => this.resetTileUrlOverride());
        L.DomEvent.on(this.tileUrlInput, 'keydown', (event) => {
            if (event.key === "Enter") {
                this.applyTileUrlOverride();
            } else if (event.key === "Escape") {
                this.hideTileUrlPanel();
            }
        });
        const TileUrlControl = L.Control.extend({
            options: { position: 'topleft' },
            onAdd: () => this.tileUrlControlContainer
        });
        this.map.addControl(new TileUrlControl());
    }

    getMapElementId() {
        return this.mapElement.id;
    }

    setUseSizeToggleContainer(useSizeToggle, sizeToggleIsLarge) {
        this.sizeToggleIsLarge = sizeToggleIsLarge;
        if (useSizeToggle) {
            this.sizeToggleContainer.style.display = "";
        } else {
            this.sizeToggleContainer.style.display = "none";
        }
    }

    reloadTiles(language, darkMode, backgroundColor) {
        this.lastTileLanguage = language;
        this.lastTileDarkMode = darkMode;
        this.lastTileBackgroundColor = backgroundColor;
        this.setTileUrlControlLanguage(language);
        this.tileLayers.clearLayers();

        const alpha = (backgroundColor >> 24) & 0xFF;
        const red = (backgroundColor >> 16) & 0xFF;
        const green = (backgroundColor >> 8) & 0xFF;
        const blue = backgroundColor & 0xFF;
        const alphaCss = alpha / 255;
        this.mapElement.style.backgroundColor = "rgba(" + red + ", " + green + ", " + blue + ", " + alphaCss + ")";

        const customTileUrl = this.tileUrlOverride.length > 0;
        const defaultTileUrl = darkMode ? 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/light_nolabels/{z}/{x}/{y}.png?key=cb1_2hza_1_5548584f4b723493af41eb95' : 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/rastertiles/voyager_nolabels/{z}/{x}/{y}.png?key=cb1_2hza_1_5548584f4b723493af41eb95';
        L.tileLayer(this.tileUrlOverride || defaultTileUrl, {
            maxZoom: 19,
            attribution: customTileUrl ? '' : '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a> &copy; <a href="https://api.portal.hkmapservice.gov.hk/disclaimer">HKSAR Gov</a>'
        }).addTo(this.tileLayers);
        if (!customTileUrl) {
            L.tileLayer('https://mapapi.geodata.gov.hk/gs/api/v1.0.0/xyz/label/hk/{lang}/WGS84/{z}/{x}/{y}.png'.replace("{lang}", language === "en" ? "en" : "tc"), {
                maxZoom: 19,
            }).addTo(this.tileLayers);
        }

        const mapComponents = document.querySelectorAll('.leaflet-layer, .leaflet-control-zoom, .leaflet-control-attribution');
        if (darkMode) {
            mapComponents.forEach(element => element.classList.add('leaflet-dark-theme'));
            this.tileUrlControlContainer.classList.add('leaflet-dark-theme');
        } else {
            mapComponents.forEach(element => element.classList.remove('leaflet-dark-theme'));
            this.tileUrlControlContainer.classList.remove('leaflet-dark-theme');
        }
    }

    setTileUrlControlLanguage(language) {
        const text = language === "en" ? {
            title: "Override map tiles URL",
            label: "Map tiles URL",
            apply: "Apply",
            reset: "Reset"
        } : {
            title: "\u8986\u84cb\u5730\u5716\u5716\u78da\u7db2\u5740",
            label: "\u5730\u5716\u5716\u78da\u7db2\u5740",
            apply: "\u78ba\u8a8d",
            reset: "\u91cd\u8a2d"
        };
        this.tileUrlButton.title = text.title;
        this.tileUrlButton.setAttribute("aria-label", text.title);
        this.tileUrlLabel.innerText = text.label;
        this.tileUrlApplyButton.innerText = text.apply;
        this.tileUrlResetButton.innerText = text.reset;
    }

    toggleTileUrlPanel() {
        if (this.tileUrlPanel.style.display === "none") {
            this.tileUrlPanel.style.display = "";
            this.tileUrlInput.value = this.tileUrlOverride;
            this.tileUrlInput.focus();
            this.tileUrlInput.select();
        } else {
            this.hideTileUrlPanel();
        }
    }

    hideTileUrlPanel() {
        this.tileUrlPanel.style.display = "none";
    }

    applyTileUrlOverride() {
        this.tileUrlOverride = this.tileUrlInput.value.trim();
        if (this.tileUrlOverride) {
            localStorage.setItem(this.tileUrlOverrideStorageKey, this.tileUrlOverride);
        } else {
            localStorage.removeItem(this.tileUrlOverrideStorageKey);
        }
        this.hideTileUrlPanel();
        this.reloadTiles(this.lastTileLanguage, this.lastTileDarkMode, this.lastTileBackgroundColor);
    }

    resetTileUrlOverride() {
        this.tileUrlOverride = "";
        this.tileUrlInput.value = "";
        localStorage.removeItem(this.tileUrlOverrideStorageKey);
        this.hideTileUrlPanel();
        this.reloadTiles(this.lastTileLanguage, this.lastTileDarkMode, this.lastTileBackgroundColor);
    }

    remove() {
        this.valid = false;
        this.hide();
        if (this.routeArrowUpdateFrame !== null) cancelAnimationFrame(this.routeArrowUpdateFrame);
        this.map.off('moveend zoomend', this.routeArrowUpdateCallback);
        window.removeEventListener("resize", this.resizeCallback);
        setTimeout(() => this.mapElement.remove(), 1000);
    }

    setMapPosition(x, y, width, height) {
        this.mapElement.style.left = x + "px";
        this.mapElement.style.top = y + "px";
        this.mapElement.style.width = width + "px";
        this.mapElement.style.height = height + "px";
        this.map.invalidateSize({ pan: false });
        this.scheduleRouteDirectionArrowUpdate();
    }

    show() {
        this.mapElement.style.display = "";
        this.map.invalidateSize({ pan: false });
        this.scheduleRouteDirectionArrowUpdate();
        if (this.mapElement.style.opacity && Number(this.mapElement.style.opacity) < 1) {
            var fadeInEffect = setInterval(() => {
                if (this.mapElement.style.opacity < 1) {
                    this.mapElement.style.opacity = Number(this.mapElement.style.opacity) + 0.1;
                } else {
                   clearInterval(fadeInEffect);
                }
           }, 20);
        }
    }

    hide() {
       if (!this.mapElement.style.opacity) {
           this.mapElement.style.opacity = 1;
       }
       var fadeOutEffect = setInterval(() => {
           if (Number(this.mapElement.style.opacity) > 0) {
               this.mapElement.style.opacity = Number(this.mapElement.style.opacity) - 0.1;
           } else {
               clearInterval(fadeOutEffect);
               this.mapElement.style.display = "none";
           }
       }, 20);
    }

    startSelect(lat, lng, radius, onMoveCallback) {
        this.updateSelect(lat, lng, radius);
        this.map.flyTo([lat, lng], 15, {animate: false});

        var onMapMove = () => {
            var center = this.map.getCenter();
            var zoom = this.map.getZoom();
            onMoveCallback(center.lat, center.lng, zoom);
        }

        this.map.on('moveend', onMapMove);
    }

    flyToSelect(lat, lng) {
        this.map.flyTo([lat, lng], 15, { animate: true, duration: 0.5 });
    }

    updateSelect(lat, lng, radius) {
        this.layer.clearLayers();
        L.marker([lat, lng]).addTo(this.layer);
        L.circle([lat, lng], {
            color: '#199fff',
            fillColor: '#199fff',
            fillOpacity: 0.3,
            radius: radius
        }).addTo(this.layer);
    }

    clearMarkings() {
        this.layer.clearLayers();
        this.stopMarkersList = [];
        this.polylinesList = [];
        this.polylinesOutlineList = [];
        this.routeArrowSections = [];
        this.routeArrowMarkers = [];
    }

    addMarkings(stopsJsArray, stopNamesJsArray, pathsJsArray, colorHex, opacity, outlineHex, outlineOpacity, iconFile, anchorX, anchorY, indexMap, shouldShowStopIndex, selectStopCallback) {
        var stopIcon = L.icon({
            iconUrl: iconFile,
            iconSize: [30, 30],
            iconAnchor: [anchorX * 30, anchorY * 30]
        });

        var stops = splitLatLngPairs(stopsJsArray);
        var stopNames = stopNamesJsArray.split('\0');
        var indexMap = indexMap.split(',').map((s) => Number(s));

        var stopMarkers = stops.map((point, index) => {
            var clicked = false;
            var title;
            if (shouldShowStopIndex) {
                title = "<div style='text-align: center;'><b>" + (indexMap[index] + 1) + ". </b>" + stopNames[index] + "<div>";
            } else {
                title = "<div style='text-align: center;'>" + stopNames[index] + "<div>";
            }
            var marker = L.marker(point, {icon: stopIcon})
                .addTo(this.layer)
                .bindPopup(title, { offset: L.point(0, -22), closeButton: false })
                .on('click', () => {
                    selectStopCallback(index);
                    clicked = true;
                    marker.openPopup();
                });
                marker.on('mouseover', () => {
                    if (!marker.getPopup().isOpen()) {
                        clicked = false;
                        marker.openPopup();
                    }
                });
                marker.on('mouseout', () => {
                    if (!clicked) {
                        marker.closePopup();
                    }
                });
            return marker;
        });

        var paths = splitLatLngPaths(pathsJsArray);

        var polylines = [];
        var polylinesOutline = [];

        paths.forEach(path => {
            polylinesOutline.push(L.polyline(path, { color: outlineHex, opacity: outlineOpacity, weight: 5 }).addTo(this.layer));
        });
        paths.forEach(path => {
            polylines.push(L.polyline(path, { color: colorHex, opacity: opacity, weight: 4 }).addTo(this.layer));
        });

        this.routeArrowSections.push({ paths: paths, stops: stops, color: colorHex, outlineColor: outlineHex, outlineOpacity: outlineOpacity });

        this.stopMarkersList.push(stopMarkers);
        this.polylinesList.push(polylines);
        this.polylinesOutlineList.push(polylinesOutline);
        this.scheduleRouteDirectionArrowUpdate();
    }

    scheduleRouteDirectionArrowUpdate() {
        if (this.routeArrowUpdateFrame !== null) return;
        this.routeArrowUpdateFrame = requestAnimationFrame(() => {
            this.routeArrowUpdateFrame = requestAnimationFrame(() => {
                this.routeArrowUpdateFrame = null;
                if (this.valid) this.updateRouteDirectionArrows();
            });
        });
    }

    updateRouteDirectionArrows() {
        const size = this.map.getSize();
        if (size.x <= 0 || size.y <= 0 || this.mapElement.style.display === "none") return;
        this.routeArrowMarkers.forEach(marker => this.layer.removeLayer(marker));
        this.routeArrowMarkers = [];
        const occupied = [];
        const allStops = [];
        this.routeArrowSections.forEach(section => {
            section.stops.forEach(stop => allStops.push(this.map.latLngToContainerPoint(stop)));
        });

        this.routeArrowSections.forEach(section => {
            const segments = [];
            let totalLength = 0;
            section.paths.forEach(path => {
                for (let i = 1; i < path.length; i++) {
                    const startPoint = this.map.latLngToContainerPoint(path[i - 1]);
                    const endPoint = this.map.latLngToContainerPoint(path[i]);
                    const length = startPoint.distanceTo(endPoint);
                    if (Number.isFinite(length) && length > 0) {
                        segments.push({ start: path[i - 1], end: path[i], startPoint, endPoint, length });
                        totalLength += length;
                    }
                }
            });
            if (totalLength < 40) return;

            let traversed = 0;
            let nextDistance = totalLength < 96 ? totalLength / 2 : 48;
            const lastDistance = totalLength < 96 ? nextDistance : totalLength - 48;
            let visibleCount = 0;
            let opposingPhaseShifted = false;
            for (let s = 0; s < segments.length && visibleCount < 24; s++) {
                const segment = segments[s];
                const segmentEnd = traversed + segment.length;
                while (nextDistance <= segmentEnd && nextDistance <= lastDistance && visibleCount < 24) {
                    const fraction = Math.max(0, Math.min(1, (nextDistance - traversed) / segment.length));
                    const x = segment.startPoint.x + (segment.endPoint.x - segment.startPoint.x) * fraction;
                    const y = segment.startPoint.y + (segment.endPoint.y - segment.startPoint.y) * fraction;
                    const rotation = (Math.atan2(segment.endPoint.y - segment.startPoint.y, segment.endPoint.x - segment.startPoint.x) * 180 / Math.PI + 450) % 360;
                    const conflicts = occupied.map(arrow => ({ arrow, distance: arrow.point.distanceTo([x, y]) })).filter(conflict => conflict.distance < 48);
                    const hardOpposingOverlap = conflicts.some(conflict => angleDifference(conflict.arrow.rotation, rotation) >= 120 && conflict.distance < 14);
                    if (hardOpposingOverlap && !opposingPhaseShifted && nextDistance + 48 <= lastDistance) {
                        nextDistance += 48;
                        opposingPhaseShifted = true;
                        continue;
                    }
                    const clearOfStops = allStops.every(point => point.distanceTo([x, y]) >= 24);
                    const clearOfArrows = conflicts.every(conflict => angleDifference(conflict.arrow.rotation, rotation) >= 120 && conflict.distance >= 14);
                    if (x >= 0 && y >= 0 && x <= size.x && y <= size.y && clearOfStops && clearOfArrows) {
                        const location = [
                            segment.start[0] + (segment.end[0] - segment.start[0]) * fraction,
                            segment.start[1] + (segment.end[1] - segment.start[1]) * fraction
                        ];
                        const icon = L.divIcon({
                            className: 'route-direction-arrow',
                            iconSize: [14, 14],
                            iconAnchor: [7, 7],
                            html: '<svg width="14" height="14" viewBox="0 0 12 12" style="transform:rotate(' + rotation + 'deg)"><path d="M6 0.8 L10.8 11.2 L6 8.9 L1.2 11.2 Z" fill="' + section.color + '" stroke="' + (section.outlineColor || section.color) + '" stroke-opacity="' + (section.outlineColor ? section.outlineOpacity : 1) + '" stroke-width="0.8" stroke-linejoin="round" paint-order="stroke fill"/></svg>'
                        });
                        this.routeArrowMarkers.push(L.marker(location, { icon, pane: 'routeDirections', interactive: false, keyboard: false }).addTo(this.layer));
                        occupied.push({ point: L.point(x, y), rotation });
                        visibleCount++;
                    }
                    nextDistance += 96;
                }
                traversed = segmentEnd;
            }
        });
    }

    mapFlyTo(lat, lng) {
        this.map.flyTo([lat.toString(), lng.toString()], 15, { animate: true, duration: 0.5 });
    }

    updateLineColor(sectionIndex, colorHex, opacity, outlineHex, outlineOpacity) {
        if (this.polylinesList[sectionIndex] || this.polylinesOutlineList[sectionIndex]) {
            this.polylinesOutlineList[sectionIndex].forEach(polyline => {
                polyline.setStyle({ color: outlineHex, opacity: outlineOpacity });
            });
            this.polylinesList[sectionIndex].forEach(polyline => {
                polyline.setStyle({ color: colorHex, opacity: opacity });
            });
        }
        if (this.routeArrowSections[sectionIndex]) {
            this.routeArrowSections[sectionIndex].color = colorHex;
            this.routeArrowSections[sectionIndex].outlineColor = outlineHex;
            this.routeArrowSections[sectionIndex].outlineOpacity = outlineOpacity;
            this.scheduleRouteDirectionArrowUpdate();
        }
    }

    showMarker(sectionIndex, stopIndex) {
        this.stopMarkersList[sectionIndex][stopIndex].openPopup()
    }
}

function splitLatLngPairs(str) {
    const parts = str.split('\0');
    const result = [];
    for (let i = 0; i < parts.length; i += 2) {
        result.push([Number(parts[i]), Number(parts[i + 1])]);
    }
    return result;
}

function angleDifference(first, second) {
    const difference = Math.abs(first - second) % 360;
    return Math.min(difference, 360 - difference);
}

function splitLatLngPaths(str) {
    const groups = str.split('\0');
    const result = groups.map(group => {
        const numbers = group.split('|').map(Number);
        const pairs = [];
        for (let i = 0; i < numbers.length; i += 2) {
            pairs.push([numbers[i], numbers[i + 1]]);
        }
        return pairs;
    });
    return result;
}
