package com.example.myapplicationdfsd.softWareSystem.service.media.data;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

public class MediaEncodeData implements MediaData{
    private MediaCodec.BufferInfo BufferInfo;
    private ByteBuffer encodedData;

    public MediaEncodeData(MediaCodec.BufferInfo BufferInfo, ByteBuffer encodedData) {
        this.BufferInfo = BufferInfo;
        this.encodedData = encodedData;
    }

    public MediaCodec.BufferInfo getBufferInfo() {
        return BufferInfo;
    }

    public ByteBuffer getEncodedData() {
        return encodedData;
    }
}
