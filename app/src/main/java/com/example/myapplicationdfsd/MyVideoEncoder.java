package com.example.myapplicationdfsd;

import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import org.webrtc.Logging;
import org.webrtc.VideoFrame;
import org.webrtc.YuvHelper;
import org.webrtc.audio.WebRtcAudioRecord;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyVideoEncoder {

    private static final String TAG = "VideoEncoder";

    private final static String VIDEO_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final String AUDIO_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final long DEQUEUE_TIME_OUT = 100L;


    private int mVideoTrackIndex = -1;
    private int mAudioTrackIndex = -1;
    private boolean mStop = false;
    private final Object mLock = new Object();

    private AtomicBoolean mVideoThreadCancel = new AtomicBoolean(true);
    private AtomicBoolean mAudioThreadCancel = new AtomicBoolean(true);
    private AtomicBoolean mVideoRecordStarted = new AtomicBoolean(false);

    //音频
    private LinkedBlockingQueue<AudioData> mAudioOutBufferQueue;
    private int mAudioSampleRate= 44100;
    private int mAudioChannels = 1;
    private int mAudioBufferSize = 8192;
    private int mAudioBitsPerSample =10;
    private static final int QUEUE_MAX_COUNT = 100;

    //muxer
    private MediaCodec mAudioCodec;
    private MediaCodec mVideoCodec;
    private MediaMuxer mMediaMuxer;
    private AtomicBoolean mMuxerStarted = new AtomicBoolean(false);
    private long mLastAudioPresentationTimeUs = 0L;
    private long mNanoTime;
    private int height;
    private int width;

    private Thread mAudioFeedThread;
    private Thread mAudioWriteThread;
    private Thread mVideoWriteThread;
    private String outPath;
    private Handler mRecorderThreadHandler;
    private HandlerThread mRecorderThread;

    private static final String FILE_SAVE_DIR;

    static {
        FILE_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/rtc/";
    }

    public void init(String outPath, int width, int height) {

        mRecorderThread = new HandlerThread("MediaRecordController");
        mRecorderThread.start();
        mRecorderThreadHandler = new Handler(mRecorderThread.getLooper());
        this.outPath = outPath;
        this.width = width;
        this.height = height;
        mAudioOutBufferQueue = new LinkedBlockingQueue<AudioData>(QUEUE_MAX_COUNT);


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
//                mAudioOutBufferQueue = new LinkedBlockingQueue<AudioData>(QUEUE_MAX_COUNT);
            }

            @Override
            public void onWebRtcAudioRecordStart() {

            }

            @Override
            public void onWebRtcAudioRecording(ByteBuffer buffer, int bufferSize, boolean microphoneMute) {
                if (mVideoRecordStarted.get()) {
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
            }

            @Override
            public void onWebRtcAudioRecordStop() {

            }
        });
    }

    public void prepareEncoder() {
//        if (!mIsCreate.get()) {
//            throw new RuntimeException("you need call onCreate method of MediaRecordController before start record");
//        }
        if (TextUtils.isEmpty(outPath)) {
            outPath = FILE_SAVE_DIR + "/room.mp4";
        }
        File file = new File(outPath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        mNanoTime = System.nanoTime();

        mRecorderThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
//                    mState = STATE_PREPARING;
                    if (mAudioFeedThread != null && mAudioFeedThread.isAlive()) {
                        mAudioThreadCancel.set(true);
                        mAudioFeedThread.join();
                    }
                    if (mAudioWriteThread != null && mAudioWriteThread.isAlive()) {
                        mAudioThreadCancel.set(true);
                        mAudioWriteThread.join();
                    }
                    if (mVideoWriteThread != null && mVideoWriteThread.isAlive()) {
                        mVideoThreadCancel.set(true);
                        mVideoWriteThread.join();
                    }
                    try {
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
                        mVideoCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
                        mVideoCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                        mVideoCodec.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mMediaMuxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mAudioThreadCancel.set(false);
                mAudioFeedThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!mAudioThreadCancel.get()) {
                            if (feedAudioData()) {
                                break;
                            }
                        }
                    }
                });
                mAudioFeedThread.start();

                mAudioWriteThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!mAudioThreadCancel.get()) {
                            if (writeAudioData()) {
                                break;
                            }
                        }
                    }
                });
                mAudioWriteThread.start();

                mVideoThreadCancel.set(false);
                mVideoWriteThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!mVideoThreadCancel.get()) {
                            if (writeVideoData()) {
                                break;
                            }
                        }
                    }
                });
                mVideoWriteThread.start();
//                mState = STATE_RECORDING;
            }
        });


        mVideoRecordStarted.set(true);
    }

    private boolean writeVideoData() {

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIME_OUT);
            Log.d(TAG, "outputBufferIndex: " + outputBufferIndex);
            if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mVideoCodec.getOutputBuffer(outputBufferIndex);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    Log.d(TAG, "write outputBuffer");
                    mMediaMuxer.writeSampleData(mVideoTrackIndex, outputBuffer, bufferInfo);
                }
                mVideoCodec.releaseOutputBuffer(outputBufferIndex, false);

            } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // 请求超时
                try {
                    // wait 10ms
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 后续输出格式变化
                if (mMuxerStarted.get()) {
                    throw new IllegalStateException("output format already changed!");
                }
                MediaFormat newFormat = mVideoCodec.getOutputFormat();
                mVideoTrackIndex = mMediaMuxer.addTrack(newFormat);
                synchronized (mLock) {
                    if (mVideoTrackIndex >= 0 && mVideoTrackIndex >= 0) {
                        mMediaMuxer.start();
                        mMuxerStarted.set(true);
                        Logging.d(TAG, "started media muxer, mAudioTrackIndex=" + mAudioTrackIndex
                                + ",mVideoTrackIndex=" + mVideoTrackIndex);
                    }
                }
            }


        return true;
    }

    public void release() {
        mStop = true;
        if (mRecorderThreadHandler != null) {
            mRecorderThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mAudioFeedThread != null && mAudioFeedThread.isAlive()) {
                            mAudioThreadCancel.set(true);
                            mAudioFeedThread.join();
                        }
                        if (mAudioWriteThread != null && mAudioWriteThread.isAlive()) {
                            mAudioThreadCancel.set(true);
                            mAudioWriteThread.join();
                        }
//                        if (mVideoThread != null && mVideoThread.isAlive()) {
//                            mVideoThreadCancel.set(true);
//                            mVideoThread.join();
//                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        mAudioThreadCancel.set(true);
                        mVideoThreadCancel.set(true);
                    }
                    if (mAudioCodec != null) {
                        mAudioCodec.stop();
                        mAudioCodec.release();
                        mAudioCodec = null;
                    }
                    if (mVideoCodec != null) {
                        mVideoCodec.stop();
                        mVideoCodec.release();
                        mVideoCodec = null;
                    }
                    mAudioTrackIndex = -1;
                    mVideoTrackIndex = -1;
                    if (mMuxerStarted.get()) {
                        mMuxerStarted.set(false);
                        if (mMediaMuxer != null) {
                            mMediaMuxer.stop();
                            mMediaMuxer.release();
                            mMediaMuxer = null;
                        }
                    }
                    Logging.d(TAG, "released");
                }
            });
        }
    }

    public void encode(VideoFrame videoFrame) {
        if (mVideoRecordStarted.get()) {
            mNanoTime = System.nanoTime();
            if (mVideoCodec == null || mMediaMuxer == null) {
                Log.e(TAG, "mEncoder or mMediaMuxer is null");
                return;
            }
            int inputBufferIndex = mVideoCodec.dequeueInputBuffer(DEQUEUE_TIME_OUT);
            Log.d(TAG, "inputBufferIndex: " + inputBufferIndex);
            if (inputBufferIndex == -1) {
                Log.e(TAG, "no valid buffer available");
                return;
            }
            ByteBuffer inputBuffer = mVideoCodec.getInputBuffer(inputBufferIndex);
            VideoFrame.I420Buffer i420 = videoFrame.getBuffer().toI420();
            YuvHelper.I420ToNV12(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(),
                    i420.getDataV(), i420.getStrideV(), inputBuffer, i420.getWidth(), i420.getHeight());
            i420.release();
            mVideoCodec.queueInputBuffer(inputBufferIndex, 0, videoFrame.getBuffer().getHeight() * videoFrame.getBuffer().getWidth() * 3 / 2, videoFrame.getTimestampNs() / 1000, 0);


        }

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
        ByteBuffer audioOutBuffer = audioOut.mData;
        int size = audioOut.mSize;
        long timeUs = audioOut.mPresentationTimeUs;
        int index = mAudioCodec.dequeueInputBuffer(DEQUEUE_TIME_OUT);
        if (index >= 0) {
            ByteBuffer inputBuffer = mAudioCodec.getInputBuffer(index);
            inputBuffer.clear();
            inputBuffer.put(audioOutBuffer);
            mAudioCodec.queueInputBuffer(index, 0, size,
                    timeUs, // presentationTimeUs要与视频的一致（MediaProjection使用的是System.nanoTime() / 1000L）
                    0);
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
                    mMuxerStarted.set(true);
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
                Logging.d(TAG, "inf3" +
                        "" +
                        " o.size == 0, drop it.");
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
