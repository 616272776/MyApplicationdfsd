package com.example.myapplicationdfsd.softWareSystem.service.media.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.IOException;

public abstract class AbstractAudioMediaEncode extends AbstractMediaCodec {

    private static final String AUDIO_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    private AudioMediaEncodeConfig audioMediaEncodeConfig;


    @Override
    public void init() {
        super.init();
        try {
            mediaFormat = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, audioMediaEncodeConfig.getmAudioSampleRate(), audioMediaEncodeConfig.getmAudioChannels());
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioMediaEncodeConfig.getmAudioBitsPerSample() * audioMediaEncodeConfig.getmAudioSampleRate() * 4);
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, audioMediaEncodeConfig.getmAudioBufferSize());
            mediaCodec = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void configAudioMediaEncodeConfig(AudioMediaEncodeConfig audioMediaEncodeConfig){
        this.audioMediaEncodeConfig= audioMediaEncodeConfig;
    }



}
