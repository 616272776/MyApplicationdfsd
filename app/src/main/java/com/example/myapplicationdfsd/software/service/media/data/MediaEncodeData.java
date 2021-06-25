package com.example.myapplicationdfsd.software.service.media.data;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

public class MediaEncodeData implements MediaData{
    private MediaCodec.BufferInfo BufferInfo;
    private ByteBuffer encodedData;
    private MediaFormat mediaFormat;

    public MediaEncodeData(MediaCodec.BufferInfo BufferInfo, ByteBuffer encodedData,MediaFormat mediaFormat) {
        this.BufferInfo = BufferInfo;
        this.encodedData = encodedData;
        this.mediaFormat = mediaFormat;
    }

    public MediaCodec.BufferInfo getBufferInfo() {
        return BufferInfo;
    }

    public ByteBuffer getEncodedData() {
        return encodedData;
    }

    public MediaFormat getMediaFormat() {
        return mediaFormat;
    }
}
