package com.example.myapplicationdfsd.software.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.example.myapplicationdfsd.software.service.communicator.P2PCommunicationInterface;
import com.example.myapplicationdfsd.software.service.communicator.SignalingClientInterface;

import java.util.List;

import androidx.annotation.Nullable;

public class P2PCommunicationService extends Service {
    public static final String TAG = "P2PCommunicationService";
    private MyBinder binder = new MyBinder();

    private List<P2PCommunicationInterface>  WebRTCP2PCommunicatorList;
    private SignalingClientInterface signalingClient;


    public class MyBinder extends Binder {
        public P2PCommunicationService getService(){
            return P2PCommunicationService.this;
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();

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

    }

    public void connectSignaling(){
        signalingClient.connectSignaling();

    }
}
