package com.example.myapplicationdfsd.softWareSystem.service.media.codec;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.example.myapplicationdfsd.softWareSystem.service.media.data.MediaData;
import com.example.myapplicationdfsd.softWareSystem.service.media.data.MediaDataQueue;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class  AbstractMediaCodec {

    protected AtomicBoolean mediaEncodeQueueThreadIsRunning = new AtomicBoolean(false);
    protected AtomicBoolean mediaEncodeDequeueThreadIsRunning = new AtomicBoolean(false);
    protected MediaCodec mediaCodec;
    protected Thread mediaEncodeQueueThread;
    protected Thread mediaEncodeDequeueThread;

    protected MediaDataQueue<MediaData> mediaRecordDataQueue;
    protected MediaDataQueue<MediaData> mediaEncodeDataQueue;

    protected MediaFormat mediaFormat;

    public void init(){
        if(mediaRecordDataQueue == null || mediaEncodeDataQueue == null){
            throw new IllegalStateException("mediaRecordDataQueue or  mediaEncodeDataQueue is null");
        }
    }
    public void start(){
        mediaCodec.start();
        mediaEncodeQueueThreadIsRunning.set(true);
        mediaEncodeDequeueThreadIsRunning.set(true);
        mediaEncodeQueueThread.start();
        mediaEncodeDequeueThread.start();
    }
    public void stop(){
        mediaEncodeQueueThreadIsRunning.set(false);
        mediaEncodeDequeueThreadIsRunning.set(false);
    }
    public void release(){
        stop();
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
    }
    public abstract void queue(MediaData mediaData);
    public abstract void dequeue();

    public void setMediaRecordDataQueue(MediaDataQueue<MediaData> mediaRecordDataQueue) {
        this.mediaRecordDataQueue = mediaRecordDataQueue;
    }

    public void setMediaEncodeDataQueue(MediaDataQueue<MediaData> mAudioMediaEncodeDataQueue){
        this.mediaEncodeDataQueue = mAudioMediaEncodeDataQueue;
    }
}
