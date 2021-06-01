package com.example.myapplicationdfsd;

import java.nio.ByteBuffer;

class AudioData {
        ByteBuffer mData;
        long mPresentationTimeUs;
        int mSize;
        int mType;  // 1-out;2-in

        public AudioData(ByteBuffer data, long timeUs, int size, int type) {
            mData = data;
            mPresentationTimeUs = timeUs;
            mSize = size;
            mType = type;
        }
    }