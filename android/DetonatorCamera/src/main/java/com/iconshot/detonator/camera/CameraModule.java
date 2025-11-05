package com.iconshot.detonator.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import com.iconshot.detonator.Detonator;
import com.iconshot.detonator.module.Module;

import com.iconshot.detonator.camera.cameraelement.CameraElement;

public class CameraModule extends Module {
    public CameraModule(Detonator detonator) {
        super(detonator);
    }

    @Override
    public void setUp() {
        detonator.setElementClass("com.iconshot.detonator.camera", CameraElement.class);

        detonator.setRequestListener("com.iconshot.detonator.camera::requestPermissions", (promise, value, edge) -> {
            boolean hasCameraPermission = ContextCompat.checkSelfPermission(
                    detonator.context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED;

            boolean hasMicrophonePermission = ContextCompat.checkSelfPermission(
                    detonator.context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED;

            boolean permissionsGranted = hasCameraPermission && hasMicrophonePermission;

            if (permissionsGranted) {
                promise.resolve(true);

                return;
            }

            CameraActivity.permissionResultCallback = granted -> {
                promise.resolve(granted);
            };

            detonator.context.startActivity(new Intent(detonator.context, CameraActivity.class));
        });
    }
}
