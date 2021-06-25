package com.example.myapplicationdfsd.software.service.media.record;

import android.media.AudioFormat;



public class AudioRecordConfig {

    private int BufferSizeInBytes;
    private Integer BufferSize;
    private Integer AudioSource;
    private AudioFormat audioFormat;

    public AudioRecordConfig(int bufferSizeInBytes, Integer bufferSize, Integer audioSource, AudioFormat audioFormat) {
        BufferSizeInBytes = bufferSizeInBytes;
        BufferSize = bufferSize;
        AudioSource = audioSource;
        this.audioFormat = audioFormat;
    }

    public int getBufferSizeInBytes() {
        return BufferSizeInBytes;
    }

    public void setBufferSizeInBytes(int bufferSizeInBytes) {
        BufferSizeInBytes = bufferSizeInBytes;
    }

    public Integer getBufferSize() {
        return BufferSize;
    }

    public void setBufferSize(Integer bufferSize) {
        BufferSize = bufferSize;
    }

    public Integer getAudioSource() {
        return AudioSource;
    }

    public void setAudioSource(Integer audioSource) {
        AudioSource = audioSource;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public void setAudioFormat(AudioFormat audioFormat) {
        this.audioFormat = audioFormat;
    }
}
