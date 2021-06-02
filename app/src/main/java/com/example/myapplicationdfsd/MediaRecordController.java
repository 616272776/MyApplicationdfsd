package com.example.myapplicationdfsd;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
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
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        Log.d(TAG, "created video format: " + videoFormat);

        mVideoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE/*配置成编码器encode*/);
        Log.d(TAG, "configured videoCodec");

//        mSurface = mVideoCodec.createInputSurface();
//        Log.d(TAG, "created input surface: " + mSurface);

        mVideoCodec.start();
        Log.d(TAG, "start videoCodec");
    }

    public void startRecord(String path){

    }

    private void encode(byte[] yuv, long presentationTimeUs) {
        // 一、给编码器设置一帧输入数据
        // 1.获取一个可用的输入buffer，最大等待时长为DEFAULT_TIMEOUT_US
        int inputBufferIndex = mVideoCodec.dequeueInputBuffer(DEQUEUE_TIME_OUT);


        ByteBuffer inputBuffer = mVideoCodec.getInputBuffer(inputBufferIndex);
        // 2.将输入数据放到buffer中
        inputBuffer.put(yuv);
        // 3.将buffer压入解码队列中，即编码线程就会处理队列中的数据了
        mVideoCodec.queueInputBuffer(inputBufferIndex, 0, yuv.length, presentationTimeUs, 0);

        // 二、从编码器中取出一帧编码后的输出数据
        // 1.获取一个可用的输出buffer，最大等待时长为DEFAULT_TIMEOUT_US
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIME_OUT);
        ByteBuffer outputBuffer = mVideoCodec.getOutputBuffer(outputBufferIndex);
        // 2.TODO MediaMuxer将编码数据写入到mp4中
        // 3.用完后释放这个输出buffer
        mVideoCodec.releaseOutputBuffer(outputBufferIndex, false);
    }


//    private boolean writeVideoData() {
//        MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
//        int outIndex = mVideoCodec.dequeueOutputBuffer(videoBufferInfo, DEQUEUE_TIME_OUT);
//        return true;
//    }
}
