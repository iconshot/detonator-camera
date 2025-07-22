import AVFoundation

import Detonator

class CameraRequestPermissionsRequest: Request {
    public override func run() -> Void {
        let cameraStatus = AVCaptureDevice.authorizationStatus(for: .video)
        let microphoneStatus = AVCaptureDevice.authorizationStatus(for: .audio)

        switch (cameraStatus, microphoneStatus) {
        case (.authorized, .authorized):
            end(data: true)

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
                
                self.end(data: granted)
            }

        case (.denied, _), (_, .denied), (.restricted, _), (_, .restricted):
            end(data: false)

        @unknown default:
            end(data: false)
        }
    }
}
