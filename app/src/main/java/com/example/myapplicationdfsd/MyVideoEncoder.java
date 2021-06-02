package com.example.myapplicationdfsd;

import android.app.assist.AssistStructure;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import org.webrtc.Logging;
import org.webrtc.VideoCodecStatus;
import org.webrtc.VideoFrame;
import org.webrtc.YuvHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MyVideoEncoder {

    private static final String TAG = "VideoEncoder";

    private final static String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final long DEFAULT_TIMEOUT_US = 10000;

    private MediaCodec mEncoder;
    private MediaMuxer mMediaMuxer;
    private int mVideoTrackIndex;
    private boolean mStop = false;

    public void init(String outPath, int width, int height) {
        try {
            mStop = false;
            mVideoTrackIndex = -1;
            mMediaMuxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);

            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 6);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mEncoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void release() {
        mStop = true;
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mMediaMuxer != null) {
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }

    public void encode(VideoFrame videoFrame) {
        if (mEncoder == null || mMediaMuxer == null) {
            Log.e(TAG, "mEncoder or mMediaMuxer is null");
            return;
        }
//        if (yuv == null) {
//            Log.e(TAG, "input yuv data is null");
//            return;
//        }
        int inputBufferIndex = mEncoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
        Log.d(TAG, "inputBufferIndex: " + inputBufferIndex);
        if (inputBufferIndex == -1) {
            Log.e(TAG, "no valid buffer available");
            return;
        }

        ByteBuffer inputBuffer = mEncoder.getInputBuffer(inputBufferIndex);
        VideoFrame.I420Buffer i420 = videoFrame.getBuffer().toI420();
        YuvHelper.I420ToNV12(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(),
                i420.getDataV(), i420.getStrideV(), inputBuffer, i420.getWidth(), i420.getHeight());
        i420.release();

        mEncoder.queueInputBuffer(inputBufferIndex, 0, videoFrame.getBuffer().getHeight() * videoFrame.getBuffer().getWidth() * 3 / 2, videoFrame.getTimestampNs()/1000, 0);
        while (!mStop) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mEncoder.dequeueOutputBuffer(bufferInfo, DEFAULT_TIMEOUT_US);
            Log.d(TAG, "outputBufferIndex: " + outputBufferIndex);
            if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mEncoder.getOutputBuffer(outputBufferIndex);
                // write head info
                if (mVideoTrackIndex == -1) {
                    Log.d(TAG, "this is first frame, call writeHeadInfo first");
                    mVideoTrackIndex = writeHeadInfo(outputBuffer, bufferInfo);
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    Log.d(TAG, "write outputBuffer");
                    mMediaMuxer.writeSampleData(mVideoTrackIndex, outputBuffer, bufferInfo);
                }
                mEncoder.releaseOutputBuffer(outputBufferIndex, false);
                break; // 跳出循环
            }
        }
    }

    private int writeHeadInfo(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        byte[] csd = new byte[bufferInfo.size];
        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
        outputBuffer.position(bufferInfo.offset);
        outputBuffer.get(csd);
        ByteBuffer sps = null;
        ByteBuffer pps = null;
        for (int i = bufferInfo.size - 1; i > 3; i--) {
            if (csd[i] == 1 && csd[i - 1] == 0 && csd[i - 2] == 0 && csd[i - 3] == 0) {
                sps = ByteBuffer.allocate(i - 3);
                pps = ByteBuffer.allocate(bufferInfo.size - (i - 3));
                sps.put(csd, 0, i - 3).position(0);
                pps.put(csd, i - 3, bufferInfo.size - (i - 3)).position(0);
            }
        }
        MediaFormat outputFormat = mEncoder.getOutputFormat();
        if (sps != null && pps != null) {
            outputFormat.setByteBuffer("csd-0", sps);
            outputFormat.setByteBuffer("csd-1", pps);
        }
        int videoTrackIndex = mMediaMuxer.addTrack(outputFormat);
        Log.d(TAG, "videoTrackIndex: " + videoTrackIndex);
        mMediaMuxer.start();
        return videoTrackIndex;
    }
}