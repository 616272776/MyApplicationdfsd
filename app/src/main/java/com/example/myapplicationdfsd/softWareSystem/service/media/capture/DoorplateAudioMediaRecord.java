package com.example.myapplicationdfsd.softWareSystem.service.media.capture;

import com.example.myapplicationdfsd.softWareSystem.service.media.MediaData;
import com.example.myapplicationdfsd.softWareSystem.service.media.capture.config.AudioRecordConfig;

import org.webrtc.audio.WebRtcAudioRecord;

import androidx.annotation.NonNull;

public class DoorplateAudioMediaRecord extends AudioMediaRecord {

    private WebRtcAudioRecord webRtcAudioRecord;
    private static DoorplateAudioMediaRecord mInstance;

    private DoorplateAudioMediaRecord() {
    }

    public static synchronized DoorplateAudioMediaRecord getInstance(){
        if(mInstance==null){
            return new DoorplateAudioMediaRecord();
        }
        return mInstance;
    }


    public void init(AudioRecordConfig audioRecordConfig,WebRtcAudioRecord webRtcAudioRecord) {
        super.init(audioRecordConfig);
        this.webRtcAudioRecord = webRtcAudioRecord;
        webRtcAudioRecord.byteBuffer = byteBuffer;
        webRtcAudioRecord.audioRecord= audioRecord;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRecording.get()) {
                    recordingStep();
                    if (webRtcAudioRecord.keepAlive) {
                        if (bytesRead == byteBuffer.capacity()) {
                            if (webRtcAudioRecord.microphoneMute) {
                                byteBuffer.clear();
                                byteBuffer.put(webRtcAudioRecord.emptyBytes);
                            }
                            webRtcAudioRecord.nativeDataIsRecorded(webRtcAudioRecord.nativeAudioRecord, bytesRead);
                        }

                    }
                }
            }
        });
        config(thread);
    }




    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void release() {
        super.release();
    }
}
