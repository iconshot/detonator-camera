import Detonator

class CameraStartRecordingRequest: Request {
    override func run() -> Void {
        let edge = getComponentEdge()
        
        let element = edge!.children[0].element as! CameraElement
        
        element.startRecording() { result in
            switch result {
            case .success(let media):
                self.end(data: media)
            case .failure(let error):
                self.error(error: error)
            }
        }
    }
}
