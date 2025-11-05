import AVFoundation

import Detonator

public class CameraModule: Module {
    override public func setUp() -> Void {
        detonator.setElementClass("com.iconshot.detonator.camera", CameraElement.self)
        
        detonator.setRequestListener("com.iconshot.detonator.camera::requestPermissions") { promise, value, edge in
            let cameraStatus = AVCaptureDevice.authorizationStatus(for: .video)
            let microphoneStatus = AVCaptureDevice.authorizationStatus(for: .audio)

            switch (cameraStatus, microphoneStatus) {
            case (.authorized, .authorized):
                promise.resolve(true)

            case (.notDetermined, _), (_, .notDetermined):
                let group = DispatchGroup()

                var cameraGranted = false
                var microphoneGranted = false

                if cameraStatus == .notDetermined {
                    group.enter()
                    
                    AVCaptureDevice.requestAccess(for: .video) { granted in
                        cameraGranted = granted
                        
                        group.leave()
                    }
                } else {
                    cameraGranted = (cameraStatus == .authorized)
                }

                if microphoneStatus == .notDetermined {
                    group.enter()
                    
                    AVCaptureDevice.requestAccess(for: .audio) { granted in
                        microphoneGranted = granted
                        
                        group.leave()
                    }
                } else {
                    microphoneGranted = (microphoneStatus == .authorized)
                }

                group.notify(queue: .main) {
                    let granted = cameraGranted && microphoneGranted
                    
                    promise.resolve(granted)
                }

            case (.denied, _), (_, .denied), (.restricted, _), (_, .restricted):
                promise.resolve(false)

            @unknown default:
                promise.resolve(false)
            }
        }
    }
}
