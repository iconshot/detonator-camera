// swift-tools-version: 5.10
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "DetonatorCamera",
    platforms: [
        .iOS(.v13),
    ],
    products: [
        // Products define the executables and libraries a package produces, making them visible to other packages.
        .library(
            name: "DetonatorCamera",
            targets: ["DetonatorCamera"]),
    ],
    dependencies: [
        .package(name: "Detonator", path: "../../../../../node_modules/detonator/ios/Detonator")
    ],
    targets: [
        // Targets are the basic building blocks of a package, defining a module or a test suite.
        // Targets can depend on other targets in this package and products from dependencies.
        .target(
            name: "DetonatorCamera",
            dependencies: ["Detonator"]),
        .testTarget(
            name: "DetonatorCameraTests",
            dependencies: ["DetonatorCamera"]),
    ]
)
