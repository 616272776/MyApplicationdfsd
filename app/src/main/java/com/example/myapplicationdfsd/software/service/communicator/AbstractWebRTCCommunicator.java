package com.example.myapplicationdfsd.software.service.communicator;

import android.content.Context;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractWebRTCCommunicator implements P2PCommunicationInterface {

    private SignalingClientInterface signalingCommunicator;

    EglBase.Context eglBaseContext;
    PeerConnectionFactory peerConnectionFactory;
    SurfaceViewRenderer localView;
    MediaStream mediaStream;
    List<PeerConnection.IceServer> iceServers;
    HashMap<String, PeerConnection> peerConnectionMap;

    private Context applicationContext;
    //视频存储
    public static VideoSource videoSource;
    private VideoTrack videoTrack;
    private AudioTrack audioTrack;

    @Override
    public void init() {
        peerConnectionMap = new HashMap<>();
        iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:139.224.12.1").createIceServer());
        eglBaseContext = EglBase.create().getEglBaseContext();


        createPeerConnectionFactory();
        createVideoCapturer();
        createVideoTrack();
        createMediaStream();
    }

    private void createMediaStream() {
        mediaStream = peerConnectionFactory.createLocalMediaStream("mediaStream");
        mediaStream.addTrack(videoTrack);
        mediaStream.addTrack(audioTrack);
    }

    private void createPeerConnectionFactory() {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions
                .builder(applicationContext)
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
    }

    private void createVideoCapturer() {
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext);
        VideoCapturer videoCapturer = createCameraCapturer(true);
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());

        videoCapturer.initialize(surfaceTextureHelper, applicationContext, videoSource.getCapturerObserver());
        videoCapturer.startCapture(1920,1080, 30);
    }

    private void createVideoTrack() {
        // create VideoTrack
        videoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        MediaConstraints constraints = new MediaConstraints();
        AudioSource audioSource = peerConnectionFactory.createAudioSource(constraints);
        audioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);
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

    public void setApplicationContext(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void reconnect() {

    }

    @Override
    public void release() {

    }

    public void configApplicationContent(Context context) {
        this.applicationContext = context;
    }

    public void configurateSignalingCommunicator(SignalingClientInterface roomCommunicator) {
        this.signalingCommunicator = roomCommunicator;
    }

    public boolean signalingHasConfigurated() {
        if (signalingCommunicator == null) {
            return false;
        } else {
            return true;
        }
    }
}
