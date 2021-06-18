package com.example.myapplicationdfsd.softWareSystem.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class mediaService extends Service {
    public static final String TAG = "mediaService";

//    public class MyBinder extends Binder{
//        public DoorplateSystemManagerService getService(){
//            return DoorplateSystemManagerService.this;
//        }
//    }


//    private MyBinder binder = new MyBinder();

    @Override
    public void onCreate() {
        super.onCreate();


    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
//        return binder;
        return null;
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
}
