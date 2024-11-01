//
//  AppIntent.swift
//  FavouriteRoutesWidgetWatchExtensionExtension
//
//  Created by LOOHP on 1/11/2024.
//

import WidgetKit
import AppIntents

struct ConfigurationAppIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "configuration.title"
    static var description = IntentDescription("configuration.description")

    @Parameter(title: "selected.route")
    var selectedRoute: FavouriteRoute?
}

struct ColorInfoString {
    let string: String
    let color: Int64?
}

struct FavouriteRoute: AppEntity {
    typealias DefaultQuery = FavouriteRouteQuery
    
    let id: Int
    let groupName: String
    let routeNumber: String
    let co: String
    let coDisplay: [ColorInfoString]
    let prependTo: String?
    let dest: String
    let coSpecialRemark: String?
    let secondLine: String?
    let deeplink: String
    let useRelativeTime: Bool
    let precomputedData: String

    static var typeDisplayRepresentation: TypeDisplayRepresentation = "type.display.representation"
    static var defaultQuery = FavouriteRouteQuery()
    
    var displayName: String {
        "\(groupName) - \(routeNumber) \(prependTo ?? "")\(dest)\n\(secondLine ?? "*")"
    }
            
    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(groupName) - \(routeNumber) \(prependTo ?? "")\(dest)\n\(secondLine ?? "*")")
    }
}

struct FavouriteRouteQuery: EntityQuery {
    func entities(for identifiers: [FavouriteRoute.ID]) async -> [FavouriteRoute] {
        favouriteRouteGroupNames().map { favouriteRouteGroupEntries($0) }.filter { $0 != nil }.flatMap { $0! }
    }
    
    func suggestedEntities() async -> [FavouriteRoute] {
        favouriteRouteGroupNames().map { favouriteRouteGroupEntries($0) }.filter { $0 != nil }.flatMap { $0! }
    }
    
    func defaultResult() async -> FavouriteRoute? {
        await suggestedEntities().first
    }
}
