import AVFoundation
import UIKit

import Detonator

class CameraElement: Element, AVCapturePhotoCaptureDelegate, AVCaptureFileOutputRecordingDelegate {
    private let captureSession: AVCaptureSession = AVCaptureSession()
    
    private let previewLayer: AVCaptureVideoPreviewLayer = AVCaptureVideoPreviewLayer()
    
    private let photoOutput: AVCapturePhotoOutput = AVCapturePhotoOutput()
    private let movieOutput: AVCaptureMovieFileOutput = AVCaptureMovieFileOutput()
    
    private var photoCaptureCallback: ((Result<CameraMedia, Error>) -> Void)?
    private var movieCaptureCallback: ((Result<CameraMedia, Error>) -> Void)?
    
    private var currentBack: Bool = false
    
    private var removing: Bool = false
    
    private var audioInput: AVCaptureDeviceInput?
        
    override public func decodeAttributes() -> CameraAttributes? {
        return super.decodeAttributes()
    }
    
    override public func createView() -> CameraView {
        let view = CameraView()
        
        captureSession.usesApplicationAudioSession = true
        captureSession.automaticallyConfiguresApplicationAudioSession = false
        
        if captureSession.canAddOutput(photoOutput) {
            captureSession.addOutput(photoOutput)
        }
        
        if captureSession.canAddOutput(movieOutput) {
            captureSession.addOutput(movieOutput)
        }
        
        previewLayer.session = captureSession
        
        view.previewLayer = previewLayer
        
        return view
    }
    
    override public func patchView() -> Void {
        let attributes = attributes as! CameraAttributes
        let prevAttributes = prevAttributes as! CameraAttributes?
        
        let back = attributes.back
        let prevBack = prevAttributes?.back
        
        let patchBackBool = forcePatch || back != prevBack
        
        if patchBackBool {
            let tmpBack = back ?? false
            
            if forcePatch || tmpBack != currentBack {
                currentBack = tmpBack
                
                startCamera()
            }
        }
    }
    
    private func startCamera() -> Void {
        for input in captureSession.inputs {
            captureSession.removeInput(input)
        }
        
        guard let cameraDevice = AVCaptureDevice.default(
                .builtInWideAngleCamera,
                for: .video,
                position: currentBack ? .back : .front
        ) else {
            return
        }
        
        guard let videoInput = try? AVCaptureDeviceInput(device: cameraDevice) else {
            return
        }
        
        if !captureSession.canAddInput(videoInput) {
            return
        }
        
        captureSession.addInput(videoInput)
        
        DispatchQueue.global(qos: .userInitiated).async {
            self.captureSession.startRunning()
        }
    }
    
    func takePhoto(callback: @escaping (Result<CameraMedia, Error>) -> Void) -> Void {
        if photoCaptureCallback != nil {
            callback(.failure(NSError(domain: "com.iconshot.detonator.camera.cameraelement", code: -1, userInfo: [NSLocalizedDescriptionKey: "Already capturing."])))
            
            return
        }

        photoCaptureCallback = callback
    
        let settings = AVCapturePhotoSettings()
    
        photoOutput.capturePhoto(with: settings, delegate: self)
    }
    
    func photoOutput(
        _ output: AVCapturePhotoOutput,
        didFinishProcessingPhoto photo: AVCapturePhoto,
        error: Error?
    ) {
        let photoCaptureCallback = self.photoCaptureCallback!
        
        self.photoCaptureCallback = nil
        
        if removing {
            cleanup()
        }
        
        if let error = error {
            photoCaptureCallback(.failure(NSError(domain: "com.iconshot.detonator.camera.cameraelement", code: -1, userInfo: [NSLocalizedDescriptionKey: "Unknown error."])))
            
            return
        }
        
        guard let data = photo.fileDataRepresentation() else {
            photoCaptureCallback(.failure(NSError(domain: "com.iconshot.detonator.camera.cameraelement", code: -1, userInfo: [NSLocalizedDescriptionKey: "Bad image data."])))
            
            return
        }
        
        let fileManager = FileManager.default
        
        let cacheDirectory = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first!
        
        let directory = cacheDirectory.appendingPathComponent("com.iconshot.detonator.camera", isDirectory: true)
        
        if !fileManager.fileExists(atPath: directory.path) {
            try? fileManager.createDirectory(at: directory, withIntermediateDirectories: true, attributes: nil)
        }

        let fileName = UUID().uuidString + ".jpg"
        
        let fileURL = directory.appendingPathComponent(fileName)
        
        do {
            try data.write(to: fileURL)

            let image = UIImage(data: data)!
            
            let media = CameraMedia(
                source: "file://\(fileURL.path)",
                width: Int(image.size.width),
                height: Int(image.size.height)
            )

            photoCaptureCallback(.success(media))
        } catch {
            photoCaptureCallback(.failure(error))
        }
    }
    
    public func startRecording(callback: @escaping (Result<CameraMedia, Error>) -> Void) -> Void {
        if movieCaptureCallback != nil {
            callback(.failure(NSError(domain: "com.iconshot.detonator.camera.cameraelement", code: -1, userInfo: [NSLocalizedDescriptionKey: "Already capturing."])))
            
            return
        }
        
        movieCaptureCallback = callback
        
        let fileManager = FileManager.default
        
        let cacheDirectory = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first!
        
        let directory = cacheDirectory.appendingPathComponent("com.iconshot.detonator.camera", isDirectory: true)
        
        if !fileManager.fileExists(atPath: directory.path) {
            try? fileManager.createDirectory(at: directory, withIntermediateDirectories: true, attributes: nil)
        }

        let fileName = UUID().uuidString + ".mov"
        
        let fileURL = directory.appendingPathComponent(fileName)

        DispatchQueue.global(qos: .userInitiated).async {
            let audioSession = AVAudioSession.sharedInstance()
            
            try? audioSession.setCategory(.playAndRecord, mode: .videoRecording, options: [.allowBluetoothA2DP, .mixWithOthers])
            try? audioSession.setActive(true)
            
            self.addAudioInput()
            
            self.movieOutput.startRecording(to: fileURL, recordingDelegate: self)
        }
    }
    
    public func stopRecording() throws -> Void {
        if movieCaptureCallback == nil {
            throw NSError(domain: "com.iconshot.detonator.camera.cameraelement", code: -1, userInfo: [NSLocalizedDescriptionKey: "No active recording to stop."])
        }
        
        DispatchQueue.global(qos: .userInitiated).async {
            self.stopCameraRecording()
        }
    }
    
    private func stopCameraRecording() -> Void {
        movieOutput.stopRecording()
        
        let audioSession = AVAudioSession.sharedInstance()
        
        try? audioSession.setActive(false)
        
        removeAudioInput()
    }
    
    private func addAudioInput() -> Void {
        guard let microphoneDevice = AVCaptureDevice.default(for: .audio) else {
            return
        }
        
        guard let audioInput = try? AVCaptureDeviceInput(device: microphoneDevice) else {
            return
        }
        
        self.audioInput = audioInput
        
        if !captureSession.canAddInput(audioInput) {
            return
        }
        
        captureSession.beginConfiguration()
        
        captureSession.addInput(audioInput)
        
        captureSession.commitConfiguration()
    }
    
    private func removeAudioInput() -> Void {
        guard let audioInput = audioInput else {
            return
        }
        
        captureSession.beginConfiguration()
        
        captureSession.removeInput(audioInput)
        
        captureSession.commitConfiguration()
        
        self.audioInput = nil
    }
    
    func fileOutput(
        _ output: AVCaptureFileOutput,
        didFinishRecordingTo outputFileURL: URL,
        from connections: [AVCaptureConnection],
        error: Error?
    ) {
        let movieCaptureCallback = self.movieCaptureCallback!
        
        self.movieCaptureCallback = nil
        
        if removing {
            cleanup()
        }
        
        if let error = error {
            movieCaptureCallback(.failure(NSError(domain: "com.iconshot.detonator.camera.cameraelement", code: -1, userInfo: [NSLocalizedDescriptionKey: "Unknown error."])))
            
            return
        }
        
        let asset = AVAsset(url: outputFileURL)
        
        guard let track = asset.tracks(withMediaType: .video).first else {
            movieCaptureCallback(.failure(NSError(domain: "com.iconshot.detonator.camera.cameraelement", code: -1, userInfo: [NSLocalizedDescriptionKey: "Error reading video asset."])))
            
            return
        }
        
        let size = track.naturalSize.applying(track.preferredTransform)
        
        let width = Int(abs(size.width))
        let height = Int(abs(size.height))

        let media = CameraMedia(
            source: "file://\(outputFileURL.path)",
            width: width,
            height: height
        )
        
        movieCaptureCallback(.success(media))
    }
    
    private func cleanup() -> Void {
        if photoCaptureCallback != nil {
            return
        }
        
        if movieCaptureCallback != nil {
            return
        }
                
        if captureSession.isRunning {
            captureSession.stopRunning()
        }

        for input in captureSession.inputs {
            captureSession.removeInput(input)
        }

        for output in captureSession.outputs {
            captureSession.removeOutput(output)
        }

        DispatchQueue.main.async {
            self.previewLayer.session = nil
        }
    }
    
    override func removeView() -> Void {
        removing = true
                
        if movieOutput.isRecording {
            DispatchQueue.global(qos: .userInitiated).async {
                self.stopCameraRecording()
            }
        }
        
        cleanup()
    }
    
    class CameraAttributes: Attributes {
        var back: Bool?
        
        required init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            
            back = try container.decodeIfPresent(Bool.self, forKey: .back)
            
            try super.init(from: decoder)
        }
        
        private enum CodingKeys: String, CodingKey {
            case back
        }
    }
}
 
