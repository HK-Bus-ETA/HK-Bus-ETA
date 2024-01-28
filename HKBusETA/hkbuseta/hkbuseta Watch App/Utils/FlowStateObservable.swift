//
//  FlowStateObservable.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 21/12/2023.
//

import Foundation
import KMPNativeCoroutinesCore
import KMPNativeCoroutinesRxSwift
import KMPNativeCoroutinesAsync
import KMPNativeCoroutinesCombine
import RxSwift
import shared

class FlowStateObservable<T>: ObservableObject {
    
    @Published var state: T
    
    private let nativeFlow: NativeFlow<T, Error, KotlinUnit>
    private var disposable: Disposable?
    
    private var subscribed = false
    private let lock = NSLock()
    
    init(defaultValue: T, nativeFlow: @escaping NativeFlow<T, Error, KotlinUnit>, initSubscribe: Bool = false) {
        self.state = defaultValue
        self.nativeFlow = nativeFlow
        if initSubscribe {
            subscribe()
        }
    }
    
    func subscribe() {
        lock.lock()
        defer { lock.unlock() }
        if subscribed {
            return
        }
        let observable = createObservable(for: nativeFlow)
        disposable = observable.subscribe(onNext: { value in
            DispatchQueue.main.async {
                self.state = value
            }
            // print("Received value: \(value)")
        }, onError: { error in
            // print("Received error: \(error)")
        }, onCompleted: {
            // print("Observable completed")
        }, onDisposed: {
            // print("Observable disposed")
        })
        subscribed = true
    }
    
    func unsubscribe() {
        lock.lock()
        defer { lock.unlock() }
        disposable?.dispose()
        subscribed = false
    }
    
    func isSubscribed() -> Bool {
        return subscribed
    }
    
    deinit {
        unsubscribe()
    }
    
}
