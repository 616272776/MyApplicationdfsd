package com.example.myapplicationdfsd.softWareSystem.service.media.capture;

import com.example.myapplicationdfsd.softWareSystem.service.media.MediaData;

public interface MediaRecord {
    interface Callback {
        void init();
        void record();
        void recording(MediaData mediaData);
        void stop();
        void release();
    }
}
