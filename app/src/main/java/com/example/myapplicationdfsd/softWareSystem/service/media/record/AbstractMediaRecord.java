package com.example.myapplicationdfsd.softWareSystem.service.media.record;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractMediaRecord {

    protected AtomicBoolean isRecording = new AtomicBoolean(false);
    protected MediaRecordCallBack mediaRecordCallBack;
    protected Thread mMediaRecordingThread;

    public Boolean isRecording(){
        return isRecording.get();
    }

    protected abstract void recordingStep();

    public void init(MediaRecordCallBack mediaRecordCallBack){
        this.mediaRecordCallBack = mediaRecordCallBack;
        if(mediaRecordCallBack!=null){
            mediaRecordCallBack.init();
        }
    }
    public void record(){
        if(this.mMediaRecordingThread ==null){
            throw new IllegalArgumentException("audioMediaRecordingThread can't be null");
        }
        isRecording.set(true);
        mMediaRecordingThread.start();
        if(mediaRecordCallBack !=null){
            mediaRecordCallBack.record();
        }
    }

    public void stop(){
        isRecording.set(false);
        if(mediaRecordCallBack !=null){
            mediaRecordCallBack.stop();
        }
    }
    public void release(){
        stop();
        if(mediaRecordCallBack !=null){
            mediaRecordCallBack.release();
        }
    }
}
