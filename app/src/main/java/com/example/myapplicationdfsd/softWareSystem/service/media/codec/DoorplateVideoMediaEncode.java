package com.example.myapplicationdfsd.softWareSystem.service.media.codec;

import android.media.MediaCodec;
import android.util.Log;

import com.example.myapplicationdfsd.MainActivity;
import com.example.myapplicationdfsd.softWareSystem.service.media.data.MediaEncodeData;
import com.example.myapplicationdfsd.softWareSystem.service.media.data.MediaData;
import com.example.myapplicationdfsd.softWareSystem.service.media.data.VideoData;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSource;
import org.webrtc.YuvHelper;

import java.nio.ByteBuffer;

public class DoorplateVideoMediaEncode extends AbstractVideoMediaEncode {
    private static final String TAG = "DoorplateVideoMediaEncode";
    private long mNanoTime;
    private static final long DEQUEUE_TIME_OUT = 100L;
    @Override
    public void init() {
        super.init();

        MainActivity.videoSource.setVideoSourceCallback(new VideoSource.VideoSourceCallback() {
            @Override
            public void onFrameCaptured(VideoFrame videoFrame) {
                    queue(videoFrame);
            }
        });

        mediaEncodeQueueThread = new Thread(new Runnable() {
            @Override
            public void run() {
            }
        });
        mediaEncodeDequeueThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mediaEncodeDequeueThreadIsRunning.get()){
                    dequeue();
                }
            }
        });
    }

    @Override
    public void queue(MediaData mediaData) {

    }


    public void queue(VideoFrame videoFrame) {
        if (mediaEncodeQueueThreadIsRunning.get()) {
            mNanoTime = System.nanoTime();
            if (mediaCodec == null) {
                Log.e(TAG, "mEncoder is null");
                return;
            }
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(DEQUEUE_TIME_OUT);
//            Log.d(TAG, "inputBufferIndex: " + inputBufferIndex);
            if (inputBufferIndex == -1) {
                Log.e(TAG, "no valid buffer available");
                return;
            }
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
            VideoFrame.I420Buffer i420 = videoFrame.getBuffer().toI420();
            YuvHelper.I420ToNV12(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(),
                    i420.getDataV(), i420.getStrideV(), inputBuffer, i420.getWidth(), i420.getHeight());
            i420.release();
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, videoFrame.getBuffer().getHeight() * videoFrame.getBuffer().getWidth() * 3 / 2, videoFrame.getTimestampNs() / 1000, 0);
        }
    }

    @Override
    public void dequeue() {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIME_OUT);
//        Log.d(TAG, "outputBufferIndex: " + outputBufferIndex);
        if (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
//                Log.d(TAG, "write outputBuffer");
//                mMediaMuxer.writeSampleData(mVideoTrackIndex, outputBuffer, bufferInfo);
                mediaEncodeDataQueue.offer(new MediaEncodeData(bufferInfo,outputBuffer,null));
            }
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);

        } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            // 请求超时
            try {
                // wait 10ms
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            // 后续输出格式变化
//            if (mMuxerStarted.get()) {
//                throw new IllegalStateException("output format already changed!");
//            }
            mediaFormat = mediaCodec.getOutputFormat();
            mediaEncodeDataQueue.offer(new MediaEncodeData(null,null,mediaFormat));
//            mVideoTrackIndex = mMediaMuxer.addTrack(newFormat);
//            synchronized (mLock) {
//                if (mVideoTrackIndex >= 0 && mVideoTrackIndex >= 0) {
//                    mMediaMuxer.start();
//                    mMuxerStarted.set(true);
//                    Logging.d(TAG, "started media muxer, mAudioTrackIndex=" + mAudioTrackIndex
//                            + ",mVideoTrackIndex=" + mVideoTrackIndex);
//                }
//            }
        }
    }
}
