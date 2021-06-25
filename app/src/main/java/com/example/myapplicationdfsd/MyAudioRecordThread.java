package com.example.myapplicationdfsd;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.example.myapplicationdfsd.softWareSystem.service.media.data.AudioData;

import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.WebRtcAudioRecord;

import java.nio.ByteBuffer;

import java.util.concurrent.atomic.AtomicBoolean;

public class MyAudioRecordThread implements Runnable{
    private String TAG = "AudioRecordThread";
    public static WebRtcAudioRecord webRtcAudioRecord;

    // 音频源：音频输入-麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    // 采样率
    // 44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    // 采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
    private final static int AUDIO_SAMPLE_RATE = 44100;
    // 音频通道 单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;

    // 音频格式：PCM编码
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private byte[] buffer;
    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private AudioRecord audioRecord;
    private int recordBufSize;

    private Callback callback;
    private ByteBuffer byteBuffer;
    public volatile boolean keepAlive = true;
    @Override
    public void run() {
        while (true) {
            int bytesRead = audioRecord.read(byteBuffer, byteBuffer.capacity());
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
            if(webRtcAudioRecord.keepAlive){
                if (bytesRead == byteBuffer.capacity()) {
//                    if (webRtcAudioRecord.microphoneMute) {
//                        byteBuffer.clear();
//                        byteBuffer.put(webRtcAudioRecord.emptyBytes);
//                    }
                    // It's possible we've been shut down during the read, and stopRecording() tried and
                    // failed to join this thread. To be a bit safer, try to avoid calling any native methods
                    // in case they've been unregistered after stopRecording() returned.
                    if (keepAlive) {
                        webRtcAudioRecord.nativeDataIsRecorded(webRtcAudioRecord.nativeAudioRecord, bytesRead);
                        // new add begin
//                    if (mWebRtcAudioRecordCallback != null) {
//                        mWebRtcAudioRecordCallback.onWebRtcAudioRecording(byteBuffer,
//                                bytesRead, microphoneMute);
//                    }
                        // new add end
                    }
            }

        }

        }

    }


    interface Callback {
        void inited();
        void recording(AudioData audioData);
        void stop();
        void release();
    }

    public MyAudioRecordThread() {
    }

    public MyAudioRecordThread(Callback callback) {
        this.callback = callback;
    }

    public void init(){
        webRtcAudioRecord = JavaAudioDeviceModule.MyAudioInput;
        byteBuffer = ByteBuffer.allocateDirect(882);
        audioRecord = new AudioRecord.Builder()
                .setAudioSource(7)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(2)
                        .setSampleRate(44100)
                        .setChannelMask(16)
                        .build())
                .setBufferSizeInBytes(8192)
                .build();
webRtcAudioRecord.byteBuffer = byteBuffer;
        audioRecord.startRecording();
        webRtcAudioRecord.audioRecord= audioRecord;

    }


    public void stop(){
        isRecording.set(false);
        audioRecord.stop();
        if(callback!=null){
            callback.stop();
        }
    }

    public void release(){
        if(audioRecord!=null){
            audioRecord.release();
        }
        if(callback!=null){
            callback.release();
        }
    }
}
