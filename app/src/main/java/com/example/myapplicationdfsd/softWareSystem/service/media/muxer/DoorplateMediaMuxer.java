package com.example.myapplicationdfsd.softWareSystem.service.media.muxer;

import android.media.MediaFormat;
import com.example.myapplicationdfsd.softWareSystem.service.media.data.MediaEncodeData;

public class DoorplateMediaMuxer extends AbstractMediaMuxer {
    private final Object mLock = new Object();
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
                    MediaFormat mediaFormat = mediaEncodeData.getMediaFormat();
                    if(mediaFormat==null){
                        if(mMediaMuxerIsStarted.get()){
                            mMediaMuxer.writeSampleData(mAudioTrackIndex, mediaEncodeData.getEncodedData(), mediaEncodeData.getBufferInfo());
                        }
                    }else {
                        mAudioTrackIndex = addAudioTrack(mediaEncodeData.getMediaFormat());
                        synchronized (mLock) {
                            if (mVideoTrackIndex >= 0 && mVideoTrackIndex >= 0 && !mMediaMuxerIsStarted.get()) {
                                mMediaMuxerIsStarted.set(true);
                                mMediaMuxer.start();
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

                    MediaFormat mediaFormat = mediaEncodeData.getMediaFormat();
                    if(mediaFormat==null ){
                        if(mMediaMuxerIsStarted.get()){
                            mMediaMuxer.writeSampleData(mVideoTrackIndex, mediaEncodeData.getEncodedData(), mediaEncodeData.getBufferInfo());


                        }
                    }else {
                        mVideoTrackIndex = addVideoTrack(mediaFormat);
                        synchronized (mLock) {
                            if (mVideoTrackIndex >= 0 && mVideoTrackIndex >= 0 && !mMediaMuxerIsStarted.get()) {
                                mMediaMuxerIsStarted.set(true);
                                mMediaMuxer.start();
                            }
                        }
                    }
                }
            }
        });
    }
}
