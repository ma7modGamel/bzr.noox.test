// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "BzrCore",
    products: [
        .library(name: "BzrCore", targets: ["BzrCore"])
    ],
    targets: [
        .target(name: "BzrCore"),
        .testTarget(name: "BzrCoreTests", dependencies: ["BzrCore"]),
    ]
)
