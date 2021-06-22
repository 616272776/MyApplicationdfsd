package com.example.myapplicationdfsd.softWareSystem.service.media.record;

import com.example.myapplicationdfsd.softWareSystem.service.media.data.MediaData;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class MediaRecordCallBack {

    public  void init(){}
    public  void record(){}
    public  void recording(MediaData mediaData){}
    public  void stop(){}
    public  void release(){}
}
