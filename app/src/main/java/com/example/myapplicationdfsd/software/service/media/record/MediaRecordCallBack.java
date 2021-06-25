package com.example.myapplicationdfsd.software.service.media.record;

import com.example.myapplicationdfsd.software.service.media.data.MediaData;

public abstract class MediaRecordCallBack {

    public  void init(){}
    public  void record(){}
    public  void recording(MediaData mediaData){}
    public  void stop(){}
    public  void release(){}
}
