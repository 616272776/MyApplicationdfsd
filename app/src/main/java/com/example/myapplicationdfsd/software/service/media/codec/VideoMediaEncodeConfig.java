package com.example.myapplicationdfsd.software.service.media.codec;

public class VideoMediaEncodeConfig {
    int width;
    int height;

    public VideoMediaEncodeConfig(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
