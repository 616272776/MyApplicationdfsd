package com.example.myapplicationdfsd.software.service.media.record;

//import org.webrtc.audio.WebRtcAudioRecord;



public class DoorplateAudioMediaRecord extends AbstractAudioMediaRecord {

//    private WebRtcAudioRecord webRtcAudioRecord;
    private static DoorplateAudioMediaRecord mInstance;

    private DoorplateAudioMediaRecord() {
    }

    public static synchronized DoorplateAudioMediaRecord getInstance(){
        if(mInstance==null){
            return new DoorplateAudioMediaRecord();
        }
        return mInstance;
    }


//    @Override
//    public void init(MediaRecordCallBack mediaRecordCallBack) {
//        super.init(mediaRecordCallBack);
//
//        this.mMediaRecordingThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (isRecording.get()) {
//                    recordingStep();
//                    if (webRtcAudioRecord.keepAlive) {
//                        if (bytesRead == byteBuffer.capacity()) {
////                            if (webRtcAudioRecord.microphoneMute) {
////                                byteBuffer.clear();
////                                byteBuffer.put(webRtcAudioRecord.emptyBytes);
////                            }
//                            webRtcAudioRecord.nativeDataIsRecorded(webRtcAudioRecord.nativeAudioRecord, bytesRead);
//                        }
//
//                    }
//                }
//            }
//        });
//
//        webRtcAudioRecord.byteBuffer = byteBuffer;
//        webRtcAudioRecord.audioRecord= audioRecord;
//    }
//
//    public void configWebRtcAudioRecord (WebRtcAudioRecord MyAudioInput){
//        this.webRtcAudioRecord = MyAudioInput;
//
//    }
}
