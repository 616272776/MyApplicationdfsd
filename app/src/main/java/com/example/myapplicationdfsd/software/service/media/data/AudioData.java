package com.example.myapplicationdfsd.software.service.media.data;

import java.nio.ByteBuffer;

public class AudioData  implements MediaData {
        public ByteBuffer mData;
        public long mPresentationTimeUs;
        public int mSize;

        public AudioData(ByteBuffer data, long timeUs, int size) {
            mData = data;
            mPresentationTimeUs = timeUs;
            mSize = size;
        }


    }