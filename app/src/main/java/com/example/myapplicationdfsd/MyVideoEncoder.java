package com.example.myapplicationdfsd;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import org.webrtc.Logging;
import org.webrtc.VideoFrame;
import org.webrtc.YuvHelper;
import org.webrtc.audio.WebRtcAudioRecord;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyVideoEncoder {

    private static final String TAG = "VideoEncoder";

    private final static String VIDEO_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final String AUDIO_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final long DEFAULT_TIMEOUT_US = 10000;
    private static final long DEQUEUE_TIME_OUT = 100L;



    private int mVideoTrackIndex;
    private int mAudioTrackIndex = -1;
    private boolean mStop = false;
    private final Object mLock = new Object();

    private AtomicBoolean mVideoThreadCancel = new AtomicBoolean(true);
    private AtomicBoolean mAudioThreadCancel = new AtomicBoolean(true);

    //音频
    private LinkedBlockingQueue<AudioData> mAudioOutBufferQueue;
//    private LinkedBlockingQueue<AudioData> mAudioInBufferQueue;
    private int mAudioSampleRate;
    private int mAudioChannels;
    private int mAudioBufferSize;
    private int mAudioBitsPerSample;
    private static final int QUEUE_MAX_COUNT = 100;

    //muxer
    private MediaCodec mAudioCodec;
    private MediaCodec mVideoCoder;
    private MediaMuxer mMediaMuxer;
    private AtomicBoolean mMuxerStarted = new AtomicBoolean(false);
    private long mLastAudioPresentationTimeUs = 0L;
    private long mNanoTime;

    public void init(String outPath, int width, int height) {
        try {
            mAudioOutBufferQueue = new LinkedBlockingQueue<AudioData>(QUEUE_MAX_COUNT);
            mStop = false;
            mVideoTrackIndex = -1;
            mMediaMuxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mVideoCoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);

            WebRtcAudioRecord.setWebRtcAudioRecordCallback(new WebRtcAudioRecord.WebRtcAudioRecordCallback() {
                @Override
                public void onWebRtcAudioRecordInit(int audioSource, int audioFormat, int sampleRate,
                                                    int channels, int bitPerSample, int bufferPerSecond, int bufferSizeInBytes) {
//                    mAudioSource = audioSource;
//                    mAudioFormat = audioFormat;
                    mAudioSampleRate = sampleRate;
                    mAudioChannels = channels;
                    mAudioBitsPerSample = bitPerSample;
//                    mAudioBuffersPerSecond = bufferPerSecond;
                    mAudioBufferSize = bufferSizeInBytes;
                    mAudioOutBufferQueue = new LinkedBlockingQueue<AudioData>(QUEUE_MAX_COUNT);
                }

                @Override
                public void onWebRtcAudioRecordStart() {

                }

                @Override
                public void onWebRtcAudioRecording(ByteBuffer buffer, int bufferSize, boolean microphoneMute) {
                    System.out.println("音频");
                    final ByteBuffer cpBuffer = ByteBuffer.allocateDirect(bufferSize);
                    cpBuffer.order(buffer.order());
                    cpBuffer.put(buffer.array(), buffer.arrayOffset(), bufferSize);
                    cpBuffer.rewind();
                    cpBuffer.limit(bufferSize);
                    AudioData audioData = new AudioData(cpBuffer, System.nanoTime() / 1000L
                            , bufferSize, 1);
                    mAudioOutBufferQueue.offer(audioData);
                    cpBuffer.clear();
                }

                @Override
                public void onWebRtcAudioRecordStop() {

                }
            });

            //音频格式信息
            MediaFormat audioFormat = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, mAudioSampleRate, mAudioChannels);
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mAudioBitsPerSample * mAudioSampleRate * 4);
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, mAudioBufferSize);
            Log.d(TAG, "created audio format: " + audioFormat);
            mAudioCodec = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
            mAudioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mAudioCodec.start();

            // 视频格式信息
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 6);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mVideoCoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mVideoCoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void release() {
        mStop = true;
        if (mVideoCoder != null) {
            mVideoCoder.stop();
            mVideoCoder.release();
            mVideoCoder = null;
        }
        if (mMediaMuxer != null) {
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }

    public void encode(VideoFrame videoFrame) {
        mNanoTime = System.nanoTime();

        if (mVideoCoder == null || mMediaMuxer == null) {
            Log.e(TAG, "mEncoder or mMediaMuxer is null");
            return;
        }
//        if (yuv == null) {
//            Log.e(TAG, "input yuv data is null");
//            return;
//        }
        int inputBufferIndex = mVideoCoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
        Log.d(TAG, "inputBufferIndex: " + inputBufferIndex);
        if (inputBufferIndex == -1) {
            Log.e(TAG, "no valid buffer available");
            return;
        }

        ByteBuffer inputBuffer = mVideoCoder.getInputBuffer(inputBufferIndex);
        VideoFrame.I420Buffer i420 = videoFrame.getBuffer().toI420();
        YuvHelper.I420ToNV12(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(),
                i420.getDataV(), i420.getStrideV(), inputBuffer, i420.getWidth(), i420.getHeight());
        i420.release();

        mVideoCoder.queueInputBuffer(inputBufferIndex, 0, videoFrame.getBuffer().getHeight() * videoFrame.getBuffer().getWidth() * 3 / 2, videoFrame.getTimestampNs() / 1000, 0);
        while (!mStop) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mVideoCoder.dequeueOutputBuffer(bufferInfo, DEFAULT_TIMEOUT_US);
            Log.d(TAG, "outputBufferIndex: " + outputBufferIndex);
            if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mVideoCoder.getOutputBuffer(outputBufferIndex);
                // write head info
                if (mVideoTrackIndex == -1) {
                    Log.d(TAG, "this is first frame, call writeHeadInfo first");
                    mVideoTrackIndex = writeHeadInfo(outputBuffer, bufferInfo);
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    Log.d(TAG, "write outputBuffer");
                    mMediaMuxer.writeSampleData(mVideoTrackIndex, outputBuffer, bufferInfo);
                }
                mVideoCoder.releaseOutputBuffer(outputBufferIndex, false);
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
        MediaFormat outputFormat = mVideoCoder.getOutputFormat();
        if (sps != null && pps != null) {
            outputFormat.setByteBuffer("csd-0", sps);
            outputFormat.setByteBuffer("csd-1", pps);
        }
        int videoTrackIndex = mMediaMuxer.addTrack(outputFormat);
        Log.d(TAG, "videoTrackIndex: " + videoTrackIndex);
        feedAudioData();
        writeAudioData();
        mMediaMuxer.start();
        return videoTrackIndex;
    }

    private boolean feedAudioData() {
        if (mAudioOutBufferQueue.size() < 1) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }
        AudioData audioOut = mAudioOutBufferQueue.poll();
//        AudioData audioIn = mAudioInBufferQueue.poll();
        ByteBuffer audioOutBuffer = audioOut.mData;
//        ByteBuffer audioInBuffer = audioIn.mData;
        int size = audioOut.mSize;
//        int inSize = audioIn.mSize;
//        int size = Math.max(outSize, inSize);
        long timeUs = audioOut.mPresentationTimeUs;
//        byte[][] bMulRoadAudios = new byte[2][size];
//        audioOutBuffer.get(bMulRoadAudios[0], 0, size);
//        audioInBuffer.get(bMulRoadAudios[1], 0, size);
//        if (outSize > inSize) {
//            for (int i = inSize; i < outSize; i++) {
//                bMulRoadAudios[1][i] = 0x00;
//            }
//        } else {
//            for (int i = outSize; i < inSize; i++) {
//                bMulRoadAudios[0][i] = 0x00;
//            }
//        }
//        byte[] mixAudio = averageMix(bMulRoadAudios);
        int index = mAudioCodec.dequeueInputBuffer((System.nanoTime() - mNanoTime) / 1000L);
        if (index >= 0) {
            ByteBuffer inputBuffer = mAudioCodec.getInputBuffer(index);
            inputBuffer.clear();
//            inputBuffer.put(mixAudio);
            inputBuffer.order(audioOutBuffer.order());
            mAudioCodec.queueInputBuffer(index, 0, size,
                    timeUs, // presentationTimeUs要与视频的一致（MediaProjection使用的是System.nanoTime() / 1000L）
                    mAudioThreadCancel.get() ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
        }
        return false;
    }

    private boolean writeAudioData() {
        MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
        int outIndex = mAudioCodec.dequeueOutputBuffer(audioBufferInfo, DEQUEUE_TIME_OUT);
        if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            // 后续输出格式变化
            if (mMuxerStarted.get()) {
                throw new IllegalStateException("output format already changed!");
            }
            MediaFormat newFormat = mAudioCodec.getOutputFormat();
            mAudioTrackIndex = mMediaMuxer.addTrack(newFormat);
            synchronized (mLock) {
                if (mAudioTrackIndex >= 0 && mVideoTrackIndex >= 0) {
                    mMediaMuxer.start();
//                    mMuxerStarted.set(true);
                    Logging.d(TAG, "started media muxer, mAudioTrackIndex=" + mAudioTrackIndex
                            + ",mVideoTrackIndex=" + mVideoTrackIndex);
                }
            }
        } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            // 请求超时
            try {
                // wait 10ms
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        } else if (outIndex >= 0) {
            // 获取到的实时音频数据
            ByteBuffer encodedData = mAudioCodec.getOutputBuffer(outIndex);
            if ((audioBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                // The codec config data was pulled out and fed to the muxer
                // when we got
                // the INFO_OUTPUT_FORMAT_CHANGED status.
                // Ignore it.
                Logging.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                audioBufferInfo.size = 0;
            }
            if (audioBufferInfo.size == 0) {
                Logging.d(TAG, "info.size == 0, drop it.");
                encodedData = null;
            }
            if (encodedData != null && mMuxerStarted.get()
                    && mLastAudioPresentationTimeUs < audioBufferInfo.presentationTimeUs) {
                mMediaMuxer.writeSampleData(mAudioTrackIndex, encodedData, audioBufferInfo);
                mLastAudioPresentationTimeUs = audioBufferInfo.presentationTimeUs;
            }
            mAudioCodec.releaseOutputBuffer(outIndex, false);
            if ((audioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                return true;
            }
        }
        return false;
    }
}
