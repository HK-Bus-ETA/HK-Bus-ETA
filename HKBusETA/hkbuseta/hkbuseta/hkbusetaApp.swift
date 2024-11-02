//
//  hkbusetaApp.swift
//  hkbuseta
//
//  Created by LOOHP on 20/01/2024.
//

import SwiftUI
import ActivityKit
import UniformTypeIdentifiers
import WatchConnectivity
import ComposeApp
import Gzip
import FirebaseCore
import FirebaseAnalytics
import UserNotifications
import BackgroundTasks
import LinkPresentation
import WidgetKit

class ApplicationDelegate: NSObject, UIApplicationDelegate, WCSessionDelegate {

    override init() {
        super.init()
        if WCSession.isSupported() {
            let session = WCSession.default
            session.delegate = self
            session.activate()
        }
    }

    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        if let shortcutItem = options.shortcutItem {
            quickActionLaunchData.url = shortcutItem.userInfo?["url"] as? String
        }
        let configuration = UISceneConfiguration(name: connectingSceneSession.configuration.name, sessionRole: connectingSceneSession.role)
        configuration.delegateClass = SceneDelegate.self
        return configuration
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        FirebaseApp.configure()

        BGTaskScheduler.shared.register(forTaskWithIdentifier: "com.loohp.hkbuseta.dailyrefresh", using: nil) { task in
             handleAppRefresh(task: task as! BGProcessingTask)
        }
        scheduleAppRefresh()

        if let shortcutItem = launchOptions?[UIApplication.LaunchOptionsKey.shortcutItem] as? UIApplicationShortcutItem {
            quickActionLaunchData.url = shortcutItem.userInfo?["url"] as? String
        }

        return true
    }

    func applicationWillTerminate(_ application: UIApplication) {
        let semaphore = DispatchSemaphore(value: 0)
        Task.detached {
            if #available(iOS 16.2, *) {
                if alightReminderActivity != nil {
                    let state = AlightReminderLiveActivityAttributes.ContentState(routeNumber: ":(", stopsRemaining: "X", titleLeading: ":(", titleTrailing: "香港巴士到站預報被終止", content: "HK Bus ETA was terminated", color: 0xFFFF4747, url: "https://app.hkbuseta.com")
                    await alightReminderActivity!.end(using: state, dismissalPolicy: .default)
                }
            }
            semaphore.signal()
        }
        semaphore.wait()
    }

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {

    }

    func sessionDidBecomeInactive(_ session: WCSession) {

    }

    func sessionDidDeactivate(_ session: WCSession) {

    }

    func session(_ session: WCSession, didReceiveMessage payload: [String: Any]) {
        handleDataFromWatch(payload: payload)
    }

    func session(_ session: WCSession, didReceiveMessage payload: [String : Any], replyHandler: @escaping ([String : Any]) -> Void) {
        handleDataFromWatch(payload: payload)
    }

    func session(_ session: WCSession, didReceiveApplicationContext payload: [String : Any]) {
        handleDataFromWatch(payload: payload)
    }

}

func scheduleAppRefresh(time: Int64? = nil) {
    let request = BGProcessingTaskRequest(identifier: "com.loohp.hkbuseta.dailyrefresh")
    request.requiresNetworkConnectivity = true
    let updateTime = time ?? AppContextCompose_iosKt.nextScheduledDataUpdateMillis()
    request.earliestBeginDate = Date(timeIntervalSince1970: TimeInterval(updateTime))
    if time == nil {
        BGTaskScheduler.shared.getPendingTaskRequests { tasks in
            if !tasks.contains(where: { $0.identifier == "com.loohp.hkbuseta.dailyrefresh" }) {
                do {
                    try BGTaskScheduler.shared.submit(request)
                } catch {
                    print("Could not schedule app refresh: \(error)")
                }
            }
        }
    } else {
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("Could not schedule app refresh: \(error)")
        }
    }
}

func handleAppRefresh(task: BGProcessingTask) {
    scheduleAppRefresh()
    AppContextCompose_iosKt.runDailyUpdate {
        task.setTaskCompleted(success: true)
    }
}

class QuickActionLaunchData: ObservableObject {

    @Published var url: String?

}

let quickActionLaunchData = QuickActionLaunchData()

class SceneDelegate: NSObject, UIWindowSceneDelegate {

    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        if let shortcutItem = connectionOptions.shortcutItem {
            quickActionLaunchData.url = shortcutItem.userInfo?["url"] as? String
        }
    }

    func windowScene(_ windowScene: UIWindowScene, performActionFor shortcutItem: UIApplicationShortcutItem, completionHandler: @escaping (Bool) -> Void) {
        quickActionLaunchData.url = shortcutItem.userInfo?["url"] as? String
        completionHandler(true)
    }
}

struct TextDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.text] }

    var text: String

    init(text: String) {
        self.text = text
    }

    init(configuration: ReadConfiguration) throws {
        if let data = configuration.file.regularFileContents,
           let string = String(data: data, encoding: .utf8) {
            text = string
        } else {
            text = ""
        }
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        let data = text.data(using: .utf8)!
        return .init(regularFileWithContents: data)
    }
}

class FileImportExportData: ObservableObject {

    @Published var importingFile: Bool = false
    @Published var importingFileCallback: ((String) -> KotlinUnit)? = nil

    @Published var exportingFile: Bool = false
    @Published var exportingFileName: String? = nil
    @Published var exportingFileDocument: TextDocument = TextDocument(text: "")
    @Published var exportingFileCallback: (() -> KotlinUnit)? = nil

}

struct ShareUrlView: UIViewControllerRepresentable {

    let url: String
    let title: String?

    init(url: String, title: String?) {
        self.url = url
        self.title = title
    }

    func makeUIViewController(context: UIViewControllerRepresentableContext<ShareUrlView>) -> UIActivityViewController {
        return UIActivityViewController(activityItems: [ShareUrlItemSource(url: url, title: title)], applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: UIViewControllerRepresentableContext<ShareUrlView>) { /* do nothing */ }
}

class ShareUrlItemSource: NSObject, UIActivityItemSource {

    let url: String
    let title: String?

    init(url: String, title: String?) {
        self.url = url
        self.title = title
        super.init()
    }

    func activityViewControllerPlaceholderItem(_ activityViewController: UIActivityViewController) -> Any {
        return url
    }

    func activityViewController(_ activityViewController: UIActivityViewController, itemForActivityType activityType: UIActivity.ActivityType?) -> Any? {
        return url
    }

    func activityViewController(_ activityViewController: UIActivityViewController, subjectForActivityType activityType: UIActivity.ActivityType?) -> String {
        return title ?? ""
    }

    func activityViewControllerLinkMetadata(_ activityViewController: UIActivityViewController) -> LPLinkMetadata? {
        let metadata = LPLinkMetadata()
        metadata.title = title
        metadata.iconProvider = NSItemProvider(object: UIImage(systemName: "text.bubble")!)
        metadata.originalURL = URL(string: url)
        return metadata
    }

}

class ShareUrlData: ObservableObject, Identifiable {

    @Published var url: String = ""
    @Published var title: String? = nil

}

struct AlightReminderLiveActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        let routeNumber: String
        let stopsRemaining: String
        let titleLeading: String
        let titleTrailing: String
        let content: String
        let color: Int64
        let url: String
    }
}

@available(iOS 16.1, *)
var alightReminderActivity: Activity<AlightReminderLiveActivityAttributes>? = nil
var alightReminderLastState: Int32 = -1
var alightReminderLastRemote: String? = nil

@main
struct hkbusetaApp: App {

    @UIApplicationDelegateAdaptor(ApplicationDelegate.self) var delegate

    @ObservedObject private var fileImportExport = FileImportExportData()
    @ObservedObject private var quickActionLaunch = quickActionLaunchData
    @ObservedObject private var shareUrlData = ShareUrlData()
    @State private var shareUrlDataShowing = false

    init() {
        initImpl()
    }

    private func initImpl() {
        AppContextCompose_iosKt.setTilesUpdateImpl {
            DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                WidgetCenter.shared.reloadAllTimelines()
            }
        }
        AppContextCompose_iosKt.provideBackgroundUpdateScheduler { c, t in
            scheduleAppRefresh(time: t.int64Value)
        }
        AppContextCompose_iosKt.setGzipBodyAsTextImpl(impl: { data, charset in
            let decompressedData: Data = data.isGzipped ? try! data.gunzipped() : data
            return String(data: decompressedData, encoding: encoding(from: charset))!
        })
        AppContextCompose_iosKt.setFileChooserImportImpl { callback in
            fileImportExport.importingFileCallback = callback
            fileImportExport.importingFile = true
        }
        AppContextCompose_iosKt.setFileChooserExportImpl { fileName, file, callback in
            fileImportExport.exportingFileName = fileName
            fileImportExport.exportingFileDocument = TextDocument(text: file)
            fileImportExport.exportingFileCallback = callback
            fileImportExport.exportingFile = true
        }
        AppContextCompose_iosKt.setFirebaseLogImpl { eventName, appBundle in
            var dict: [String: String] = [:]
            for (key, value) in appBundle.data {
                dict[String(key as! NSString)] = String(value as! NSString)
            }
            Analytics.logEvent(eventName, parameters: dict)
        }
        AppContextCompose_iosKt.setSyncPreferencesImpl { preferenceJson in
            do {
                let payload = [
                    "messageType": AppContextCompose_iosKt.SYNC_PREFERENCES_ID,
                    "payload": preferenceJson
                ]
                if WCSession.default.isReachable {
                    WCSession.default.sendMessage(payload) { _ in }
                } else {
                    try WCSession.default.updateApplicationContext(payload)
                }
            } catch {
            }
        }
        AppContextCompose_iosKt.setShareUrlDataImpl { data in
            shareUrlData.url = data.url
            shareUrlData.title = data.title
        }
        AppContextCompose_iosKt.setAlightReminderHandler { data, remote in
            let payload = [
                "messageType": alightReminderLastRemote == nil ? AppContextCompose_iosKt.RESPONSE_ALIGHT_REMINDER_ID : AppContextCompose_iosKt.UPDATE_ALIGHT_REMINDER_ID,
                "payload": remote ?? ""
            ]
            if WCSession.default.isReachable {
                WCSession.default.sendMessage(payload) { _ in }
            }
            alightReminderLastRemote = remote
            if data == nil || data?.active != 0 {
                if #available(iOS 16.2, *) {
                    if alightReminderActivity != nil {
                        let killed = data!.active == 2
                        if data != nil {
                            let state = AlightReminderLiveActivityAttributes.ContentState(routeNumber: data!.routeNumber, stopsRemaining: data!.stopsRemaining, titleLeading: data!.titleLeading, titleTrailing: data!.titleTrailing, content: data!.content, color: data!.color, url: data!.url)
                            let alertConfig = data!.state == alightReminderLastState || killed ? nil : AlertConfiguration(
                                title: "\(data!.titleLeading) - \(data!.titleTrailing)",
                                body: "\(data!.content)",
                                sound: .default
                            )
                            Task {
                                await alightReminderActivity!.update(using: state, alertConfiguration: alertConfig)
                                await alightReminderActivity!.end(dismissalPolicy: killed ? .immediate : .default)
                                alightReminderActivity = nil
                            }
                        } else {
                            Task {
                                await alightReminderActivity!.end(dismissalPolicy: killed ? .immediate : .default)
                                alightReminderActivity = nil
                            }
                        }
                    }
                }
                alightReminderLastState = -1
            } else {
                if #available(iOS 16.2, *) {
                    if ActivityAuthorizationInfo().areActivitiesEnabled {
                        if alightReminderActivity == nil {
                            do {
                                let initialState = AlightReminderLiveActivityAttributes.ContentState(routeNumber: data!.routeNumber, stopsRemaining: data!.stopsRemaining, titleLeading: data!.titleLeading, titleTrailing: data!.titleTrailing, content: data!.content, color: data!.color, url: data!.url)

                                alightReminderActivity = try Activity.request(
                                    attributes: AlightReminderLiveActivityAttributes(),
                                    content: .init(state: initialState, staleDate: nil),
                                    pushType: nil)

                                alightReminderLastState = data!.state

                                if data!.state == 2 {
                                    Task { await alightReminderActivity?.end(dismissalPolicy: .default) }
                                }
                            } catch {
                                print(error)
                            }
                        } else {
                            let state = AlightReminderLiveActivityAttributes.ContentState(routeNumber: data!.routeNumber, stopsRemaining: data!.stopsRemaining, titleLeading: data!.titleLeading, titleTrailing: data!.titleTrailing, content: data!.content, color: data!.color, url: data!.url)
                            let alertConfig = data!.state == alightReminderLastState ? nil : AlertConfiguration(
                                title: "\(data!.titleLeading) - \(data!.titleTrailing)",
                                body: "\(data!.content)",
                                sound: .default
                            )
                            Task {
                                await alightReminderActivity!.update(using: state, alertConfiguration: alertConfig)
                                if data!.state == 2 {
                                    await alightReminderActivity?.end(dismissalPolicy: .default)
                                }
                            }
                        }
                    }
                } else {
                    UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
                        if granted {
                            let content = UNMutableNotificationContent()
                            content.title = "\(data!.titleLeading) - \(data!.titleTrailing)"
                            content.body = "\(data!.content)"
                            content.sound = alightReminderLastState == data!.state ? nil : .default
                            let request = UNNotificationRequest(identifier: "alight_reminder", content: content, trigger: nil)
                            UNUserNotificationCenter.current().add(request) { error in
                                if error != nil {
                                    print(error!)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
            .fileImporter(isPresented: $fileImportExport.importingFile, allowedContentTypes: [.text]) {
                switch ($0) {
                case .success(let fileUrl):
                    do {
                        let started = fileUrl.startAccessingSecurityScopedResource()
                        if started {
                            let _ = fileImportExport.importingFileCallback!(try String(contentsOf: fileUrl))
                            fileUrl.stopAccessingSecurityScopedResource()
                        }
                    } catch {
                    }
                default:
                    break
                }
            }
            .fileExporter(isPresented: $fileImportExport.exportingFile, document: fileImportExport.exportingFileDocument, contentType: .text, defaultFilename: fileImportExport.exportingFileName) {
                switch ($0) {
                case .success(_):
                    let _ = fileImportExport.exportingFileCallback!()
                default:
                    break
                }
            }
            .onOpenURL { url in
                let shareUrl: String
                if url.absoluteString.hasPrefix("https://app.hkbuseta.com/shared?url=") {
                    let urlStr = url.absoluteString
                    let index = urlStr.index(urlStr.startIndex, offsetBy: "https://app.hkbuseta.com/shared?url=".count)
                    shareUrl = String(urlStr[index...])
                } else {
                    shareUrl = url.absoluteString
                }
                AppContextCompose_iosKt.extractShareLinkAndLaunch(shareUrl)
            }
            .onChange(of: quickActionLaunch.url) { url in
                if url != nil {
                    AppContextCompose_iosKt.extractShareLinkAndLaunch(url!)
                    quickActionLaunch.url = nil
                }
            }
            .onAppear {
                if quickActionLaunch.url != nil {
                    AppContextCompose_iosKt.extractShareLinkAndLaunch(quickActionLaunch.url!)
                    quickActionLaunch.url = nil
                }
            }
            .onChange(of: shareUrlData.url) { data in
                if !data.isEmpty {
                    shareUrlDataShowing = true
                }
            }
            .sheet(isPresented: $shareUrlDataShowing, onDismiss: {
                shareUrlData.url = ""
                shareUrlData.title = nil
            }) {
                ShareUrlView(url: shareUrlData.url, title: shareUrlData.title)
            }
        }
    }

}

func encoding(from charsetName: String) -> String.Encoding {
    switch charsetName {
    case "UTF-8", "utf-8":
        return .utf8
    case "US-ASCII", "us-ascii":
        return .ascii
    case "ISO-8859-1", "iso-8859-1":
        return .isoLatin1
    case "ISO-8859-2", "iso-8859-2":
        return .isoLatin2
    case "WINDOWS-1251", "windows-1251":
        return .windowsCP1251
    case "WINDOWS-1252", "windows-1252":
        return .windowsCP1252
    case "WINDOWS-1253", "windows-1253":
        return .windowsCP1253
    case "WINDOWS-1254", "windows-1254":
        return .windowsCP1254
    default:
        print("Warning: Unknown charset \(charsetName). Defaulting to UTF-8.")
        return .utf8
    }
}

func handleDataFromWatch(payload: [String: Any]) {
    let type = payload["messageType"] as! String
    switch type {
    case AppContextCompose_iosKt.SYNC_PREFERENCES_ID:
        let preferences = payload["payload"] as! String
        AppContextCompose_iosKt.syncPreference(context: applicationContext(), preferenceJson: preferences, sync: false)
    case AppContextCompose_iosKt.REQUEST_PREFERENCES_ID:
        let payload: [String: Any] = [
            "messageType": AppContextCompose_iosKt.SYNC_PREFERENCES_ID,
            "payload": AppContextCompose_iosKt.getRawPreference(context: applicationContext())
        ]
        WCSession.default.sendMessage(payload) { _ in }
    case AppContextCompose_iosKt.REQUEST_ALIGHT_REMINDER_ID:
        let payload = [
            "messageType": AppContextCompose_iosKt.RESPONSE_ALIGHT_REMINDER_ID,
            "payload": alightReminderLastRemote ?? ""
        ]
        WCSession.default.sendMessage(payload) { _ in }
    case AppContextCompose_iosKt.TERMINATE_ALIGHT_REMINDER_ID:
        AppContextCompose_iosKt.terminateAlightReminder()
    case AppContextCompose_iosKt.INVALIDATE_CACHE_ID:
        AppContextCompose_iosKt.invalidateCacheAndRestart()
    default:
        print("")
    }
}

func applicationContext() -> AppContextCompose {
    return AppContextCompose_iosKt.applicationAppContext
}
