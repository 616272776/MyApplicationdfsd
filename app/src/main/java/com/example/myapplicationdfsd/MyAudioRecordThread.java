package com.example.myapplicationdfsd;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import org.webrtc.Logging;
import org.webrtc.audio.WebRtcAudioRecord;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyAudioRecordThread implements Runnable{
    private String TAG = "AudioRecordThread";


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

    @Override
    public void run() {

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

    public AudioRecord init(){
        return audioRecord = createAudioRecordOnMOrHigher(
                7, 44100, 16, 2, 8192);

    }
    public void startRecording(){
        audioRecord.startRecording();
    }
    private static AudioRecord createAudioRecordOnMOrHigher(
            int audioSource, int sampleRate, int channelConfig, int audioFormat, int bufferSizeInBytes) {
        return new AudioRecord.Builder()
                .setAudioSource(audioSource)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build())
                .setBufferSizeInBytes(bufferSizeInBytes)
                .build();
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
