package com.example.myapplicationdfsd.software.service.media.codec;

import android.media.MediaCodec;

import com.example.myapplicationdfsd.software.service.media.data.AudioData;
import com.example.myapplicationdfsd.software.service.media.data.MediaEncodeData;
import com.example.myapplicationdfsd.software.service.media.data.MediaData;

import org.webrtc.Logging;

import java.nio.ByteBuffer;

public class DoorplateAudioMediaEncode extends AbstractAudioMediaEncode {
    private static final String TAG = "DoorplateAudioMediaEncode";
    private long mLastAudioPresentationTimeUs = 0L;
    private static final long DEQUEUE_TIME_OUT = 100L;

    @Override
    public void init( ) {
        super.init();

        mediaEncodeQueueThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mediaEncodeQueueThreadIsRunning.get()){
                    if(mediaRecordDataQueue.checkQueueSizeLowerThen0()){
                        continue;
                    }

                    AudioData audioData = (AudioData) mediaRecordDataQueue.poll();
                    queue(audioData);
                }
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
        AudioData audioData = (AudioData)mediaData;
        int index = mediaCodec.dequeueInputBuffer(DEQUEUE_TIME_OUT);
        if (index >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            inputBuffer.put(audioData.mData);
            mediaCodec.queueInputBuffer(index, 0, audioData.mSize,
                    audioData.mPresentationTimeUs, // presentationTimeUs要与视频的一致（MediaProjection使用的是System.nanoTime() / 1000L）
                    0);
        }else if(index == MediaCodec.INFO_TRY_AGAIN_LATER){
            // 请求超时
            try {
                // wait 10ms
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
    }


    @Override
    public void dequeue() {
        MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
        int outIndex = mediaCodec.dequeueOutputBuffer(audioBufferInfo, DEQUEUE_TIME_OUT);
        if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            // 后续输出格式变化
//            if (mMuxerStarted.get()) {
//                throw new IllegalStateException("output format already changed!");
//            }
            mediaFormat= mediaCodec.getOutputFormat();
            mediaEncodeDataQueue.offer(new MediaEncodeData(null,null,mediaFormat));
//            mAudioTrackIndex = mMediaMuxer.addTrack(newFormat);
//            synchronized (mLock) {
//                if (mAudioTrackIndex >= 0 && mVideoTrackIndex >= 0) {
//                    mMediaMuxer.start();
//                    mMuxerStarted.set(true);
//                    Logging.d(TAG, "started media muxer, mAudioTrackIndex=" + mAudioTrackIndex
//                            + ",mVideoTrackIndex=" + mVideoTrackIndex);
//                }
//            }
        } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            // 请求超时
            try {
                // wait 10ms
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        } else if (outIndex >= 0) {
            // 获取到的实时音频数据
            ByteBuffer encodedData = mediaCodec.getOutputBuffer(outIndex);
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

            if (encodedData != null
//                    && mMuxerStarted.get()
                    && mLastAudioPresentationTimeUs < audioBufferInfo.presentationTimeUs) {
//                mMediaMuxer.writeSampleData(mAudioTrackIndex, encodedData, audioBufferInfo);
                mLastAudioPresentationTimeUs = audioBufferInfo.presentationTimeUs;
                mediaEncodeDataQueue.offer(new MediaEncodeData(audioBufferInfo,encodedData,null));
            }
            mediaCodec.releaseOutputBuffer(outIndex, false);
//
//            if ((audioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                return true;
//            }
        }
    }

}
