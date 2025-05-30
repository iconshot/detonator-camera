package com.iconshot.detonator.camera.camerarequest;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.function.Consumer;

public class CameraActivity extends Activity {

    public static Consumer<Boolean> permissionResultCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                100
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        boolean hasGrantedCameraPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        boolean hasGrantedMicrophonePermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;

        boolean permissionsGranted = hasGrantedCameraPermission && hasGrantedMicrophonePermission;

        if (permissionResultCallback != null) {
            permissionResultCallback.accept(permissionsGranted);
        }

        finish();
    }
}