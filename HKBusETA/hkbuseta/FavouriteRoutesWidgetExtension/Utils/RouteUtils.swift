//
//  Utils.swift
//  HKBusETA Phone App
//
//  Created by LOOHP on 18/4/2024.
//

import Foundation


func readJSONFile(forName name: String) -> [String: Any] {
    return autoreleasepool {
        do {
            let dir = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: "group.com.loohp.hkbuseta")!
            let fileUrl = dir.appendingPathComponent(name)
            let text = try String(contentsOf: fileUrl)
            let jsonData = text.data(using: .utf8)!
            if let json = try JSONSerialization.jsonObject(with: jsonData, options: .mutableLeaves) as? [String: Any] {
                return json
            }
        } catch {
            print(error)
        }
        return [:]
    }
}

func isNewInstall() -> Bool {
    return autoreleasepool {
        return readJSONFile(forName: "preferences.json").isEmpty
    }
}

func favouriteRouteGroupNames() -> [String] {
    return autoreleasepool {
        let preferences = readJSONFile(forName: "preferences.json")
        if preferences.isEmpty {
            return []
        }
        let language = preferences["language"] as! String
        let favouriteRouteStops = preferences["favouriteRouteStops"] as! [[String : Any]]
        return favouriteRouteStops.map { ($0["name"] as! [String: String])[language]! }
    }
}

func favouriteRouteGroupEntries(_ name: String) -> [FavouriteRoute]? {
    return autoreleasepool {
        let preferences = readJSONFile(forName: "preferences.json")
        if preferences.isEmpty {
            return nil
        }
        let useRelativeTime = preferences["etaDisplayMode"] as? String == "COUNTDOWN"
        let favouriteRouteStops = preferences["favouriteRouteStops"] as! [[String : Any]]
        let favouriteGroup = favouriteRouteStops.first { ($0["name"] as! [String: String]).values.contains { $0 == name } }
        if favouriteGroup == nil {
            return nil
        }
        let entries = favouriteGroup!["favouriteRouteStops"] as! [[String : Any]]
        return entries.filter { $0.contains { $0.key == "platformDisplayInfo" } }.map {
            let id = $0["favouriteId"] as! Int
            let display = $0["platformDisplayInfo"] as! [String: Any]
            let routeNumber = display["routeNumber"] as! String
            let co = display["co"] as! String
            let coDisplay: [ColorInfoString] = (display["coDisplay"] as! [[String: Any]]).map {
                let string = $0["string"] as! String
                let color = $0["color"] as? Int64
                return ColorInfoString(string: string, color: color)
            }
            let prependTo = display["prependTo"] as? String
            let dest = display["dest"] as! String
            let coSpecialRemark = display["coSpecialRemark"] as? String
            let secondLine = display["secondLine"] as? String
            let deeplink = display["deeplink"] as! String
            let precomputedData = display["precomputedData"] as! String
            return FavouriteRoute(id: id, groupName: name, routeNumber: routeNumber, co: co, coDisplay: coDisplay, prependTo: prependTo, dest: dest, coSpecialRemark: coSpecialRemark, secondLine: secondLine, deeplink: deeplink, useRelativeTime: useRelativeTime, precomputedData: precomputedData)
        }
    }
}
