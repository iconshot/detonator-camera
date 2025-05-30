package com.iconshot.detonator.camera.cameraelement;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
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
import com.iconshot.detonator.helpers.ContextHelper;

import java.io.File;

public class CameraView extends ViewGroup {
    private float[] radii;

    private final RectF rect = new RectF(0, 0, 0, 0);

    private final Path path = new Path();

    public CameraView(Context context) {
        super(context);
    }

    public void setRadii(float[] radii) {
        this.radii = radii;

        this.invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specWidth = MeasureSpec.getSize(widthMeasureSpec);
        int specHeight = MeasureSpec.getSize(heightMeasureSpec);

        View child = getChildAt(0);

        int childWidth = specWidth;
        int childHeight = specHeight;

        child.measure(
                MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
        );

        setMeasuredDimension(specWidth, specHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();

        View child = getChildAt(0);

        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();

        int childLeft = (measuredWidth - childWidth) / 2;
        int childTop = (measuredHeight - childHeight) / 2;

        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int save = canvas.save();

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        path.reset();

        rect.set(0, 0, viewWidth, viewHeight);

        path.addRoundRect(rect, radii, Path.Direction.CW);

        canvas.clipPath(path);

        super.dispatchDraw(canvas);

        canvas.restoreToCount(save);
    }
}