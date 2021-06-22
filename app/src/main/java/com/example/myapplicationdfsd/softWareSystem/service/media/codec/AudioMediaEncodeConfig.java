package com.example.myapplicationdfsd.softWareSystem.service.media.codec;

public class AudioMediaEncodeConfig implements MediaCodecConfig {
    private int mAudioSampleRate= 44100;
    private int mAudioChannels = 1;
    private int mAudioBufferSize = 8192;
    private int mAudioBitsPerSample =10;

    public int getmAudioSampleRate() {
        return mAudioSampleRate;
    }

    public void setmAudioSampleRate(int mAudioSampleRate) {
        this.mAudioSampleRate = mAudioSampleRate;
    }

    public int getmAudioChannels() {
        return mAudioChannels;
    }

    public void setmAudioChannels(int mAudioChannels) {
        this.mAudioChannels = mAudioChannels;
    }

    public int getmAudioBufferSize() {
        return mAudioBufferSize;
    }

    public void setmAudioBufferSize(int mAudioBufferSize) {
        this.mAudioBufferSize = mAudioBufferSize;
    }

    public int getmAudioBitsPerSample() {
        return mAudioBitsPerSample;
    }

    public void setmAudioBitsPerSample(int mAudioBitsPerSample) {
        this.mAudioBitsPerSample = mAudioBitsPerSample;
    }
}
