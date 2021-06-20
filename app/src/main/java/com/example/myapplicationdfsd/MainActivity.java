package com.example.myapplicationdfsd;

import android.Manifest;
import android.app.SmatekManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplicationdfsd.softWareSystem.service.DoorplateSystemManagerService;
import com.example.myapplicationdfsd.softWareSystem.service.media.capture.DoorplateAudioMediaRecord;
import com.example.myapplicationdfsd.softWareSystem.service.media.capture.MediaRecord;
import com.example.myapplicationdfsd.softWareSystem.service.media.capture.config.AudioRecordConfig;

import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements SignalingClient.Callback {

    EglBase.Context eglBaseContext;
    PeerConnectionFactory peerConnectionFactory;
    SurfaceViewRenderer localView;
    MediaStream mediaStream;
    List<PeerConnection.IceServer> iceServers;

    HashMap<String, PeerConnection> peerConnectionMap;
    private String surfaceIndex0;
    private String surfaceIndex1;
    private String surfaceIndex2;
    SurfaceViewRenderer[] remoteViews;
    int remoteViewsIndex = 0;
    private int currVolume;
    private boolean onSpeaker = false;

    private PeerConnection mPeerConnection;

    private Button speaker;
    private Button button;
    private Button button2;
    private Button button5;
    private Button button6;
    private Button button3;
    private AtomicBoolean mFunctionOpen = new AtomicBoolean(false);

    private AtomicBoolean mEquipmentNormal = new AtomicBoolean(false);
    public static AtomicBoolean startConnect = new AtomicBoolean(false);


    //权限
    private final static int PERMISSIONS_REQUEST_CODE = 1;
    private final String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
            , Manifest.permission.WRITE_EXTERNAL_STORAGE};

    //视频存储
    MyVideoEncoder mVideoEncoder = null;
    private AtomicBoolean mVideoRecordStarted = new AtomicBoolean(false);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermission();
        }
    }

    private void init() {
        //门牌管理设备
        DoorplateSystemManagerService.smatekManager = (SmatekManager) getSystemService("smatek");
        Intent intent = new Intent(this, DoorplateSystemManagerService.class);
        startService(intent);

        // todo 门牌音频编码测试
        DoorplateAudioMediaRecord doorplateAudioMediaRecord = DoorplateAudioMediaRecord.getInstance();
        doorplateAudioMediaRecord.init(
                new AudioRecordConfig(8192,
                        882,
                        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                        new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(44100)
                        .setChannelMask(AudioFormat.CHANNEL_IN_FRONT)
                        .build()),
                        JavaAudioDeviceModule.MyAudioInput);
        
        doorplateAudioMediaRecord.record();


        checkEquipment();

        speaker = (Button) findViewById(R.id.speaker);
        button = (Button) findViewById(R.id.button);
        button2 = (Button) findViewById(R.id.button2);
        button5 = (Button) findViewById(R.id.button5);
        button6 = (Button) findViewById(R.id.button6);
        button3 = (Button) findViewById(R.id.button3);

//        // 按钮
//        ButtonComponent buttonComponent = ButtonComponent.getInstance().prepare(this);
//        buttonComponent.setMainButtonCallback(new ButtonComponent.ButtonCallback() {
//            @Override
//            public void onGPIO1() {
//
//            }
//
//            @Override
//            public void onGPIO2() {
//                if (startConnect.get()) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            closeConnect(null);
//                        }
//                    });
//
//                } else {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            startConnect(null);
//                        }
//                    });
//
//                }
//
//            }
//        });


        if (mEquipmentNormal.get()) {

            //视频存储
            if (mVideoEncoder == null) {
                mVideoEncoder = new MyVideoEncoder();
                mVideoEncoder.init("/sdcard/test_out.mp4", 640, 480);
            }


            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            //最大音量
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            //当前音量
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);


            peerConnectionMap = new HashMap<>();
            iceServers = new ArrayList<>();
            iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
            iceServers.add(PeerConnection.IceServer.builder("stun:139.224.12.1").createIceServer());

            eglBaseContext = EglBase.create().getEglBaseContext();

            // create PeerConnectionFactory
            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions
                    .builder(this)
                    .createInitializationOptions());
            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            DefaultVideoEncoderFactory defaultVideoEncoderFactory =
                    new DefaultVideoEncoderFactory(eglBaseContext, true, true);
            DefaultVideoDecoderFactory defaultMyVideoDecoderFactory =
                    new DefaultVideoDecoderFactory(eglBaseContext);
            peerConnectionFactory = PeerConnectionFactory.builder()
                    .setOptions(options)
                    .setVideoEncoderFactory(defaultVideoEncoderFactory)
                    .setVideoDecoderFactory(defaultMyVideoDecoderFactory)
                    .createPeerConnectionFactory();

            SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext);
            // create VideoCapturer
            VideoCapturer videoCapturer = createCameraCapturer(true);
            VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());

            videoSource.setVideoSourceCallback(new VideoSource.VideoSourceCallback() {
                @Override
                public void onFrameCaptured(VideoFrame videoFrame) {
                    if (mVideoRecordStarted.get()) {
                        mVideoEncoder.encode(videoFrame);
                    }
                }
            });


            videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
            videoCapturer.startCapture(640, 480, 30);


            localView = findViewById(R.id.localView);
            localView.setMirror(true);
            localView.init(eglBaseContext, null);

            // create VideoTrack
            VideoTrack videoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

            MediaConstraints constraints = new MediaConstraints();
            AudioSource audioSource = peerConnectionFactory.createAudioSource(constraints);
            AudioTrack audioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);


//        // display in localView
            videoTrack.addSink(localView);


            remoteViews = new SurfaceViewRenderer[]{
                    findViewById(R.id.remoteView),
                    findViewById(R.id.remoteView2),
                    findViewById(R.id.remoteView3),
            };
            for (SurfaceViewRenderer remoteView : remoteViews) {
                remoteView.setMirror(false);
                remoteView.init(eglBaseContext, null);
            }

            mediaStream = peerConnectionFactory.createLocalMediaStream("mediaStream");
            mediaStream.addTrack(videoTrack);
            mediaStream.addTrack(audioTrack);
        }
//        function(null);
    }

    private void checkEquipment() {
        Camera1Enumerator enumerator = new Camera1Enumerator(false);
        final String[] deviceNames = enumerator.getDeviceNames();

        if (deviceNames.length > 0) {
            mEquipmentNormal.set(true);
        }

    }

    private void requestPermission() {
        if (!checkPermissionAllGranted()) {
            ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMISSIONS_REQUEST_CODE);
        } else {
            init();
        }
    }

    /**
     * 检查是否拥有指定的所有权限
     */
    private boolean checkPermissionAllGranted() {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                //Log.e("err","权限"+permission+"没有授权");
                return false;
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean isAllGranted = true;
            // 判断是否所有的权限都已经授予了
            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }
            if (isAllGranted) {
                // 所有的权限都授予了
                init();
            } else {
                requestPermission();
                Toast.makeText(this, "申请权限", Toast.LENGTH_SHORT).show();
                // 弹出对话框告诉用户需要权限的原因, 并引导用户去应用权限管理中手动打开权限按钮
                //容易判断错
                //MyDialog("提示", "某些权限未开启,请手动开启", 1) ;
            }
        }
    }

    //打开扬声器
    public void OpenSpeaker() {

        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            //audioManager.setMode(AudioManager.ROUTE_SPEAKER);
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            currVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);

            if (!audioManager.isSpeakerphoneOn()) {
                audioManager.setSpeakerphoneOn(true);

                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                        AudioManager.STREAM_VOICE_CALL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "扬声器已经打开", Toast.LENGTH_SHORT).show();
    }


    //关闭扬声器
    public void CloseSpeaker() {

        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                if (audioManager.isSpeakerphoneOn()) {
                    audioManager.setSpeakerphoneOn(false);
                    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, currVolume,
                            AudioManager.STREAM_VOICE_CALL);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "扬声器已经关闭", Toast.LENGTH_SHORT).show();
    }


    private synchronized PeerConnection getOrCreatePeerConnection(String socketId) {


        PeerConnection peerConnection = peerConnectionMap.get(socketId);
        if (peerConnection != null) {
            return peerConnection;
        }
        peerConnection = peerConnectionFactory.createPeerConnection(iceServers, new PeerConnectionAdapter("PC:" + socketId) {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                SignalingClient.get().sendIceCandidate(iceCandidate, socketId);
            }

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                super.onSignalingChange(signalingState);
//                remoteViews[0].clearImage();
//                remoteViews[1].clearImage();
//                remoteViews[2].clearImage();
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                super.onIceConnectionChange(iceConnectionState);
                if (PeerConnection.IceConnectionState.DISCONNECTED.equals(iceConnectionState)) {
                    runOnUiThread(() -> {
                        if (socketId.equals(surfaceIndex0)) {
                            surfaceIndex0 = null;
                        } else if (socketId.equals(surfaceIndex1)) {
                            surfaceIndex1 = null;
                        } else if (socketId.equals(surfaceIndex2)) {
                            surfaceIndex2 = null;
                        }
                        // 清理下peerconnectMap
                        remoteViews[0].clearImage();
                        remoteViews[1].clearImage();
                        remoteViews[2].clearImage();
                    });
                }
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                runOnUiThread(() -> {
                    int index;
                    if (surfaceIndex0 == null) {
                        surfaceIndex0 = socketId;
                        index = 0;
                    } else if (surfaceIndex1 == null) {
                        surfaceIndex1 = socketId;
                        index = 1;
                    } else if (surfaceIndex2 == null) {
                        surfaceIndex2 = socketId;
                        index = 2;
                    } else {
                        index = -1;
                    }

                    if (index != -1) {
                        remoteVideoTrack.addSink(remoteViews[index]);
                    }
                });
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                super.onRemoveStream(mediaStream);

            }
        });
        peerConnection.addStream(mediaStream);
        peerConnectionMap.put(socketId, peerConnection);


        return peerConnection;
    }


    @Override
    public void socketConnectError() {
        Toast.makeText(this, "socket连接失败", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreateRoom(String socketId) {

    }

    @Override
    public void onPeerJoined(String socketId) {
        PeerConnection peerConnection = getOrCreatePeerConnection(socketId);
        peerConnection.createOffer(new SdpAdapter("createOfferSdp:" + socketId) {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                peerConnection.setLocalDescription(new SdpAdapter("setLocalSdp:" + socketId), sessionDescription);
                SignalingClient.get().sendSessionDescription(sessionDescription, socketId);
            }
        }, new MediaConstraints());
    }

    @Override
    public void onSelfJoined(String socketId) {

    }

    @Override
    public void onPeerLeave(String msg, String socketId) {
        if (socketId.equals(surfaceIndex0)) {
            surfaceIndex0 = null;
        } else if (socketId.equals(surfaceIndex1)) {
            surfaceIndex1 = null;
        } else if (socketId.equals(surfaceIndex2)) {
            surfaceIndex2 = null;
        }

        remoteViews[0].clearImage();
        remoteViews[1].clearImage();
        remoteViews[2].clearImage();
        PeerConnection peerConnection = peerConnectionMap.get(socketId);
        if(peerConnection==null){

        }else {
            peerConnectionMap.remove(socketId);
            peerConnection.dispose();
        }
    }

    @Override
    public void onOfferReceived(JSONObject data) {
        runOnUiThread(() -> {
            final String socketId = data.optString("from");
            PeerConnection peerConnection = getOrCreatePeerConnection(socketId);
            peerConnection.setRemoteDescription(new SdpAdapter("setRemoteSdp:" + socketId),
                    new SessionDescription(SessionDescription.Type.OFFER, data.optString("sdp")));
            peerConnection.createAnswer(new SdpAdapter("localAnswerSdp") {
                @Override
                public void onCreateSuccess(SessionDescription sdp) {
                    super.onCreateSuccess(sdp);
                    peerConnectionMap.get(socketId).setLocalDescription(new SdpAdapter("setLocalSdp:" + socketId), sdp);
                    SignalingClient.get().sendSessionDescription(sdp, socketId);
                }
            }, new MediaConstraints());

        });
    }

    @Override
    public void onAnswerReceived(JSONObject data) {
        String socketId = data.optString("from");
        PeerConnection peerConnection = getOrCreatePeerConnection(socketId);
        peerConnection.setRemoteDescription(new SdpAdapter("setRemoteSdp:" + socketId),
                new SessionDescription(SessionDescription.Type.ANSWER, data.optString("sdp")));
    }

    @Override
    public void onIceCandidateReceived(JSONObject data) {
        String socketId = data.optString("from");
        PeerConnection peerConnection = getOrCreatePeerConnection(socketId);
        peerConnection.addIceCandidate(new IceCandidate(
                data.optString("id"),
                data.optInt("label"),
                data.optString("candidate")
        ));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SignalingClient.get().destroy();
    }

    private VideoCapturer createCameraCapturer(boolean isFront) {
        Camera1Enumerator enumerator = new Camera1Enumerator(false);
        final String[] deviceNames = enumerator.getDeviceNames();
        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (isFront ? enumerator.isFrontFacing(deviceName) : enumerator.isBackFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    public void speaker(View view) {
        if (onSpeaker) {
            CloseSpeaker();
            onSpeaker = false;
        } else {
            OpenSpeaker();
            onSpeaker = true;
        }
    }

    public void stopRecord(View view) {
        if (mVideoEncoder != null) {
            mVideoEncoder.release();
        }
        mVideoRecordStarted.set(false);
        Toast.makeText(this, "停止录制", Toast.LENGTH_SHORT).show();
    }

    public void startRecord(View view) {
        mVideoEncoder.prepareEncoder();
        mVideoRecordStarted.set(true);

    }

    public void closeConnect(View view) {
        SignalingClient.get().close();
        peerConnectionMap.forEach((key, value) -> {
            value.close();
        });
        peerConnectionMap.clear();
        remoteViews[0].clearImage();
        remoteViews[1].clearImage();
        remoteViews[2].clearImage();

        surfaceIndex0 = null;
        surfaceIndex1 = null;
        surfaceIndex2 = null;

        startConnect.set(false);
    }

    private boolean index = false;
    public void startConnect(View view) {
        if(!index){
            SignalingClient.get().init(this);
            index=true;
        }else {
            SignalingClient.get().reconnect();
        }

        startConnect.set(true);

    }

    // 音频源：音频输入-麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;

    // 采样率
    // 44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    // 采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
    private final static int AUDIO_SAMPLE_RATE = 44100;

    // 音频通道 单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;

    // 音频格式：PCM编码
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private byte[] buffer;
    private boolean isRecording;



    public void function(View view) {
        MyAudioRecordThread myAudioRecordThread = new MyAudioRecordThread(new MyAudioRecordThread.Callback() {
            @Override
            public void inited() {

            }

            @Override
            public void recording(AudioData audioData) {
if(mVideoRecordStarted.get() && !MyVideoEncoder.mAudioThreadCancel.get()){
    MyVideoEncoder.mAudioOutBufferQueue.offer(audioData);
}
            }

            @Override
            public void stop() {

            }

            @Override
            public void release() {

            }
        });
        myAudioRecordThread.init();
        Thread thread = new Thread(myAudioRecordThread);
        thread.start();
    }
}
