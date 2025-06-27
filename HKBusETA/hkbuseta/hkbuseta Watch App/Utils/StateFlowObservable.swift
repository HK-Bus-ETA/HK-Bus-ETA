//
//  FlowStateObservable.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 21/12/2023.
//

import shared

class StateFlowObservable<T: AnyObject>: ObservableObject {
    
    @Published var state: T
    
    private let stateFlow: MutableNonNullStateFlow<T>
    private var stateWatcher: Closeable?
    
    private var subscribed = false
    private let lock = NSLock()

    init(stateFlow: MutableNonNullStateFlow<T>, initSubscribe: Bool = false) {
        self.stateFlow = stateFlow
        self.state = stateFlow.value 
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
        stateWatcher = stateFlow.watch { [weak self] state in
            Task.detached { @MainActor in
                self?.state = state
            }
        }
        subscribed = true
    }
    
    func unsubscribe() {
        lock.lock()
        defer { lock.unlock() }
        stateWatcher?.close()
        subscribed = false
    }
    
    func isSubscribed() -> Bool {
        return subscribed
    }
    
    deinit {
        unsubscribe()
    }
    
}

class StateFlowNullableObservable<T: AnyObject>: ObservableObject {
    
    @Published var state: T?
    
    private let stateFlow: MutableNullableStateFlow<T>
    private var stateWatcher: Closeable?
    
    private var subscribed = false
    private let lock = NSLock()

    init(stateFlow: MutableNullableStateFlow<T>, initSubscribe: Bool = false) {
        self.stateFlow = stateFlow
        self.state = stateFlow.valueNullable
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
        stateWatcher = stateFlow.watch { [weak self] state in
            Task.detached { @MainActor in
                self?.state = state.value
            }
        }
        subscribed = true
    }
    
    func unsubscribe() {
        lock.lock()
        defer { lock.unlock() }
        stateWatcher?.close()
        subscribed = false
    }
    
    func isSubscribed() -> Bool {
        return subscribed
    }
    
    deinit {
        unsubscribe()
    }
    
}

class StateFlowListObservable<T: AnyObject>: ObservableObject {
    
    @Published var state: [T]
    
    private let stateFlow: MutableNonNullStateFlowList<T>
    private var stateWatcher: Closeable?
    
    private var subscribed = false
    private let lock = NSLock()

    init(stateFlow: MutableNonNullStateFlowList<T>, initSubscribe: Bool = false) {
        self.stateFlow = stateFlow
        self.state = stateFlow.value as! [T]
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
        stateWatcher = stateFlow.watch { [weak self] state in
            Task.detached { @MainActor in
                self?.state = state as! [T]
            }
        }
        subscribed = true
    }
    
    func unsubscribe() {
        lock.lock()
        defer { lock.unlock() }
        stateWatcher?.close()
        subscribed = false
    }
    
    func isSubscribed() -> Bool {
        return subscribed
    }
    
    deinit {
        unsubscribe()
    }
    
}

func typedValue<T>(_ stateFlowList: MutableNonNullStateFlowList<T>) -> [T] {
    return stateFlowList.value as! [T]
}
