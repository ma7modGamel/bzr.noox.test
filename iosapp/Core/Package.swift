// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "BzrCore",
    // @Observable needs iOS 17 / macOS 14 (owner decision 2026-10-04); Linux has no availability gate.
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "BzrCore", targets: ["BzrCore"])
    ],
    targets: [
        .target(name: "BzrCore"),
        .testTarget(name: "BzrCoreTests", dependencies: ["BzrCore"]),
    ]
)
