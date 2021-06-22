package com.example.myapplicationdfsd.softWareSystem.service.media.record;

import android.media.AudioRecord;

import com.example.myapplicationdfsd.softWareSystem.service.media.data.AudioData;

import java.nio.ByteBuffer;

public abstract class AbstractAudioMediaRecord extends AbstractMediaRecord {

    protected AudioRecord audioRecord;
    protected ByteBuffer byteBuffer;
    protected int bytesRead;

    @Override
    public void init(MediaRecordCallBack mediaRecordCallBack){
        super.init(mediaRecordCallBack);
        this.audioRecord.startRecording();
    }

    public void configAudioRecordConfig(AudioRecordConfig audioRecordConfig){
        this.audioRecord = new AudioRecord.Builder()
                .setAudioSource(audioRecordConfig.getAudioSource())
                .setAudioFormat(audioRecordConfig.getAudioFormat())
                .setBufferSizeInBytes(audioRecordConfig.getBufferSizeInBytes())
                .build();
        this.byteBuffer = ByteBuffer.allocateDirect(audioRecordConfig.getBufferSize());
    }

    @Override
    protected void recordingStep(){
        bytesRead = audioRecord.read(byteBuffer, byteBuffer.capacity());
        if(mediaRecordCallBack !=null){
            final ByteBuffer cpBuffer = ByteBuffer.allocateDirect(bytesRead);
            cpBuffer.order(byteBuffer.order());
            cpBuffer.put(byteBuffer.array(), byteBuffer.arrayOffset(), bytesRead);
            cpBuffer.rewind();
            cpBuffer.limit(bytesRead);

            AudioData audioData = new AudioData(cpBuffer, System.nanoTime() / 1000L
                    , bytesRead);
            cpBuffer.clear();
            mediaRecordCallBack.recording(audioData);
        }
    }

    @Override
    public void stop(){
        super.stop();
        if(audioRecord!=null){
            audioRecord.stop();
        }
    }

    @Override
    public void release(){
        super.release();
        if(audioRecord!=null){
            audioRecord.release();
        }
    }
}
