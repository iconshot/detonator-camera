package com.iconshot.detonator.camera;

import com.iconshot.detonator.Detonator;
import com.iconshot.detonator.module.Module;

import com.iconshot.detonator.camera.cameraelement.CameraElement;
import com.iconshot.detonator.camera.camerarequest.CameraRequestPermissionsRequest;
import com.iconshot.detonator.camera.camerarequest.CameraStartRecordingRequest;
import com.iconshot.detonator.camera.camerarequest.CameraStopRecordingRequest;
import com.iconshot.detonator.camera.camerarequest.CameraTakePhotoRequest;

public class CameraModule extends Module {
    public CameraModule(Detonator detonator) {
        super(detonator);
    }

    @Override
    public void setUp() {
        detonator.setElementClass("com.iconshot.detonator.camera", CameraElement.class);

        detonator.setRequestClass("com.iconshot.detonator.camera::requestPermissions", CameraRequestPermissionsRequest.class);
        detonator.setRequestClass("com.iconshot.detonator.camera::takePhoto", CameraTakePhotoRequest.class);
        detonator.setRequestClass("com.iconshot.detonator.camera::startRecording", CameraStartRecordingRequest.class);
        detonator.setRequestClass("com.iconshot.detonator.camera::stopRecording", CameraStopRecordingRequest.class);
    }
}
