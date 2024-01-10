//
//  WatchOSMTRRouteMapUtils.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 25/12/2023.
//

import SwiftUI
import shared

func MTRLineSection(appContext: AppContext, sectionData: MTRStopSectionData, ambientMode: Bool) -> some View {
    let mainLine = sectionData.mainLine
    let spurLine = sectionData.spurLine
    let co = sectionData.co
    let color = sectionData.color.asColor()
    let isLrtCircular = sectionData.isLrtCircular
    let interchangeData = sectionData.interchangeData
    let hasOutOfStation = sectionData.hasOutOfStation
    let stopByBranceId = sectionData.stopByBranchId
    return Canvas { context, size in
        let width = size.width
        let height = size.height
        
        let leftShift = hasOutOfStation ? 17.0.scaled(appContext) : 0
        let horizontalCenter = width / 2.0 - leftShift
        let horizontalPartition = width / 10.0
        let horizontalCenterPrimary = stopByBranceId.count == 1 ? horizontalCenter : horizontalPartition * 3.0
        let horizontalCenterSecondary = horizontalPartition * 7.0
        let verticalCenter = height / 2.0
        let lineWidth = 6.0.scaled(appContext)
        let outlineWidth = lineWidth * 0.6
        let lineOffset = 6.0.scaled(appContext)
        let dashEffect: [CGFloat] = [10.0.scaled(appContext), 5.0.scaled(appContext)]
        
        var useSpurStopCircle = false
        if mainLine != nil {
            if mainLine!.isFirstStation {
                context.stroke(
                    Path { path in
                        path.move(to: CGPoint(x: horizontalCenterPrimary, y: verticalCenter))
                        path.addLine(to: CGPoint(x: horizontalCenterPrimary, y: size.height))
                    },
                    with: .color(color),
                    lineWidth: lineWidth,
                    outlineMode: ambientMode,
                    outlineWidth: outlineWidth
                )

                if isLrtCircular {
                    context.stroke(
                        Path { path in
                            path.move(to: CGPoint(x: horizontalCenterPrimary, y: -verticalCenter))
                            path.addLine(to: CGPoint(x: horizontalCenterPrimary, y: verticalCenter))
                        },
                        with: .linearGradient(
                            Gradient(stops: [
                                .init(color: color.withAlpha(alpha: 0), location: 0),
                                .init(color: color, location: 1)
                            ]),
                            startPoint: CGPoint(x: horizontalCenterPrimary, y: -verticalCenter / 2),
                            endPoint: CGPoint(x: horizontalCenterPrimary, y: verticalCenter)
                        ),
                        lineWidth: lineWidth,
                        outlineMode: ambientMode,
                        outlineWidth: outlineWidth
                    )
                }
            } else if mainLine!.isLastStation {
                context.stroke(
                    Path { path in
                        path.move(to: CGPoint(x: horizontalCenterPrimary, y: 0))
                        path.addLine(to: CGPoint(x: horizontalCenterPrimary, y: verticalCenter))
                    },
                    with: .color(color),
                    lineWidth: lineWidth,
                    outlineMode: ambientMode,
                    outlineWidth: outlineWidth
                )

                if isLrtCircular {
                    context.stroke(
                        Path { path in
                            path.move(to: CGPoint(x: horizontalCenterPrimary, y: verticalCenter))
                            path.addLine(to: CGPoint(x: horizontalCenterPrimary, y: height + verticalCenter / 2))
                        },
                        with: .linearGradient(
                            Gradient(colors: [color, color.withAlpha(alpha: 0)]),
                            startPoint: CGPoint(x: horizontalCenterPrimary, y: verticalCenter),
                            endPoint: CGPoint(x: horizontalCenterPrimary, y: height + verticalCenter / 2)
                        ),
                        lineWidth: lineWidth,
                        outlineMode: ambientMode,
                        outlineWidth: outlineWidth
                    )
                }
            } else {
                context.stroke(
                    Path { path in
                        path.move(to: CGPoint(x: horizontalCenterPrimary, y: 0))
                        path.addLine(to: CGPoint(x: horizontalCenterPrimary, y: height))
                    },
                    with: .color(color),
                    lineWidth: lineWidth,
                    outlineMode: ambientMode,
                    outlineWidth: outlineWidth
                )
            }
            if mainLine!.hasOtherParallelBranches {
                context.stroke(
                    Path { path in
                        path.move(to: CGPoint(x: horizontalCenterPrimary, y: lineOffset))
                        path.addLine(to: CGPoint(x: horizontalCenter, y: lineOffset))
                        path.addArc(center: CGPoint(x: horizontalCenter, y: (horizontalCenterSecondary - horizontalCenter) + lineOffset),
                                    radius: horizontalCenterSecondary - horizontalCenter,
                                    startAngle: .degrees(-90),
                                    endAngle: .degrees(0),
                                    clockwise: false)
                        path.addLine(to: CGPoint(x: horizontalCenterSecondary, y: height))
                    },
                    with: .color(color),
                    style: StrokeStyle(
                        lineWidth: lineWidth,
                        dash: dashEffect
                    ),
                    outlineMode: ambientMode,
                    outlineWidth: outlineWidth
                )
            }
            switch mainLine!.sideSpurLineType {
            case .combine:
                useSpurStopCircle = true
                context.stroke(
                    Path() { path in
                        path.move(to: CGPoint(x: horizontalCenterPrimary, y: verticalCenter))
                        path.addLine(to: CGPoint(x: horizontalCenter, y: verticalCenter))
                        path.addArc(center: CGPoint(x: horizontalCenter, y: verticalCenter - (horizontalCenterSecondary - horizontalCenter)),
                                    radius: horizontalCenterSecondary - horizontalCenter,
                                    startAngle: .degrees(90),
                                    endAngle: .degrees(0),
                                    clockwise: true)
                        path.addLine(to: CGPoint(x: horizontalCenterSecondary, y: 0))
                    }, 
                    with: .color(color), 
                    lineWidth: lineWidth,
                    outlineMode: ambientMode,
                    outlineWidth: outlineWidth
                )

            case .diverge:
                useSpurStopCircle = true
                context.stroke(
                    Path() { path in
                        path.move(to: CGPoint(x: horizontalCenterPrimary, y: verticalCenter))
                        path.addLine(to: CGPoint(x: horizontalCenter, y: verticalCenter))
                        path.addArc(center: CGPoint(x: horizontalCenter, y: verticalCenter + (horizontalCenterSecondary - horizontalCenter)),
                                    radius: horizontalCenterSecondary - horizontalCenter,
                                    startAngle: .degrees(-90),
                                    endAngle: .degrees(0),
                                    clockwise: false)
                        path.addLine(to: CGPoint(x: horizontalCenterSecondary, y: height))
                    },
                    with: .color(color),
                    lineWidth: lineWidth,
                    outlineMode: ambientMode,
                    outlineWidth: outlineWidth
                )
            default:
                break
            }
        } else if spurLine != nil {
            context.stroke(
                Path { path in
                    path.move(to: CGPoint(x: horizontalCenterPrimary, y: 0))
                    path.addLine(to: CGPoint(x: horizontalCenterPrimary, y: height))
                },
                with: .color(color),
                lineWidth: lineWidth,
                outlineMode: ambientMode,
                outlineWidth: outlineWidth
            )
            let dashLineResult = spurLine!.dashLineResult
            if dashLineResult.value {
                if dashLineResult.isStartOfSpur {
                    context.stroke(
                        Path { path in
                            path.move(to: CGPoint(x: horizontalCenterPrimary, y: lineOffset))
                            path.addLine(to: CGPoint(x: horizontalCenter, y: lineOffset))
                            path.addArc(center: CGPoint(x: horizontalCenter, y: (horizontalCenterSecondary - horizontalCenter) + lineOffset),
                                        radius: horizontalCenterSecondary - horizontalCenter,
                                        startAngle: .degrees(-90),
                                        endAngle: .degrees(0),
                                        clockwise: false)
                            path.addLine(to: CGPoint(x: horizontalCenterSecondary, y: height))
                        },
                        with: .color(color),
                        style: StrokeStyle(
                            lineWidth: lineWidth,
                            dash: dashEffect
                        ),
                        outlineMode: ambientMode,
                        outlineWidth: outlineWidth
                    )
                } else if dashLineResult.isEndOfSpur {
                    context.stroke(
                        Path { path in
                            path.move(to: CGPoint(x: horizontalCenterPrimary, y: height - lineOffset))
                            path.addLine(to: CGPoint(x: horizontalCenter, y: height - lineOffset))
                            path.addArc(center: CGPoint(x: horizontalCenter, y: (height - lineOffset) - (horizontalCenterSecondary - horizontalCenter)),
                                        radius: horizontalCenterSecondary - horizontalCenter,
                                        startAngle: .degrees(90),
                                        endAngle: .degrees(0),
                                        clockwise: true)
                            path.addLine(to: CGPoint(x: horizontalCenterSecondary, y: 0))
                        },
                        with: .color(color),
                        style: StrokeStyle(
                            lineWidth: lineWidth,
                            dash: dashEffect
                        ),
                        outlineMode: ambientMode,
                        outlineWidth: outlineWidth
                    )
                } else {
                    context.stroke(
                        Path { path in
                            path.move(to: CGPoint(x: horizontalCenterSecondary, y: 0))
                            path.addLine(to: CGPoint(x: horizontalCenterSecondary, y: height))
                        },
                        with: .color(color),
                        style: StrokeStyle(
                            lineWidth: lineWidth,
                            dash: dashEffect
                        ),
                        outlineMode: ambientMode,
                        outlineWidth: outlineWidth
                    )
                }
            } else if spurLine!.isFirstStation {
                context.stroke(
                    Path { path in
                        path.move(to: CGPoint(x: horizontalCenterSecondary, y: verticalCenter))
                        path.addLine(to: CGPoint(x: horizontalCenterSecondary, y: height))
                    },
                    with: .color(color),
                    lineWidth: lineWidth,
                    outlineMode: ambientMode,
                    outlineWidth: outlineWidth
                )
            } else if spurLine!.isLastStation {
                context.stroke(
                    Path { path in
                        path.move(to: CGPoint(x: horizontalCenterSecondary, y: 0))
                        path.addLine(to: CGPoint(x: horizontalCenterSecondary, y: verticalCenter))
                    },
                    with: .color(color),
                    lineWidth: lineWidth,
                    outlineMode: ambientMode,
                    outlineWidth: outlineWidth
                )
            } else {
                context.stroke(
                    Path { path in
                        path.move(to: CGPoint(x: horizontalCenterSecondary, y: 0))
                        path.addLine(to: CGPoint(x: horizontalCenterSecondary, y: height))
                    },
                    with: .color(color),
                    lineWidth: lineWidth,
                    outlineMode: ambientMode,
                    outlineWidth: outlineWidth
                )
            }
        }
        let interchangeLineWidth = 15.0.scaled(appContext)
        let interchangeLineHeight = 6.0.scaled(appContext)
        let interchangeLineSpacing = interchangeLineHeight * 1.5
        if interchangeData.isHasLightRail && co != Operator.Companion().LRT {
            context.fill(
                Path(roundedRect: CGRect(
                        x: horizontalCenterPrimary - interchangeLineWidth,
                        y: verticalCenter - interchangeLineHeight / 2,
                        width: interchangeLineWidth,
                        height: interchangeLineHeight
                    ),
                     cornerSize: CGSize(width: interchangeLineHeight / 2, height: interchangeLineHeight / 2)
                ),
                with: .color(colorInt(0xFFD3A809).asColor()),
                outlineMode: ambientMode,
                outlineWidth: outlineWidth * 0.75
            )
        } else if !interchangeData.lines.isEmpty {
            var leftCorner = CGPoint(
                x: horizontalCenterPrimary - interchangeLineWidth,
                y: verticalCenter - (Double(interchangeData.lines.count - 1) * interchangeLineSpacing / 2) - interchangeLineHeight / 2
            )
            for interchange in interchangeData.lines {
                context.fill(
                    Path(roundedRect: CGRect(
                            x: leftCorner.x,
                            y: leftCorner.y,
                            width: interchangeLineWidth,
                            height: interchangeLineHeight
                        ),
                         cornerSize: CGSize(width: interchangeLineHeight / 2, height: interchangeLineHeight / 2)
                    ),
                    with: .color(interchange == "HighSpeed" ? colorInt(0xFF9C948B).asColor() : Operator.Companion().MTR.getColor(routeNumber: interchange, elseColor: 0xFFFFFFFF as Int64).asColor()),
                    outlineMode: ambientMode,
                    outlineWidth: outlineWidth * 0.75
                )
                leftCorner = CGPoint(x: leftCorner.x, y: leftCorner.y + interchangeLineSpacing)
            }
        }
        let circleWidth = 10.5.scaled(appContext)
        if !interchangeData.outOfStationLines.isEmpty {
            let otherStationHorizontalCenter = horizontalCenterPrimary + circleWidth * 2
            let connectionLineWidth: CGFloat = 2.0.scaled(appContext)
            if interchangeData.isOutOfStationPaid {
                context.stroke(
                    Path { path in
                        path.move(to: CGPoint(x: horizontalCenterPrimary, y: verticalCenter))
                        path.addLine(to: CGPoint(x: otherStationHorizontalCenter, y: verticalCenter))
                    },
                    with: .color(colorInt(0xFF003180).asColor().adjustBrightness(percentage: ambientMode ? 1.5 : 1)),
                    lineWidth: connectionLineWidth
                )
            } else {
                context.stroke(
                    Path { path in
                        path.move(to: CGPoint(x: horizontalCenterPrimary, y: verticalCenter))
                        path.addLine(to: CGPoint(x: otherStationHorizontalCenter, y: verticalCenter))
                    },
                    with: .color(colorInt(0xFF003180).asColor().adjustBrightness(percentage: ambientMode ? 1.5 : 1)),
                    style: StrokeStyle(
                        lineWidth: connectionLineWidth,
                        dash: [2.0.scaled(appContext), 1.0.scaled(appContext)]
                    )
                )
            }

            var leftCorner = CGPoint(
                x: otherStationHorizontalCenter,
                y: verticalCenter - (Double(interchangeData.outOfStationLines.count - 1) * interchangeLineSpacing / 2) - interchangeLineHeight / 2
            )
            for interchange in interchangeData.outOfStationLines {
                context.fill(
                    Path(roundedRect: CGRect(
                            x: leftCorner.x,
                            y: leftCorner.y,
                            width: interchangeLineWidth,
                            height: interchangeLineHeight),
                         cornerSize: CGSize(width: interchangeLineHeight / 2, height: interchangeLineHeight / 2)),
                    with: .color(interchange == "HighSpeed" ? colorInt(0xFF9C948B).asColor() : Operator.Companion().MTR.getColor(routeNumber: interchange, elseColor: 0xFFFFFFFF as Int64).asColor()),
                    outlineMode: ambientMode,
                    outlineWidth: outlineWidth * 0.75
                )
                leftCorner = CGPoint(x: leftCorner.x, y: leftCorner.y + interchangeLineSpacing)
            }

            let circleCenter = CGPoint(x: otherStationHorizontalCenter, y: verticalCenter)
            let heightExpand = max(0.0, Double(interchangeData.outOfStationLines.count - 1) * interchangeLineSpacing)
            context.fill(
                Path(roundedRect: CGRect(
                        x: circleCenter.x - (circleWidth / 1.4),
                        y: circleCenter.y - (circleWidth / 1.4 + heightExpand / 2),
                        width: circleWidth / 1.4 * 2,
                        height: circleWidth / 1.4 * 2 + heightExpand
                    ),
                    cornerSize: CGSize(width: circleWidth / 1.4, height: circleWidth / 1.4)),
                with: .color(colorInt(0xFF003180).asColor().adjustBrightness(percentage: ambientMode ? 1.5 : 1))
            )
            context.fill(
                Path(roundedRect: CGRect(
                        x: circleCenter.x - (circleWidth / 2),
                        y: circleCenter.y - (circleWidth / 2 + heightExpand / 2),
                        width: circleWidth,
                        height: circleWidth + heightExpand
                    ),
                    cornerSize: CGSize(width: circleWidth / 2, height: circleWidth / 2)),
                with: .color(ambientMode ? colorInt(0xFF000000).asColor() : colorInt(0xFFFFFFFF).asColor())
            )
        }
        let circleCenter = CGPoint(x: mainLine != nil ? horizontalCenterPrimary : horizontalCenterSecondary, y: verticalCenter)
        let widthExpand: CGFloat = useSpurStopCircle ? lineWidth : 0
        let heightExpand = max(0.0, Double(interchangeData.lines.count - 1) * interchangeLineSpacing)
        context.fill(
            Path(roundedRect: CGRect(x: circleCenter.x - (circleWidth / 1.4),
                                     y: circleCenter.y - (circleWidth / 1.4 + heightExpand / 2),
                                     width: circleWidth / 1.4 * 2 + widthExpand,
                                     height: circleWidth / 1.4 * 2 + heightExpand),
                 cornerSize: CGSize(width: circleWidth / 1.4, height: circleWidth / 1.4)),
            with: .color(colorInt(0xFF003180).asColor().adjustBrightness(percentage: ambientMode ? 1.5 : 1))
        )
        context.fill(
            Path(roundedRect: CGRect(x: circleCenter.x - (circleWidth / 2),
                                     y: circleCenter.y - (circleWidth / 2 + heightExpand / 2),
                                     width: circleWidth + widthExpand,
                                     height: circleWidth + heightExpand),
                 cornerSize: CGSize(width: circleWidth / 1.4, height: circleWidth / 1.4)),
            with: .color(ambientMode ? colorInt(0xFF000000).asColor() : colorInt(0xFFFFFFFF).asColor())
        )
    }
}

extension GraphicsContext {
    
    func fill(_ path: Path, with shading: GraphicsContext.Shading, style: FillStyle = FillStyle(), outlineMode: Bool, outlineWidth: CGFloat) {
        self.fill(path, with: shading, style: style)
        if outlineMode {
            let boundingBox = path.cgPath.boundingBox
            let scaleFactor = (boundingBox.width - outlineWidth) / boundingBox.width
            let transform = CGAffineTransform(translationX: boundingBox.midX, y: boundingBox.midY)
                .scaledBy(x: scaleFactor, y: scaleFactor)
                .translatedBy(x: -boundingBox.midX, y: -boundingBox.midY)
            self.fill(path.applying(transform), with: .color(colorInt(0xFF000000).asColor()), style: style)
        }
    }

    func stroke(_ path: Path, with shading: GraphicsContext.Shading, style: StrokeStyle, outlineMode: Bool, outlineWidth: CGFloat) {
        self.stroke(path, with: shading, style: style)
        if outlineMode {
            self.stroke(path, with: .color(colorInt(0xFF000000).asColor()), style: StrokeStyle(
                lineWidth: style.lineWidth - outlineWidth,
                lineCap: style.lineCap,
                lineJoin: style.lineJoin,
                miterLimit: style.miterLimit,
                dash: style.dash,
                dashPhase: style.dashPhase
            ))
        }
    }

    func stroke(_ path: Path, with shading: GraphicsContext.Shading, lineWidth: CGFloat = 1, outlineMode: Bool, outlineWidth: CGFloat) {
        self.stroke(path, with: shading, lineWidth: lineWidth)
        if outlineMode {
            self.stroke(path, with: .color(colorInt(0xFF000000).asColor()), lineWidth: lineWidth - outlineWidth)
        }
    }
    
}
