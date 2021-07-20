package com.example.myapplicationdfsd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SmatekManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplicationdfsd.software.activity.PoliceAffairsActivity;
import com.example.myapplicationdfsd.software.service.P2PCommunicationService;
import com.example.myapplicationdfsd.software.service.DoorplateSystemManagerService;
import com.example.myapplicationdfsd.software.service.MediaService;
import com.example.myapplicationdfsd.software.service.communicator.DoorplateWebRTCCommunicator;
import com.example.myapplicationdfsd.software.service.communicator.webrtc.PeerConnectionAdapter;
import com.example.myapplicationdfsd.software.service.communicator.webrtc.SdpAdapter;
import com.example.myapplicationdfsd.software.service.communicator.webrtc.SignalingClient;

import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.HardwareVideoDecoderFactory;
import org.webrtc.HardwareVideoEncoderFactory;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements SignalingClient.Callback {

    private static final String TAG = "MainActivity";
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
//    int remoteViewsIndex = 0;
    private int currVolume;
    private boolean onSpeaker = false;

//    private Button speaker;
//    private Button button;
//    private Button button2;
//    private Button button5;
//    private Button button6;
//    private Button button3;
    private AtomicBoolean mFunctionOpen = new AtomicBoolean(false);

    private AtomicBoolean mEquipmentNormal = new AtomicBoolean(false);
    public static AtomicBoolean startConnect = new AtomicBoolean(false);

    private MediaService mediaService;
    private P2PCommunicationService communicationService;

    private ServiceConnection mediaServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaService.MyBinder binder = (MediaService.MyBinder) service;
            mediaService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };
    private ServiceConnection communicationConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            P2PCommunicationService.MyBinder binder = (P2PCommunicationService.MyBinder) service;
            communicationService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };


    //权限
    private final static int PERMISSIONS_REQUEST_CODE = 1;
    private final String[] permissions = {Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    //视频存储
    public static VideoSource videoSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermission();
        }
    }

    @SuppressLint("WrongConstant")
    private void init() {
        //门牌管理设备
        DoorplateSystemManagerService.smatekManager = (SmatekManager) getSystemService("smatek");
        Intent intent = new Intent(this, DoorplateSystemManagerService.class);
        startService(intent);

//        speaker = (Button) findViewById(R.id.speaker);
//        button = (Button) findViewById(R.id.button);
//        button2 = (Button) findViewById(R.id.button2);
//        button5 = (Button) findViewById(R.id.button5);
//        button6 = (Button) findViewById(R.id.button6);
//        button3 = (Button) findViewById(R.id.button3);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //最大音量
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        //当前音量
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);


        peerConnectionMap = new HashMap<>();
        iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:139.224.12.1").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:139.224.12.1").setUsername("test").setPassword("123456").createIceServer());
        eglBaseContext = EglBase.create().getEglBaseContext();

        // create PeerConnectionFactory
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions
                .builder(this)
                .createInitializationOptions());
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();


        DefaultVideoEncoderFactory defaultVideoEncoderFactory =
                new DefaultVideoEncoderFactory(eglBaseContext, false, true);
        DefaultVideoDecoderFactory defaultMyVideoDecoderFactory =
                new DefaultVideoDecoderFactory(eglBaseContext);
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
//                .setVideoEncoderFactory(new HardwareVideoEncoderFactory(eglBaseContext, false, true))
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(new HardwareVideoDecoderFactory(eglBaseContext))
                .createPeerConnectionFactory();

        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext);
        // create VideoCapturer
        VideoCapturer videoCapturer = createCameraCapturer(true);
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());

        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        videoCapturer.startCapture(1280, 720, 30);


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

        if (DoorplateSystemManagerService.smatekManager == null) {
            SignalingClient.room = "1";

        } else {
            String macAddress = DoorplateSystemManagerService.smatekManager.getEthMacAddress();
            String url = "http://139.224.12.1:20880/getDoorplate/" + macAddress;
            OkHttpClient okHttpClient = new OkHttpClient();
            final Request request = new Request.Builder()
                    .url(url)
                    .get()//默认就是GET请求，可以不写
                    .build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d(TAG, "onFailure: ");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String doorplateNumber = response.body().string();
                    Log.d(TAG, "onResponse: " + doorplateNumber);
                    if (doorplateNumber.contains("false")) {

                    } else {
//                    startConnect(null);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, String.format("查询到门牌为%s，加入房间%s", doorplateNumber, doorplateNumber), Toast.LENGTH_SHORT).show();
                                SignalingClient.room = doorplateNumber.substring(22, 40);
                                SignalingClient.room = "520502103003011111";
                                startConnect(null);
                            }
                        });
                    }
                }
            });

        }


//        function(null);
//        startConnect(null);

        Intent mediaServiceIntent = new Intent(this, MediaService.class);
        bindService(mediaServiceIntent, mediaServiceConnection, Context.BIND_AUTO_CREATE);
        startService(mediaServiceIntent);

        Intent webRTCCommunicatorServer = new Intent(this, P2PCommunicationService.class);
        bindService(webRTCCommunicatorServer, communicationConnection, Context.BIND_AUTO_CREATE);


        Intent policeAffairsActivity = new Intent(this, PoliceAffairsActivity.class);
//        startActivity(policeAffairsActivity);
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
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        PeerConnection peerConnection = getOrCreatePeerConnection(socketId);
        peerConnection.createOffer(new SdpAdapter("createOfferSdp:" + socketId) {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                peerConnection.setLocalDescription(new SdpAdapter("setLocalSdp:" + socketId), sessionDescription);
                SignalingClient.get().sendSessionDescription(sessionDescription, socketId);
            }
        }, mediaConstraints);
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
        if (peerConnection == null) {

        } else {
            peerConnectionMap.remove(socketId);
            peerConnection.dispose();
        }
    }

    @Override
    public void onOfferReceived(JSONObject data) {
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
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
            }, mediaConstraints);

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
        mediaService.stop();
    }

    public void startRecord(View view) {
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
        if (!index) {
            SignalingClient.get().init(this);
            index = true;
        } else {
            SignalingClient.get().reconnect();
        }

        startConnect.set(true);

    }
}
