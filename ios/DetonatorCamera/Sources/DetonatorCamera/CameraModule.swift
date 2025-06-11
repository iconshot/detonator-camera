import Detonator

public class CameraModule: Module {
    override public func register() -> Void {
        detonator.setElementClass("com.iconshot.detonator.camera", CameraElement.self)
        
        detonator.setRequestClass("com.iconshot.detonator.camera::requestPermissions", CameraRequestPermissionsRequest.self)
        detonator.setRequestClass("com.iconshot.detonator.camera::takePhoto", CameraTakePhotoRequest.self)
        detonator.setRequestClass("com.iconshot.detonator.camera::startRecording", CameraStartRecordingRequest.self)
        detonator.setRequestClass("com.iconshot.detonator.camera::stopRecording", CameraStopRecordingRequest.self)
    }
}
