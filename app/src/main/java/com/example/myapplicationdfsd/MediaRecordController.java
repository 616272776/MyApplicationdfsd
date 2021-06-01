package com.example.myapplicationdfsd;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class MediaRecordController {
    private static final String TAG = "MediaRecordController";

    // 录制基本数据
    private int mWidth;
    private int mHeight;
    private int mBitRate;
    private static final int FRAME_RATE = 15;
    private static final int I_FRAME_INTERVAL = 10; // 10 seconds between

    // 音视频编码
    private MediaCodec mVideoCodec;
    private MediaCodec mAudioCodec;
    // 音视频编码类型
    private static final String VIDEO_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final String AUDIO_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;

    private static final long DEQUEUE_TIME_OUT = 100L;


    private LinkedBlockingQueue<AudioData> mAudioOutBufferQueue;
    private LinkedBlockingQueue<AudioData> mAudioInBufferQueue;

    private Surface mSurface;

    private void prepareEncoder() throws IOException {

        mVideoCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
        Log.d(TAG, "created videoCodec");


        MediaFormat videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, mWidth, mHeight);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        Log.d(TAG, "created video format: " + videoFormat);

        mVideoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE/*配置成编码器encode*/);
        Log.d(TAG, "configured videoCodec");

        mSurface = mVideoCodec.createInputSurface();
        Log.d(TAG, "created input surface: " + mSurface);

        mVideoCodec.start();
        Log.d(TAG, "start videoCodec");
    }

    public void startRecord(String path){

    }

    private boolean writeVideoData() {
        MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
        int outIndex = mVideoCodec.dequeueOutputBuffer(videoBufferInfo, DEQUEUE_TIME_OUT);
        return true;
    }
}
