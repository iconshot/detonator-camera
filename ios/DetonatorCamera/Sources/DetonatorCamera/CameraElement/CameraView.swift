import UIKit
import AVFoundation

public class CameraView: UIView {
    public var previewLayer: AVCaptureVideoPreviewLayer? {
        didSet {
            if let previewLayer = previewLayer {
                layer.addSublayer(previewLayer)
            }
        }
    }
    
    override public func layoutSubviews() {
        previewLayer?.frame = bounds
    }
}
