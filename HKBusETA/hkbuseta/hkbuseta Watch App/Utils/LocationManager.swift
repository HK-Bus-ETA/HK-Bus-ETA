//
//  LocationManager.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 23/12/2023.
//

import CoreLocation
import shared

class SingleLocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    
    private let locationManager = CLLocationManager()
    @Published var location: CLLocation?
    @Published var isLocationFetched = false
    @Published var readyForRequest = false
    @Published var authorizationDenied = false

    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        let status = CLLocationManager().authorizationStatus
        if status == .authorizedWhenInUse || status == .authorizedAlways {
            readyForRequest = true
        } else if status == .denied {
            authorizationDenied = true
        }
    }
    
    deinit {
        locationManager.stopUpdatingLocation()
    }
    
    func requestPermission() {
        locationManager.requestWhenInUseAuthorization()
    }

    func requestLocation() {
        if readyForRequest {
            isLocationFetched = false
            locationManager.requestLocation()
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        location = locations.first
        isLocationFetched = true
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("Failed to get location: \(error)")
        isLocationFetched = true
    }

    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if status == .denied {
            authorizationDenied = true
            isLocationFetched = true
        } else if status == .authorizedWhenInUse || status == .authorizedAlways {
            readyForRequest = true
        }
    }
}

extension CLLocationCoordinate2D {
    
    func toLocationResult() -> LocationResult {
        return LocationResult.Companion().fromLatLng(lat: latitude, lng: longitude)
    }
    
}
