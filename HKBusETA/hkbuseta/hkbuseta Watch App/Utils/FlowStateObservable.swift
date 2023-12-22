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

class FlowStateObservable<T>: ObservableObject {
    
    @Published var state: T
    
    private var disposable: Disposable?
    
    init<Failure: Error, Unit>(defaultValue: T, nativeFlow: @escaping NativeFlow<T, Failure, Unit>) {
        self.state = defaultValue
        self.setup(nativeFlow: nativeFlow)
    }
    
    func setup<Failure: Error, Unit>(nativeFlow: @escaping NativeFlow<T, Failure, Unit>) {
        let observable = createObservable(for: nativeFlow)

        disposable = observable.subscribe(onNext: { value in
            DispatchQueue.main.async {
                self.state = value
            }
            print("Received value: \(value)")
        }, onError: { error in
            print("Received error: \(error)")
        }, onCompleted: {
            print("Observable completed")
        }, onDisposed: {
            print("Observable disposed")
        })
    }
    
    deinit {
        disposable?.dispose()
    }
    
}
