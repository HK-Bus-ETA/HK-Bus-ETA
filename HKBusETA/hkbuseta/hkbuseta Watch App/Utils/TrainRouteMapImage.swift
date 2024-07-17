//
//  InteractiveImageView.swift
//  HKBusETA Watch App
//
//  Created by LOOHP on 19/5/2024.
//

import SwiftUI

struct TrainRouteMapImage: View {
    
    let highlightTimer = Timer.publish(every: 0.05, on: .main, in: .common).autoconnect()
    
    let imageSize: CGSize
    let image: Image
    let minScale: CGFloat
    let maxScale: CGFloat
    let onTap: (CGPoint) -> Void
    let onPan: (CGPoint) -> Void
    let onScale: (CGFloat) -> Void
    let onLocationJumped: () -> Void
    let highlightPosition: CGPoint?
    let locationJumped: Bool
    
    @State var scale: CGFloat
    @State var targetScale: CGFloat
    @State var scaledSize: CGSize
    @State var offset: CGPoint
    
    @State var scrollAmount = 0.0
    @State var prevScrollAmount = 0.0
    
    @State var animateHighlightSign: CGFloat = 1
    @State var animateHighlightSize: CGFloat = 70
    
    init(imageSize: CGSize, image: Image, highlightPosition: CGPoint? = nil, locationJumped: Bool, minScale: CGFloat, maxScale: CGFloat, initalScale: CGFloat? = nil, initalOffset: CGPoint? = nil, onTap: @escaping (CGPoint) -> Void, onPan: @escaping (CGPoint) -> Void, onScale: @escaping (CGFloat) -> Void, onLocationJumped: @escaping () -> Void) {
        self.imageSize = imageSize
        self.image = image
        self.highlightPosition = highlightPosition
        self.locationJumped = locationJumped
        self.minScale = minScale
        self.maxScale = maxScale
        self.onTap = onTap
        self.onPan = onPan
        self.onScale = onScale
        self.onLocationJumped = onLocationJumped
        if let value = initalScale {
            self._scale = State(initialValue: value)
            self._targetScale = State(initialValue: value)
            self._scaledSize = State(initialValue: CGSize(width: imageSize.width * value, height: imageSize.height * value))
            if let offsetValue = initalOffset {
                self._offset = State(initialValue: offsetValue)
            } else {
                self._offset = State(initialValue: CGPoint(x: -imageSize.width * value / 2, y: -imageSize.height * value / 2))
            }
        } else {
            self._scale = State(initialValue: 1.0)
            self._targetScale = State(initialValue: 1.0)
            self._scaledSize = State(initialValue: imageSize)
            if let value = initalOffset {
                self._offset = State(initialValue: value)
            } else {
                self._offset = State(initialValue: CGPoint(x: -imageSize.width / 2, y: -imageSize.height / 2))
            }
        }
    }
    
    var body: some View {
        GeometryReader { geo in
            Group {
                image.resizable()
                if let originalPoint = highlightPosition {
                    let point = CGPoint(x: originalPoint.x * scale - (animateHighlightSize / 2), y: originalPoint.y * scale - (animateHighlightSize / 2))
                    let highlightSize = CGSize(width: animateHighlightSize, height: animateHighlightSize)
                    Canvas { context, size in
                        context.fill(
                            Circle().path(in: CGRect(origin: point, size: highlightSize)),
                            with: .color(colorInt(0xff199fff).asColor().opacity(0.3))
                        )
                        context.stroke(
                            Circle().path(in: CGRect(origin: point, size: highlightSize)),
                            with: .color(colorInt(0xff199fff).asColor()),
                            lineWidth: 3
                        )
                    }
                    .frame(width: scaledSize.width, height: scaledSize.height)
                    .allowsHitTesting(false)
                    .onReceive(highlightTimer) { _ in
                        if animateHighlightSize == 70 {
                            animateHighlightSign = 1
                        } else if animateHighlightSize == 90 {
                            animateHighlightSign = -1
                        }
                        animateHighlightSize = animateHighlightSize + animateHighlightSign
                    }
                }
            }
                .frame(width: scaledSize.width, height: scaledSize.height)
                .apply {
                    if #available(watchOS 10.0, *) {
                        $0.onTapGesture { pos in
                            onTap(CGPoint(x: pos.x / scale, y: pos.y / scale))
                        }
                    } else {
                        $0
                    }
                }
                .simultaneousGesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { value in
                            let x = self.offset.x + value.location.x - value.startLocation.x
                            let y = self.offset.y + value.location.y - value.startLocation.y
                            if -x >= 0 && -x + geo.size.width <= scaledSize.width {
                                self.offset.x = x
                            }
                            if -y >= 0 && -y + geo.size.height <= scaledSize.height {
                                self.offset.y = y
                            }
                        }
                )
                .onChange(of: offset, perform: onPan)
                .offset(x: offset.x, y: offset.y)
                .focusable()
                .digitalCrownRotation($scrollAmount)
                .onChange(of: scrollAmount) { scrollAmount in
                    let diff = prevScrollAmount - scrollAmount
                    prevScrollAmount = scrollAmount
                    targetScale = max(minScale, min(maxScale, targetScale - diff * 0.001))
                }
                .onChange(of: targetScale) { targetScale in
                    scaledSize = CGSize(width: imageSize.width * targetScale, height: imageSize.height * targetScale)
                    let midX = geo.size.width / 2
                    let midY = geo.size.height / 2
                    let x = (offset.x - midX) / scale * targetScale + midX
                    let y = (offset.y - midY) / scale * targetScale + midY
                    self.offset.x = -max(0, min(scaledSize.width - geo.size.width, -x))
                    self.offset.y = -max(0, min(scaledSize.height - geo.size.height, -y))
                    scale = targetScale
                }
                .onChange(of: scale, perform: onScale)
                .onChange(of: highlightPosition) { optPos in
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                        if !locationJumped {
                            if let pos = optPos {
                                let midX = geo.size.width / 2
                                let midY = geo.size.height / 2
                                let x = -pos.x * scale + midX
                                let y = -pos.y * scale + midY
                                self.offset.x = -max(0, min(scaledSize.width - geo.size.width, -x))
                                self.offset.y = -max(0, min(scaledSize.height - geo.size.height, -y))
                                onLocationJumped()
                            }
                        }
                    }
                }
                .onAppear {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                        if !locationJumped {
                            if let pos = highlightPosition {
                                let midX = geo.size.width / 2
                                let midY = geo.size.height / 2
                                let x = -pos.x * scale + midX
                                let y = -pos.y * scale + midY
                                self.offset.x = -max(0, min(scaledSize.width - geo.size.width, -x))
                                self.offset.y = -max(0, min(scaledSize.height - geo.size.height, -y))
                                onLocationJumped()
                            }
                        }
                    }
                }
        }
    }
}
