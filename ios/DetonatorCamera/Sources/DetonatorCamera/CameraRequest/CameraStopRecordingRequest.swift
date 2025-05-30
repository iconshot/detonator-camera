import Detonator

class CameraStopRecordingRequest: Request {
    override func run() -> Void {
        let edge = getComponentEdge()
        
        let element = edge!.children[0].element as! CameraElement
        
        do {
            try element.stopRecording()
            
            end()
        } catch {
            self.error(error: error)
        }
    }
}
