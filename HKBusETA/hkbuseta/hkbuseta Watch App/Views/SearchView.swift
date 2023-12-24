//
//  SearchView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 21/12/2023.
//

import SwiftUI
import shared
import KMPNativeCoroutinesCore
import KMPNativeCoroutinesRxSwift
import KMPNativeCoroutinesAsync
import KMPNativeCoroutinesCombine
import RxSwift

struct SearchView: View {
    
    @State var state = RouteKeyboardState(text: defaultText(), nextCharResult: registry().getPossibleNextChar(input: ""))
    @State var hasHistory = Shared().hasFavoriteAndLookupRoute()
    
    init(data: [String: Any]?) {
        
    }
    
    var body: some View {
        VStack {
            Text(Shared().getMtrLineName(lineName: state.text))
                .font(.system(size: 22.scaled()))
                .frame(width: 150.0.scaled(), height: 40.0.scaled())
                .background { 0xFF1A1A1A.asColor() }
                .overlay(
                    RoundedRectangle(cornerRadius: 4)
                        .stroke(0xFF252525.asColor(), lineWidth: 4.scaled())
                )
                .padding()
            HStack {
                VStack {
                    KeyboardKey(content: "7")
                    KeyboardKey(content: "4")
                    KeyboardKey(content: "1")
                    KeyboardKey(content: "<", longContent: "-")
                }.frame(width: 35.scaled(), height: 155.scaled())
                VStack {
                    KeyboardKey(content: "8")
                    KeyboardKey(content: "5")
                    KeyboardKey(content: "2")
                    KeyboardKey(content: "0")
                }.frame(width: 35.scaled(), height: 155.scaled())
                VStack {
                    KeyboardKey(content: "9")
                    KeyboardKey(content: "6")
                    KeyboardKey(content: "3")
                    KeyboardKey(content: "/")
                }.frame(width: 35.scaled(), height: 155.scaled())
                ScrollView(.vertical) {
                    let currentText = state.text
                    if currentText.isEmpty || currentText == defaultText() {
                        KeyboardKey(content: "!")
                    }
                    ForEach(65..<91) { codePoint in
                        let alphabet = Character(UnicodeScalar(codePoint)!)
                        if (!state.nextCharResult.characters.filter { $0.description == alphabet.description }.isEmpty) {
                            KeyboardKey(content: alphabet)
                        }
                    }
                }.frame(width: 35.scaled(), height: 155.scaled())
            }
        }
        .onAppear {
            hasHistory = Shared().hasFavoriteAndLookupRoute()
        }
    }
    
    func KeyboardKey(content: Character) -> some View {
        KeyboardKey(content: content, longContent: content)
    }
    
    func KeyboardKey(content: Character, longContent: Character) -> some View {
        let enabled: Bool
        switch content {
        case "/":
            enabled = state.nextCharResult.hasExactMatch
        case "<", "!":
            enabled = true
        default:
            enabled = !state.nextCharResult.characters.filter { $0.description == content.description }.isEmpty
        }
        return Button(action: {}) {
            switch content {
            case "<":
                if hasHistory && state.text.isEmpty || state.text == defaultText() {
                    Image(systemName: "clock.arrow.circlepath")
                        .font(.system(size: 17.scaled()))
                        .foregroundColor(0xFF03A9F4.asColor())
                } else {
                    Image(systemName: "trash")
                        .font(.system(size: 17.scaled()))
                        .foregroundColor(.red)
                }
            case "/":
                Image(systemName: "checkmark")
                    .font(.system(size: 17.scaled()))
                    .foregroundColor(state.nextCharResult.hasExactMatch ? .green : 0xFF444444.asColor())
            case "!":
                Image("mtr")
                    .resizable()
                    .frame(width: 20.0.scaled(), height: 20.0.scaled())
                    .foregroundColor(.red)
            default:
                Text(content.description)
                    .font(.system(size: 20.scaled()))
                    .foregroundColor(!state.nextCharResult.characters.filter { $0.description == content.description }.isEmpty ? .white : 0xFF444444.asColor())
                    .bold()
            }
        }
        .frame(width: 35.0.scaled(), height: (content.isLetter || content == "!" ? 30.0 : 35.0).scaled())
        .buttonStyle(PlainButtonStyle())
        .simultaneousGesture(
            LongPressGesture()
                .onEnded { _ in
                    playHaptics()
                    handleInput(input: longContent)
                }
        )
        .highPriorityGesture(
            TapGesture()
                .onEnded { _ in
                    handleInput(input: content)
                }
        )
        .disabled(!enabled)
    }
    
    func handleInput(input: Character) {
        var originalText = state.text
        if originalText == defaultText() {
            originalText = ""
        }

        if input == "/" || input == "!" || (input == "<" && Shared().hasFavoriteAndLookupRoute() && originalText.isEmpty) {
            let result: [RouteSearchResultEntry]
            switch input {
            case "!":
                result = registry().findRoutes(input: "", exact: false, predicate: { r in
                    KotlinBoolean(bool: r.bound[Operator.Companion().MTR] != nil)
                })
            case "<":
                result = registry().findRoutes(input: "", exact: false, coPredicate: { r, c in
                    let meta: String
                    switch c {
                    case Operator.Companion().GMB:
                        meta = r.gmbRegion!.name
                    case Operator.Companion().NLB:
                        meta = r.nlbId
                    default:
                        meta = ""
                    }
                    return KotlinBoolean(bool: Shared().getFavoriteAndLookupRouteIndex(routeNumber: r.routeNumber, co: c, meta: meta) < Int32.max)
                })
            default:
                result = registry().findRoutes(input: originalText, exact: true)
            }
            if !result.isEmpty {
                let data = newAppDataConatiner()
                data["result"] = result
                if input == "<" {
                    data["recentSort"] = RecentSortMode.forced
                }
                data["listType"] = RouteListType.Companion().RECENT
                appContext().appendStack(screen: AppScreen.listRoutes, mutableData: data)
            }
        } else {
            let newText: String
            if input == "<" {
                newText = !originalText.isEmpty ? String(originalText.dropLast()) : originalText
            } else if input == "-" {
                newText = ""
            } else {
                newText = originalText + String(input)
            }
            let possibleNextChar = registry().getPossibleNextChar(input: newText)
            let text = newText.isEmpty ? defaultText() : newText
            state = RouteKeyboardState(text: text, nextCharResult: possibleNextChar)
        }
    }
}

#Preview {
    SearchView(data: nil)
}

struct RouteKeyboardState {
    var text: String
    var nextCharResult: Registry.PossibleNextCharResult
}

func defaultText() -> String {
    return Shared().language == "en" ? "Input Route" : "輸入路線"
}
