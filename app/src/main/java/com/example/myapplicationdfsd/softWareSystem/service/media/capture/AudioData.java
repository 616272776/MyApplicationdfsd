package com.example.myapplicationdfsd.softWareSystem.service.media.capture;

import com.example.myapplicationdfsd.softWareSystem.service.media.MediaData;

import java.nio.ByteBuffer;

class AudioData implements MediaData {
        ByteBuffer mData;
        long mPresentationTimeUs;
        int mSize;

        public AudioData(ByteBuffer data, long timeUs, int size) {
            mData = data;
            mPresentationTimeUs = timeUs;
            mSize = size;
        }
    }