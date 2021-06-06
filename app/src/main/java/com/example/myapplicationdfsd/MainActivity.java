package com.example.myapplicationdfsd;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import org.webrtc.YuvHelper;
import org.webrtc.audio.WebRtcAudioRecord;

import java.nio.ByteBuffer;
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



    //权限
    private final static int PERMISSIONS_REQUEST_CODE = 1;
    private final String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
    ,Manifest.permission.WRITE_EXTERNAL_STORAGE};

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

        speaker = (Button) findViewById(R.id.speaker);
        button = (Button) findViewById(R.id.button);
        button2 = (Button) findViewById(R.id.button2);
        button5 = (Button) findViewById(R.id.button5);
        button6 = (Button) findViewById(R.id.button6);
        button3 = (Button) findViewById(R.id.button3);

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
                if(mVideoRecordStarted.get()){
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
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                super.onIceConnectionChange(iceConnectionState);
                if (PeerConnection.IceConnectionState.DISCONNECTED.equals(iceConnectionState)) {
                    runOnUiThread(() -> {
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
                    remoteVideoTrack.addSink(remoteViews[(remoteViewsIndex++) % 3]);
                });
            }
        });
        peerConnection.addStream(mediaStream);
        peerConnectionMap.put(socketId, peerConnection);
        return peerConnection;
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
    public void onPeerLeave(String msg) {

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
        if (mVideoEncoder != null){
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
        SignalingClient.get().destroy();
        peerConnectionMap.forEach((key,value)->{
            value.close();
        });
        peerConnectionMap.clear();
        remoteViews[0].clearImage();
        remoteViews[1].clearImage();
        remoteViews[2].clearImage();
    }

    public void startConnect(View view) {
        SignalingClient.get().init(this);

    }

    public void function(View view) {
        if(mFunctionOpen.get()){
            mFunctionOpen.set(false);
            speaker.setAlpha(0);
            button.setAlpha(0);
            button2.setAlpha(0);
            button5.setAlpha(0);
            button6.setAlpha(0);
            button3.setAlpha(0);
        }else {
            mFunctionOpen.set(true);
            speaker.setAlpha(1);
            button.setAlpha(1);
            button2.setAlpha(1);
            button5.setAlpha(1);
            button6.setAlpha(1);
            button3.setAlpha(1);
        }

    }
}
