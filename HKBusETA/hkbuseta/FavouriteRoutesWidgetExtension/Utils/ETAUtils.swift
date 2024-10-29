//
//  ETAUtils.swift
//  FavouriteRoutesWidgetExtensionExtension
//
//  Created by LOOHP on 31/10/2024.
//

import Foundation

func fetchETALines() -> [String] {
    
}

func etaQueryKmb(rawStopId: String, stopIndex: Int, co: Operator, route: Route, context: AppContext) async throws -> [String] {
    let allStops = await getAllStops(routeNumber: route.routeNumber, bound: route.idBound(co: co), operator: co, region: route.gmbRegion)
    guard let stopData = allStops.first(where: { $0.stopId == rawStopId }) else {
        throw SomeError.stopNotFound
    }
    
    let sameStops = allStops.filter {
        $0.stop.name == stopData.stop.name && $0.stop.location.distance(to: stopData.stop.location) < 0.1
    }.unique(by: \.branchIds, equalityPredicate: { a, b in
        !a.intersection(b).isEmpty
    }).group(by: \.stopId)
    
    let stopIds: [(String, String)]
    if sameStops.count > 1 {
        stopIds = sameStops.map { (key, value) in
            let branchRemark = key == rawStopId ? "" : value.flatMap { $0.branchIds }
                .compactMap { $0.resolveSpecialRemark(context: context)[Shared.language] }
                .filter { !$0.isEmpty }
                .joined(separator: "/")
            return (key, branchRemark)
        }
    } else {
        stopIds = [(rawStopId, "")]
    }
    
    var lines = [Int: ETALineEntry]()
    let isMtrEndOfLine = false
    let language = Shared.language
    
    lines[1] = ETALineEntry.textEntry(
        getNoScheduledDepartureMessage(language: language, typhoonTitle: typhoonInfo.typhoonWarningTitle)
    )
    
    let isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
    var unsortedLines = [ETALineEntry]()
    var suspendedMessage: ETALineEntry?
    
    for (stopId, branchRemark) in stopIds {
        let isSpecial = !branchRemark.isEmpty
        guard let data = try? await fetchJSON(url: URL(string: "https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/\(stopId)")!) else {
            continue
        }
        let buses = data["data"] as? [[String: Any]] ?? []
        
        var stopSequences = Set<Int>()
        for bus in buses {
            if let operatorString = bus["co"] as? String, let co = Operator(rawValue: operatorString), co == .KMB {
                if bus["route"] as? String == route.routeNumber, bus["dir"] as? String == route.bound[.KMB] {
                    stopSequences.insert(bus["seq"] as? Int ?? -1)
                }
            }
        }
        
        let matchingSeq = stopSequences.min(by: { abs($0 - stopIndex) < abs($1 - stopIndex) }) ?? -1
        var usedRealSeq = Set<Int>()
        
        for bus in buses {
            guard let co = Operator(rawValue: bus["co"] as? String ?? ""), co == .KMB else { continue }
            let routeNumber = bus["route"] as? String ?? ""
            let bound = bus["dir"] as? String ?? ""
            let stopSeq = bus["seq"] as? Int ?? -1
            
            if routeNumber == route.routeNumber, bound == route.bound[.KMB], stopSeq == matchingSeq, usedRealSeq.insert(bus["eta_seq"] as? Int ?? -1).inserted {
                let eta = bus["eta"] as? String ?? ""
                let mins = eta.isEmpty || eta.lowercased() == "null" ? Double.negativeInfinity : (eta.parseInstant().timeIntervalSince1970 - Date().timeIntervalSince1970) / 60
                
                if mins.isFinite, mins < -10 { continue }
                
                let minsRounded = Int(round(mins))
                var timeMessage = ""
                var remarkMessage = ""
                
                if language == "en" {
                    if minsRounded > 0 {
                        timeMessage = "\(minsRounded) Min."
                    } else if minsRounded > -60 {
                        timeMessage = "- Min."
                    }
                    if let rmkEn = bus["rmk_en"] as? String, !rmkEn.isEmpty {
                        remarkMessage = branchRemark.isEmpty ? rmkEn : "\(branchRemark) (\(rmkEn))"
                    } else if !branchRemark.isEmpty {
                        remarkMessage = branchRemark
                    }
                } else {
                    if minsRounded > 0 {
                        timeMessage = "\(minsRounded) 分鐘"
                    } else if minsRounded > -60 {
                        timeMessage = "- 分鐘"
                    }
                    if let rmkTc = bus["rmk_tc"] as? String, !rmkTc.isEmpty {
                        remarkMessage = branchRemark.isEmpty ? rmkTc : "\(branchRemark) (\(rmkTc))"
                    } else if !branchRemark.isEmpty {
                        remarkMessage = branchRemark
                    }
                }
                
                if (timeMessage.isEmpty && remarkMessage.isEmpty) || typhoonInfo.isAboveTyphoonSignalEight && (remarkMessage == "ETA service suspended" || remarkMessage == "暫停預報") {
                    if !isSpecial {
                        remarkMessage = getNoScheduledDepartureMessage(language: language, remark: remarkMessage, typhoonTitle: typhoonInfo.typhoonWarningTitle)
                        suspendedMessage = ETALineEntry.etaEntry(timeMessage: timeMessage, remarkMessage: remarkMessage, routeNumber: routeNumber, eta: mins, etaRounded: minsRounded)
                    }
                } else {
                    unsortedLines.append(ETALineEntry.etaEntry(timeMessage: timeMessage, remarkMessage: remarkMessage, routeNumber: routeNumber, eta: mins, etaRounded: minsRounded))
                }
            }
        }
    }
    
    if unsortedLines.isEmpty {
        if let suspendedMessage = suspendedMessage {
            lines[1] = suspendedMessage
        }
    } else {
        unsortedLines.sorted { $0.eta < $1.eta }.enumerated().forEach { index, entry in
            lines[index + 1] = entry
        }
    }
    
    return ETAQueryResult(isEndOfLine: isMtrEndOfLine, isTyphoonSchedule: isTyphoonSchedule, operator: co, lines: lines)
}
