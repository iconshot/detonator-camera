package com.iconshot.detonator.camera.cameraelement;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import com.iconshot.detonator.Detonator;
import com.iconshot.detonator.camera.CameraMedia;
import com.iconshot.detonator.element.Element;
import com.iconshot.detonator.helpers.CompareHelper;
import com.iconshot.detonator.helpers.ContextHelper;
import com.iconshot.detonator.layout.ViewLayout;

public class CameraElement extends Element<CameraView, CameraElement.Attributes> {
    private ProcessCameraProvider cameraProvider;

    private PreviewView previewView;

    private Preview preview;

    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;

    private Recording activeRecording;

    private boolean currentBack = false;

    public CameraElement(Detonator detonator) {
        super(detonator);
    }

    @Override
    protected Class<Attributes> getAttributesClass() { return Attributes.class; }

    @Override
    protected CameraView createView() {
        Context context = ContextHelper.context;

        CameraView cameraView = new CameraView(context);

        previewView = new PreviewView(context);

        cameraView.addView(previewView);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                startCamera();
            } catch (Exception e) {}
        }, ContextCompat.getMainExecutor(context));

        return cameraView;
    }

    @Override
    protected void patchView() {
        Boolean back = attributes.back;
        Boolean prevBack = prevAttributes != null ? prevAttributes.back : null;

        boolean patchBack = forcePatch || !CompareHelper.compareObjects(back, prevBack);

        if (patchBack) {
            boolean tmpBack = back != null && back;

            if (tmpBack != currentBack) {
                currentBack = tmpBack;

                startCamera();
            }
        }
    }

    protected void patchBackgroundColor(
            Object backgroundColor,
            Object borderRadius,
            Object borderRadiusTopLeft,
            Object borderRadiusTopRight,
            Object borderRadiusBottomLeft,
            Object borderRadiusBottomRight
    ) {
        super.patchBackgroundColor(
                backgroundColor,
                borderRadius,
                borderRadiusTopLeft,
                borderRadiusTopRight,
                borderRadiusBottomLeft,
                borderRadiusBottomRight
        );

        ViewLayout.LayoutParams layoutParams = (ViewLayout.LayoutParams) view.getLayoutParams();

        layoutParams.onLayoutClosures.put("borderRadius", () -> {
            float[] radii = getRadii(
                    borderRadius,
                    borderRadiusTopLeft,
                    borderRadiusTopRight,
                    borderRadiusBottomLeft,
                    borderRadiusBottomRight
            );

            view.setRadii(radii);
        });
    }

    protected void patchObjectFit(String objectFit) {
        String tmpObjectFit = objectFit != null ? objectFit : "cover";

        switch (tmpObjectFit) {
            case "cover": {
                previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);

                break;
            }

            case "contain": {
                previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);

                break;
            }

            case "fill": {
                previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);

                break;
            }
        }
    }

    private void startCamera() {
        if (cameraProvider == null) {
            return;
        }

        Context context = ContextHelper.context;

        if (!(context instanceof LifecycleOwner)) {
            return;
        }

        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        int rotation = display.getRotation();

        preview = new Preview.Builder()
                .setTargetRotation(rotation)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setTargetRotation(rotation)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        Recorder recorder = new Recorder.Builder()
                .setExecutor(ContextCompat.getMainExecutor(context))
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build();

        videoCapture = VideoCapture.withOutput(recorder);

        int lens = currentBack ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT;

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lens)
                .build();

        try {
            cameraProvider.unbindAll();

            cameraProvider.bindToLifecycle(
                    (LifecycleOwner) context,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture
            );
        } catch (Exception e) {}
    }

    public void takePhoto(CaptureCallback callback) {
        if (imageCapture == null) {
            callback.onError(new Exception("imageCapture is null."));

            return;
        }

        Context context = ContextHelper.context;

        File directory = new File(context.getCacheDir(), "com.iconshot.detonator.camera");

        String fileName = UUID.randomUUID().toString() + ".jpg";

        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, fileName);

        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(file)
                .build();

        ImageCapture.OnImageSavedCallback onImageSavedCallback = new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                String path = output.getSavedUri().getPath();

                try {
                    CameraMedia media = processPhoto(path);

                    callback.onEnd(media);
                } catch (Exception e) {
                    callback.onError(new Exception("Error processing photo."));
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                callback.onError(exception);
            }
        };

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(context), onImageSavedCallback);
    }

    private CameraMedia processPhoto(String originalPath) throws IOException {
        File originalFile = new File(originalPath);

        // read orientation

        ExifInterface exif = new ExifInterface(originalPath);

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        Matrix matrix = new Matrix();

        // assign rotation

        int rotateValue = 0;

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: {
                rotateValue = 90;

                break;
            }

            case ExifInterface.ORIENTATION_ROTATE_180: {
                rotateValue = 180;

                break;
            }

            case ExifInterface.ORIENTATION_ROTATE_270: {
                rotateValue = 270;

                break;
            }
        }

        if (rotateValue != 0) {
            matrix.postRotate(rotateValue);
        }

        // decode and rotate

        Bitmap originalBitmap = BitmapFactory.decodeFile(originalPath);

        Bitmap processedBitmap = Bitmap.createBitmap(
                originalBitmap,
                0, 0,
                originalBitmap.getWidth(),
                originalBitmap.getHeight(),
                matrix,
                true
        );

        // save processed photo to cache

        File processedDirectory = originalFile.getParentFile();

        String processedFileName = "p_" + originalFile.getName();

        File processedFile = new File(processedDirectory, processedFileName);

        FileOutputStream out = new FileOutputStream(processedFile);

        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

        // delete original

        originalFile.delete();

        // create CameraMedia

        CameraMedia media = new CameraMedia();

        media.source = "file://" + processedFile.getAbsolutePath();
        media.width = processedBitmap.getWidth();
        media.height = processedBitmap.getHeight();

        // clean up

        out.flush();
        out.close();

        originalBitmap.recycle();
        processedBitmap.recycle();

        return media;
    }

    @SuppressLint("MissingPermission")
    public void startRecording(CaptureCallback callback) {
        if (videoCapture == null) {
            callback.onError(new Exception("videoCapture is null."));

            return;
        }

        if (activeRecording != null) {
            callback.onError(new Exception("A recording is already in progress."));

            return;
        }

        Context context = ContextHelper.context;

        File directory = new File(context.getCacheDir(), "com.iconshot.detonator.camera");

        String fileName = UUID.randomUUID().toString() + ".mp4";

        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, fileName);

        FileOutputOptions options = new FileOutputOptions.Builder(file).build();

        Consumer<VideoRecordEvent> listener = videoRecordEvent -> {
            if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                VideoRecordEvent.Finalize finalize = (VideoRecordEvent.Finalize) videoRecordEvent;

                if (finalize.hasError()) {
                    callback.onError(new Exception("Error recording from camera."));

                    return;
                }

                String path = finalize.getOutputResults().getOutputUri().getPath();

                CameraMedia media = new CameraMedia();

                media.source = "file://" + path;

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();

                try {
                    retriever.setDataSource(path);

                    String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    String rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);

                    int width = Integer.parseInt(widthStr);
                    int height = Integer.parseInt(heightStr);
                    int rotation = Integer.parseInt(rotationStr);

                    if (rotation == 90 || rotation == 270) {
                        media.width = height;
                        media.height = width;
                    } else {
                        media.width = width;
                        media.height = height;
                    }

                    callback.onEnd(media);
                } catch (Exception exception) {
                    callback.onError(exception);
                } finally {
                    try {
                        retriever.release();
                    } catch (Exception e) {}
                }
            }
        };

        activeRecording = videoCapture.getOutput()
                .prepareRecording(context, options)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context), listener);
    }

    public void stopRecording() throws Exception {
        if (activeRecording == null) {
            throw new Exception("No active recording to stop.");
        }

        activeRecording.stop();

        activeRecording = null;
    }

    @Override
    protected void removeView() {
        if (activeRecording != null) {
            activeRecording.stop();
        }

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        activeRecording = null;
        cameraProvider = null;
        imageCapture = null;
        videoCapture = null;
    }

    protected static class Attributes extends Element.Attributes {
        Boolean back;
    }

    public interface CaptureCallback {
        void onEnd(CameraMedia media);
        void onError(Exception exception);
    }
}
