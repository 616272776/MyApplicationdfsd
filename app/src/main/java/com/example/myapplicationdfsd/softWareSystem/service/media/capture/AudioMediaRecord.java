package com.example.myapplicationdfsd.softWareSystem.service.media.capture;

import android.media.AudioRecord;

import com.example.myapplicationdfsd.softWareSystem.service.media.capture.config.AudioRecordConfig;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;



import androidx.annotation.NonNull;

public abstract class AudioMediaRecord implements MediaRecord{
    private String TAG = "AudioMediaCapture";
    protected MediaRecord.Callback callback;

    protected AudioRecord audioRecord;
    protected ByteBuffer byteBuffer;
    protected AtomicBoolean isRecording = new AtomicBoolean(false);

    protected Thread mAudioMediaRecordingThread;
    protected int bytesRead;

    public void init(AudioRecordConfig audioRecordConfig){
        audioRecord = new AudioRecord.Builder()
                .setAudioSource(audioRecordConfig.getAudioSource())
                .setAudioFormat(audioRecordConfig.getAudioFormat())
                .setBufferSizeInBytes(audioRecordConfig.getBufferSizeInBytes())
                .build();
        byteBuffer = ByteBuffer.allocateDirect(audioRecordConfig.getBufferSize());
        audioRecord.startRecording();

        if(callback!=null){
            callback.init();
        }
    }
    protected void config(@NonNull Runnable AudioMediaRecordingThread){
        this.mAudioMediaRecordingThread  = new Thread(AudioMediaRecordingThread);
    }
    public void record(){
        if(this.mAudioMediaRecordingThread==null){
            throw new IllegalArgumentException("audioMediaRecordingThread can't be null");
        }
        mAudioMediaRecordingThread.start();
        isRecording.set(true);
    }
    protected void recordingStep(){
        bytesRead = audioRecord.read(byteBuffer, byteBuffer.capacity());
        if(callback!=null){
            final ByteBuffer cpBuffer = ByteBuffer.allocateDirect(bytesRead);
            cpBuffer.order(byteBuffer.order());
            cpBuffer.put(byteBuffer.array(), byteBuffer.arrayOffset(), bytesRead);
            cpBuffer.rewind();
            cpBuffer.limit(bytesRead);

            AudioData audioData = new AudioData(cpBuffer, System.nanoTime() / 1000L
                    , bytesRead);
            cpBuffer.clear();
            callback.recording(audioData);
        }
    }
    public void stop(){
        isRecording.set(false);
        if(callback!=null){
            callback.stop();
        }
        if(audioRecord!=null){
            audioRecord.stop();
        }
    }
    public void release(){
        stop();
        if(callback!=null){
            callback.init();
        }
        if(audioRecord!=null){
            audioRecord.release();
        }
    }



}
