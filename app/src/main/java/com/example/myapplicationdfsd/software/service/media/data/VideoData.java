package com.example.myapplicationdfsd.software.service.media.data;

import org.webrtc.VideoFrame;

public class VideoData implements MediaData {
    VideoFrame videoFrame;

    public VideoData(VideoFrame videoFrame) {
        this.videoFrame = videoFrame;
    }

    public VideoFrame getVideoFrame() {
        return videoFrame;
    }
}
