package com.example.myapplicationdfsd.softWareSystem.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.myapplicationdfsd.hardWareSystem.memory.InternalMemory;
import com.example.myapplicationdfsd.softWareSystem.service.media.codec.AudioMediaEncodeConfig;
import com.example.myapplicationdfsd.softWareSystem.service.media.codec.DoorplateAudioMediaEncode;
import com.example.myapplicationdfsd.softWareSystem.service.media.codec.DoorplateVideoMediaEncode;
import com.example.myapplicationdfsd.softWareSystem.service.media.codec.VideoMediaEncodeConfig;
import com.example.myapplicationdfsd.softWareSystem.service.media.data.AudioData;
import com.example.myapplicationdfsd.MainActivity;
import com.example.myapplicationdfsd.MyVideoEncoder;
import com.example.myapplicationdfsd.softWareSystem.service.media.data.MediaDataQueue;
import com.example.myapplicationdfsd.softWareSystem.service.media.data.MediaData;
import com.example.myapplicationdfsd.softWareSystem.service.media.data.VideoData;
import com.example.myapplicationdfsd.softWareSystem.service.media.muxer.DoorplateMediaMuxer;
import com.example.myapplicationdfsd.softWareSystem.service.media.record.DoorplateAudioMediaRecord;
import com.example.myapplicationdfsd.softWareSystem.service.media.record.MediaRecordCallBack;
import com.example.myapplicationdfsd.softWareSystem.service.media.record.AudioRecordConfig;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSource;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.concurrent.atomic.AtomicBoolean;

public class MediaService extends Service {
    public static final String TAG = "mediaService";

    private AtomicBoolean doorplateVideoMediaRecordIsStart =new AtomicBoolean(true);


    private MediaDataQueue<MediaData> mAudioMediaRecordDataQueue;
    private MediaDataQueue<MediaData> mAudioMediaEncodeDataQueue;

    private DoorplateAudioMediaRecord doorplateAudioMediaRecord;
    private DoorplateAudioMediaEncode doorplateAudioMediaEncode;

    private DoorplateVideoMediaEncode doorplateVideoMediaEncode;

    private MediaDataQueue<MediaData> mVideoMediaRecordDataQueue;
    private MediaDataQueue<MediaData> mVideoMediaEncodeDataQueue;

    private DoorplateMediaMuxer doorplateMediaMuxer;
//    private MediaDataQueue<> mVideoMediaEncodeDataQueue;


    public class MyBinder extends Binder {
        public MediaService getService(){
            return MediaService.this;
        }
    }


    private MyBinder binder = new MyBinder();

    @Override
    public void onCreate() {
        super.onCreate();

        mAudioMediaRecordDataQueue = new MediaDataQueue<>();
        mAudioMediaEncodeDataQueue = new MediaDataQueue<>();

        mVideoMediaRecordDataQueue = new MediaDataQueue<>();
        mVideoMediaEncodeDataQueue = new MediaDataQueue<>();



        // todo 门牌音频获取测试
        doorplateAudioMediaRecord = DoorplateAudioMediaRecord.getInstance();
        doorplateAudioMediaRecord.configWebRtcAudioRecord(JavaAudioDeviceModule.MyAudioInput);
        doorplateAudioMediaRecord.configAudioRecordConfig(
                new AudioRecordConfig(8192,
                        882,
                        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                        new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(44100)
                                .setChannelMask(AudioFormat.CHANNEL_IN_FRONT)
                                .build()));
        doorplateAudioMediaRecord.init(
                new MediaRecordCallBack() {
                    @Override
                    public void recording(MediaData mediaData) {
                        if(MainActivity.mVideoRecordStarted.get() && !MyVideoEncoder.mAudioThreadCancel.get()){
                            MyVideoEncoder.mAudioOutBufferQueue.offer((AudioData)mediaData);
                        }
                        if(doorplateAudioMediaRecord.isRecording()){
                            mAudioMediaRecordDataQueue.offer((AudioData)mediaData);
                        }
                    }
                });
        doorplateAudioMediaRecord.record();

        doorplateAudioMediaEncode = new DoorplateAudioMediaEncode();
        doorplateAudioMediaEncode.setMediaRecordDataQueue(mAudioMediaRecordDataQueue);
        doorplateAudioMediaEncode.setMediaEncodeDataQueue(mAudioMediaEncodeDataQueue);
        doorplateAudioMediaEncode.configAudioMediaEncodeConfig(new AudioMediaEncodeConfig());
        doorplateAudioMediaEncode.init();
        doorplateAudioMediaEncode.start();

        MainActivity.videoSource.setVideoSourceCallback(new VideoSource.VideoSourceCallback() {
            @Override
            public void onFrameCaptured(VideoFrame videoFrame) {
                if (doorplateVideoMediaRecordIsStart.get()) {
                    mVideoMediaRecordDataQueue.offer(new VideoData(videoFrame));
                }
            }
        });

        doorplateVideoMediaEncode = new DoorplateVideoMediaEncode();
        doorplateVideoMediaEncode.setMediaRecordDataQueue(mVideoMediaRecordDataQueue);
        doorplateVideoMediaEncode.setMediaEncodeDataQueue(mVideoMediaEncodeDataQueue);
        doorplateVideoMediaEncode.configVideoMediaEncodeConfig(new VideoMediaEncodeConfig(640, 480));
        doorplateVideoMediaEncode.init();
        doorplateVideoMediaEncode.start();

        doorplateMediaMuxer = new DoorplateMediaMuxer();
        doorplateMediaMuxer.init(InternalMemory.getMemoryPath("test1.mp4"));
        doorplateMediaMuxer.setVideoMediaEncodeDataQueue(mVideoMediaEncodeDataQueue);
        doorplateMediaMuxer.setAudioMediaEncodeDataQueue(mAudioMediaEncodeDataQueue);
        doorplateMediaMuxer.start();



    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(doorplateAudioMediaRecord!=null){
            doorplateAudioMediaRecord.release();
        }
        if(doorplateAudioMediaEncode!=null){
            doorplateAudioMediaEncode.release();
        }

    }

    public void stop(){
        doorplateMediaMuxer.release();
    }
}
