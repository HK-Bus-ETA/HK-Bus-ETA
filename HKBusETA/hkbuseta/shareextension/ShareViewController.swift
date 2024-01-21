//
//  ShareViewController.swift
//  shareextension
//
//  Created by LOOHP on 21/01/2024.
//

import UIKit
import UniformTypeIdentifiers
import SwiftUI
import WatchConnectivity

class ShareViewController: UIViewController, WCSessionDelegate {

    override func viewDidLoad() {
        super.viewDidLoad()
        
        guard
            let extensionItem = extensionContext?.inputItems.first as? NSExtensionItem,
            let itemProvider = extensionItem.attachments?.first else {
                close()
                return
            }
        
        if WCSession.isSupported() {
            let session = WCSession.default
            session.delegate = self
            session.activate()
        }
        
        let urlDataType = UTType.url.identifier
        if itemProvider.hasItemConformingToTypeIdentifier(urlDataType) {
            itemProvider.loadItem(forTypeIdentifier: urlDataType , options: nil) { (providedUrl, error) in
                if error != nil {
                    self.close()
                    return
                }
                
                if let url = providedUrl as? URL {
                    DispatchQueue.main.async {
                        let contentView = UIHostingController(rootView: ShareExtensionView(url: url.absoluteString))
                        self.addChild(contentView)
                        self.view.addSubview(contentView.view)
                        
                        contentView.view.translatesAutoresizingMaskIntoConstraints = false
                        contentView.view.topAnchor.constraint(equalTo: self.view.topAnchor).isActive = true
                        contentView.view.bottomAnchor.constraint (equalTo: self.view.bottomAnchor).isActive = true
                        contentView.view.leftAnchor.constraint(equalTo: self.view.leftAnchor).isActive = true
                        contentView.view.rightAnchor.constraint (equalTo: self.view.rightAnchor).isActive = true
                    }
                } else {
                    self.close()
                    return
                }
            }
        } else {
            close()
            return
        }
        
        NotificationCenter.default.addObserver(forName: NSNotification.Name("close"), object: nil, queue: nil) { _ in
            DispatchQueue.main.async {
                self.close()
            }
        }
    }
    
    func close() {
        self.extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
    }
    
    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        
    }
    
    func sessionDidBecomeInactive(_ session: WCSession) {
        
    }
    
    func sessionDidDeactivate(_ session: WCSession) {
        
    }

}
