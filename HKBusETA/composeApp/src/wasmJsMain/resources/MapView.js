class WebMap {
    constructor(language, darkMode) {
        this.valid = true;
        this.mapElement = document.createElement("div");
        this.mapId = "map_" + Math.floor(Math.random() * Math.floor(1000000));
        this.mapElement.id = this.mapId;
        this.mapElement.style.display = "none";
        this.mapElement.style.position = "absolute";
        document.body.appendChild(this.mapElement);

        this.map = L.map(this.mapId).setView([22.32267, 144.17504], 13);

        this.tileLayers = L.layerGroup().addTo(this.map);
        setTimeout(() => this.reloadTiles(language, darkMode), 10);

        this.layer = L.layerGroup().addTo(this.map);
        this.polylines = [];
        this.polylinesOutline = [];

        this.resizeCallback = () => this.map.invalidateSize();
        window.addEventListener("resize", this.resizeCallback);
    }

    reloadTiles(language, darkMode) {
        this.tileLayers.clearLayers();
        L.tileLayer(darkMode ? 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/light_nolabels/{z}/{x}/{y}.png' : 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/rastertiles/voyager_nolabels/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a> &copy; <a href="https://api.portal.hkmapservice.gov.hk/disclaimer">HKSAR Gov</a>'
        }).addTo(this.tileLayers);
        L.tileLayer('https://mapapi.geodata.gov.hk/gs/api/v1.0.0/xyz/label/hk/{lang}/WGS84/{z}/{x}/{y}.png'.replace("{lang}", language === "en" ? "en" : "tc"), {
            maxZoom: 19,
        }).addTo(this.tileLayers);
    }

    remove() {
        this.valid = false;
        this.hide();
        window.removeEventListener("resize", this.resizeCallback);
        setTimeout(() => this.mapElement.remove(), 1000);
    }

    setMapPosition(x, y, width, height) {
        this.mapElement.style.left = x + "px";
        this.mapElement.style.top = y + "px";
        this.mapElement.style.width = width + "px";
        this.mapElement.style.height = height + "px";
    }

    show() {
        this.mapElement.style.display = "";
        this.map.invalidateSize();
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

    updateMarkings(stopsJsArray, stopNamesJsArray, pathsJsArray, colorHex, opacity, outlineHex, outlineOpacity, iconFile, anchorX, anchorY, selectStopCallback) {
        this.layer.clearLayers();

        var stopIcon = L.icon({
            iconUrl: iconFile,
            iconSize: [30, 30],
            iconAnchor: [anchorX * 30, anchorY * 30]
        });

        var stops = splitLatLngPairs(stopsJsArray);
        var stopNames = stopNamesJsArray.split('\0');

        stops.forEach((point, index) => {
            var clicked = false;
            var marker = L.marker(point, {icon: stopIcon})
                .addTo(this.layer)
                .bindPopup("<div style='text-align: center;'>" + stopNames[index] + "<div>", { offset: L.point(0, -22), closeButton: false })
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
        });

        var paths = splitLatLngPaths(pathsJsArray);

        this.polylines = [];
        paths.forEach(path => {
            this.polylinesOutline.push(L.polyline(path, { color: outlineHex, opacity: outlineOpacity, weight: 5 }).addTo(this.layer));
        });
        paths.forEach(path => {
            this.polylines.push(L.polyline(path, { color: colorHex, opacity: opacity, weight: 4 }).addTo(this.layer));
        });
    }

    mapFlyTo(lat, lng) {
        this.map.flyTo([lat.toString(), lng.toString()], 15, { animate: true, duration: 0.5 });
    }

    updateLineColor(colorHex, opacity, outlineHex, outlineOpacity) {
        if (this.polylines || this.polylinesOutline) {
            this.polylinesOutline.forEach(polyline => {
                polyline.setStyle({ color: outlineHex, opacity: outlineOpacity });
            });
            this.polylines.forEach(polyline => {
                polyline.setStyle({ color: colorHex, opacity: opacity });
            });
        }
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