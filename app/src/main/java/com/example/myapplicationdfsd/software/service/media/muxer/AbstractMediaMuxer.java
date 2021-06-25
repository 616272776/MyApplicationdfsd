package com.example.myapplicationdfsd.software.service.media.muxer;

import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.example.myapplicationdfsd.software.service.media.data.MediaData;
import com.example.myapplicationdfsd.software.service.media.data.MediaDataQueue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractMediaMuxer {
    protected MediaDataQueue<MediaData> AudioMediaEncodeDataQueue;
    protected MediaDataQueue<MediaData> VideoMediaEncodeDataQueue;
    protected MediaMuxer mMediaMuxer;
    protected int mAudioTrackIndex = -1;
    protected int mVideoTrackIndex = -1;

    protected Thread AudioMuxerThread;
    protected Thread VideoMuxerThread;
    protected AtomicBoolean AudioMuxerThreadIsStarted = new AtomicBoolean(false);
    protected AtomicBoolean VideoMuxerThreadIsStarted = new AtomicBoolean(false);
    protected AtomicBoolean  mMediaMuxerIsStarted= new AtomicBoolean(false);


    public void init(String filePath) {
        try {
            mMediaMuxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int addAudioTrack(MediaFormat newFormat) {
        mAudioTrackIndex = mMediaMuxer.addTrack(newFormat);
        return mAudioTrackIndex;
    }

    public int addVideoTrack(MediaFormat newFormat) {
        mVideoTrackIndex = mMediaMuxer.addTrack(newFormat);
        return mVideoTrackIndex;
    }

    public void start() {
        AudioMuxerThreadIsStarted.set(true);
        VideoMuxerThreadIsStarted.set(true);
        AudioMuxerThread.start();
        VideoMuxerThread.start();
    }

    public void release() {
        try {
            AudioMuxerThreadIsStarted.set(false);
            VideoMuxerThreadIsStarted.set(false);
            AudioMuxerThread.join();
            VideoMuxerThread.join();
            if (mMediaMuxer != null && mMediaMuxerIsStarted.get()) {
                mMediaMuxer.stop();
                mMediaMuxer.release();
                mMediaMuxer = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setAudioMediaEncodeDataQueue(MediaDataQueue<MediaData> audioMediaEncodeDataQueue) {
        AudioMediaEncodeDataQueue = audioMediaEncodeDataQueue;
    }

    public void setVideoMediaEncodeDataQueue(MediaDataQueue<MediaData> videoMediaEncodeDataQueue) {
        VideoMediaEncodeDataQueue = videoMediaEncodeDataQueue;
    }
}
