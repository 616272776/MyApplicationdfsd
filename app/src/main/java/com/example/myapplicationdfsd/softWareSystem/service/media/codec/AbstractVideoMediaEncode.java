package com.example.myapplicationdfsd.softWareSystem.service.media.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.IOException;

public abstract class AbstractVideoMediaEncode extends AbstractMediaCodec {

    private final static String VIDEO_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private VideoMediaEncodeConfig videoMediaEncodeConfig;

    @Override
    public void init( ) {
        super.init();
        try {
        // 视频格式信息
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, videoMediaEncodeConfig.width, videoMediaEncodeConfig.height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoMediaEncodeConfig.width * videoMediaEncodeConfig.height * 6);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        mediaCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void configVideoMediaEncodeConfig(VideoMediaEncodeConfig videoMediaEncodeConfig){
        this.videoMediaEncodeConfig= videoMediaEncodeConfig;
    }
}
