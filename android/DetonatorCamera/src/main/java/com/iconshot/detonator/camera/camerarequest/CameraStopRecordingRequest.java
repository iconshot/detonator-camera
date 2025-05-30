package com.iconshot.detonator.camera.camerarequest;

import com.iconshot.detonator.Detonator;
import com.iconshot.detonator.renderer.Edge;
import com.iconshot.detonator.request.Request;

import com.iconshot.detonator.camera.cameraelement.CameraElement;

public class CameraStopRecordingRequest extends Request {
    public CameraStopRecordingRequest(Detonator detonator, IncomingRequest incomingRequest) {
        super(detonator, incomingRequest);
    }

    @Override
    public void run() {
        Edge edge = getComponentEdge();

        CameraElement element = (CameraElement) edge.children.get(0).element;

        try {
            element.stopRecording();

            end();
        } catch (Exception e) {
            error(e);
        }
    }
}
