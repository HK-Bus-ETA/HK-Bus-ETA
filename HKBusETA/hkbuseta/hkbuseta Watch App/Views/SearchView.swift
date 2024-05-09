//
//  SearchView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 21/12/2023.
//

import SwiftUI
import shared

struct SearchView: AppScreenView {
    
    @StateObject private var lastLookupRoutes = StateFlowListObservable(stateFlow: Shared().lastLookupRoutes)
    
    @State var state: RouteKeyboardState
    
    private let storage: KotlinMutableDictionary<NSString, AnyObject>
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        self.storage = storage
        let input = storage["input"] as? String ?? ""
        self.state = RouteKeyboardState(text: input, nextCharResult: registry(appContext).getPossibleNextChar(input: input), isLoading: false, showLoadingIndicator: false)
    }
    
    var body: some View {
        VStack(spacing: 0) {
            let shape = RoundedRectangle(cornerRadius: 13.scaled(appContext))
            Text(state.text.isEmpty ? (Shared().language == "en" ? "Input Route" : "輸入路線") : Shared().getMtrLineName(lineName: state.text))
                .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                .font(.system(size: 22.scaled(appContext, true)))
                .frame(width: 150.0.scaled(appContext), height: 40.0.scaled(appContext))
                .background { colorInt(0xFF1A1A1A).asColor().clipShape(shape) }
                .overlay(shape.stroke(colorInt(0xFF252525).asColor(), lineWidth: 4.scaled(appContext)))
                .padding()
            HStack(spacing: 0) {
                VStack(spacing: 0) {
                    KeyboardKey(content: "1")
                    KeyboardKey(content: "4")
                    KeyboardKey(content: "7")
                    KeyboardKey(content: "<", longContent: "-")
                }.frame(width: 35.scaled(appContext), height: 155.scaled(appContext))
                VStack(spacing: 0) {
                    KeyboardKey(content: "2")
                    KeyboardKey(content: "5")
                    KeyboardKey(content: "8")
                    KeyboardKey(content: "0")
                }.frame(width: 35.scaled(appContext), height: 155.scaled(appContext))
                VStack(spacing: 0) {
                    KeyboardKey(content: "3")
                    KeyboardKey(content: "6")
                    KeyboardKey(content: "9")
                    KeyboardKey(content: "/")
                }.frame(width: 35.scaled(appContext), height: 155.scaled(appContext))
                Spacer().frame(fixedSize: 7.5.scaled(appContext))
                ScrollView(.vertical) {
                    VStack(spacing: 0) {
                        let currentText = state.text
                        if currentText.isEmpty {
                            KeyboardKey(content: "!")
                            KeyboardKey(content: "~")
                        }
                        ForEach(65..<91) { codePoint in
                            let alphabet = Character(UnicodeScalar(codePoint)!)
                            if (!state.nextCharResult.characters.filter { $0.description == alphabet.description }.isEmpty) {
                                KeyboardKey(content: alphabet).id(alphabet)
                            }
                        }
                    }.frame(width: 35.scaled(appContext))
                }.frame(height: 155.scaled(appContext))
            }
        }
        .onChange(of: state.text) { _ in
            storage["input"] = state.text
        }
    }
    
    func KeyboardKey(content: Character) -> some View {
        KeyboardKey(content: content, longContent: nil)
    }
    
    func KeyboardKey(content: Character, longContent: Character?) -> some View {
        let enabled: Bool
        switch content {
        case "/":
            enabled = state.nextCharResult.hasExactMatch
        case "<", "!", "~":
            enabled = true
        default:
            enabled = !state.nextCharResult.characters.filter { $0.description == content.description }.isEmpty
        }
        let isLookupButton = content == "<" && !lastLookupRoutes.state.isEmpty && state.text.isEmpty
        return Button(action: { /* do nothing */ }) {
            switch content {
            case "<":
                if isLookupButton {
                    Image(systemName: "clock.arrow.circlepath")
                        .font(.system(size: 17.scaled(appContext, true)))
                        .foregroundColor(colorInt(0xFF03A9F4).asColor())
                } else {
                    Image(systemName: "trash")
                        .font(.system(size: 17.scaled(appContext, true)))
                        .foregroundColor(.red)
                }
            case "/":
                if state.showLoadingIndicator {
                    let color = colorInt(0xFFF9DE09).asColor()
                    IndeterminateCircularProgressIndicator(tintColor: color, trackColor: color.adjustBrightness(percentage: 0.4), lineWidth: 3.0.scaled(appContext, true))
                        .frame(width: 17.0.scaled(appContext, true), height: 17.0.scaled(appContext, true))
                } else {
                    Image(systemName: "checkmark")
                        .font(.system(size: 17.scaled(appContext, true)))
                        .foregroundColor(state.nextCharResult.hasExactMatch ? .green : colorInt(0xFF444444).asColor())
                }
            case "!":
                Image("mtr")
                    .resizable()
                    .frame(width: 20.0.scaled(appContext, true), height: 20.0.scaled(appContext, true))
            case "~":
                Image(systemName: "ferry.fill")
                    .font(.system(size: 16.scaled(appContext, true)))
                    .foregroundColor(colorInt(0xFF66CCFF).asColor())
            default:
                Text(content.description)
                    .font(.system(size: 20.scaled(appContext, true), weight: .bold))
                    .foregroundColor(!state.nextCharResult.characters.filter { $0.description == content.description }.isEmpty ? .white : colorInt(0xFF444444).asColor())
            }
        }
        .frame(width: 35.scaled(appContext), height: (content.isLetter || content == "!" ? 35 : 40).scaled(appContext))
        .buttonStyle(PlainButtonStyle())
        .simultaneousGesture(
            LongPressGesture()
                .onEnded { _ in
                    if !state.isLoading && longContent != nil && !isLookupButton {
                        playHaptics()
                        handleInput(input: longContent!)
                    }
                }
        )
        .highPriorityGesture(
            TapGesture()
                .onEnded { _ in
                    if !state.isLoading {
                        handleInput(input: content)
                    }
                }
        )
        .disabled(!enabled || (state.isLoading && content != "/"))
    }
    
    func handleInput(input: Character) {
        let originalText = state.text
        if input == "/" || input == "!" || input == "~" || (input == "<" && !lastLookupRoutes.state.isEmpty && originalText.isEmpty) {
            AppContextWatchOSKt.handleSearchInputLaunch(context: appContext, input: UInt16(input.unicodeScalars.first!.value), text: originalText, preRun: {
                state = RouteKeyboardState(text: state.text, nextCharResult: state.nextCharResult, isLoading: true, showLoadingIndicator: state.showLoadingIndicator)
            }, loadingIndicator: {
                state = RouteKeyboardState(text: state.text, nextCharResult: state.nextCharResult, isLoading: state.isLoading, showLoadingIndicator: true)
            }, launch: { result in
                let data = newAppDataConatiner()
                data["result"] = result
                if input == "<" {
                    data["recentSort"] = RecentSortMode.forced
                    data["listType"] = RouteListType.Companion().RECENT
                } else {
                    data["listType"] = RouteListType.Companion().NORMAL
                }
                appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.listRoutes, data))
            }, complete: {
                state = RouteKeyboardState(text: state.text, nextCharResult: state.nextCharResult, isLoading: false, showLoadingIndicator: false)
            })
        } else {
            let newText: String
            if input == "<" {
                newText = !originalText.isEmpty ? String(originalText.dropLast()) : originalText
            } else if input == "-" {
                newText = ""
            } else {
                newText = originalText + String(input)
            }
            let possibleNextChar = registry(appContext).getPossibleNextChar(input: newText)
            state = RouteKeyboardState(text: newText, nextCharResult: possibleNextChar, isLoading: state.isLoading, showLoadingIndicator: state.showLoadingIndicator)
        }
    }
}

struct RouteKeyboardState {
    let text: String
    let nextCharResult: Registry.PossibleNextCharResult
    let isLoading: Bool
    let showLoadingIndicator: Bool
}
