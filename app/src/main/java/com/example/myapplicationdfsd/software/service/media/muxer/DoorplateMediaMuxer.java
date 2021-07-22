package com.example.myapplicationdfsd.software.service.media.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import com.example.myapplicationdfsd.software.service.media.data.MediaEncodeData;

import java.util.concurrent.atomic.AtomicBoolean;

public class DoorplateMediaMuxer extends AbstractMediaMuxer {
    private final Object mLock = new Object();
    private static MediaFormat mAudioMediaFormat;
    private static MediaFormat mVideoMediaFormat;


    private final AtomicBoolean mAudioTrack = new AtomicBoolean(false);
    private final AtomicBoolean mVideoTrack = new AtomicBoolean(false);
    @Override
    public void init(String filePath) {
        super.init(filePath);

        AudioMuxerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (AudioMuxerThreadIsStarted.get()){
                    if(AudioMediaEncodeDataQueue.checkQueueSizeLowerThen0()){
                        continue;
                    }
                    MediaEncodeData mediaEncodeData = (MediaEncodeData)AudioMediaEncodeDataQueue.poll();
                    //判断是否为空，正常情况下队列的一个是带有媒体格式的
                    if(mediaEncodeData.getMediaFormat()!=null){
                        mAudioMediaFormat = mediaEncodeData.getMediaFormat();
                    }
                    if(mAudioTrack.get()){
                        if(mMediaMuxerIsStarted.get()){
                            MediaCodec.BufferInfo bufferInfo = mediaEncodeData.getBufferInfo();
                            mMediaMuxer.writeSampleData(mAudioTrackIndex, mediaEncodeData.getEncodedData(), mediaEncodeData.getBufferInfo());
                        }
                    }else {
                        mAudioTrackIndex = addAudioTrack(mAudioMediaFormat);
                        mAudioTrack.set(true);
                        synchronized (mLock) {
                            if (mVideoTrackIndex >= 0 && mAudioTrackIndex >= 0 && !mMediaMuxerIsStarted.get()) {
                                mMediaMuxer.start();
                                mMediaMuxerIsStarted.set(true);
                            }
                        }
                    }
                }
            }
        });
        VideoMuxerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (VideoMuxerThreadIsStarted.get()){
                    if(VideoMediaEncodeDataQueue.checkQueueSizeLowerThen0()){
                        continue;
                    }
                    MediaEncodeData mediaEncodeData = (MediaEncodeData)VideoMediaEncodeDataQueue.poll();


                    //判断是否为空，正常情况下队列的一个是带有媒体格式的
                    if(mediaEncodeData.getMediaFormat()!=null){
                        mVideoMediaFormat = mediaEncodeData.getMediaFormat();
                    }

                    if(mVideoTrack.get()){
                        if(mMediaMuxerIsStarted.get()){
                            mMediaMuxer.writeSampleData(mVideoTrackIndex, mediaEncodeData.getEncodedData(), mediaEncodeData.getBufferInfo());
                        }
                    }else {
                        mVideoTrackIndex = addVideoTrack(mVideoMediaFormat);
                        mVideoTrack.set(true);
                        synchronized (mLock) {
                            mAudioMediaFormat =mVideoMediaFormat;
                            if (mVideoTrackIndex >= 0 && mAudioTrackIndex >= 0 && !mMediaMuxerIsStarted.get()) {
                                mMediaMuxer.start();
                                mMediaMuxerIsStarted.set(true);
                            }
                        }
                    }
                }
            }
        });
    }
}
