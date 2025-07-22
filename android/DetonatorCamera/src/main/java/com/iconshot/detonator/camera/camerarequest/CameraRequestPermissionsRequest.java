package com.iconshot.detonator.camera.camerarequest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import com.iconshot.detonator.Detonator;
import com.iconshot.detonator.request.Request;

public class CameraRequestPermissionsRequest extends Request {
    public CameraRequestPermissionsRequest(Detonator detonator, IncomingRequest incomingRequest) {
        super(detonator, incomingRequest);
    }

    @Override
    public void run() {
        Context context = detonator.context;

        boolean hasCameraPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;

        boolean hasMicrophonePermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;

        boolean permissionsGranted = hasCameraPermission && hasMicrophonePermission;

        if (permissionsGranted) {
            end(true);

            return;
        }

        CameraActivity.permissionResultCallback = granted -> {
            end(granted);
        };

        context.startActivity(new Intent(context, CameraActivity.class));
    }
}
