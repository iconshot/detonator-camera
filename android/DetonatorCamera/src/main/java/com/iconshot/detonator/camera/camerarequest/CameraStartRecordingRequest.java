package com.iconshot.detonator.camera.camerarequest;

import com.iconshot.detonator.Detonator;
import com.iconshot.detonator.camera.CameraMedia;
import com.iconshot.detonator.renderer.Edge;
import com.iconshot.detonator.request.Request;

import com.iconshot.detonator.camera.cameraelement.CameraElement;

public class CameraStartRecordingRequest extends Request {
    public CameraStartRecordingRequest(Detonator detonator, IncomingRequest incomingRequest) {
        super(detonator, incomingRequest);
    }

    @Override
    public void run() {
        Edge edge = getComponentEdge();

        CameraElement element = (CameraElement) edge.children.get(0).element;

        element.startRecording(new CameraElement.CaptureCallback() {
            public void onEnd(CameraMedia media) {
                end(media);
            }

            public void onError(Exception exception) {
                error(exception);
            }
        });
    }
}
