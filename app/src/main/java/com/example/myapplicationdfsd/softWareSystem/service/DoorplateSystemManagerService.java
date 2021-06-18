package com.example.myapplicationdfsd.softWareSystem.service;

import android.app.Service;
import android.app.SmatekManager;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.myapplicationdfsd.MainActivity;
import com.example.myapplicationdfsd.activity.MediaShowActivity;
import com.example.myapplicationdfsd.activity.PoliceAffairsActivity;
import com.example.myapplicationdfsd.hardWareSystem.Button;
import com.example.myapplicationdfsd.hardWareSystem.DoorplateFunctionButtonGroup;
import com.example.myapplicationdfsd.hardWareSystem.GPIOButton;

import java.io.Serializable;

public class DoorplateSystemManagerService extends Service {

    public static final String TAG = "DoorplateSystemManagerService";

    public static SmatekManager smatekManager;
    private DoorplateFunctionButtonGroup doorplateFunctionButtonGroup;

//    public class MyBinder extends Binder{
//        public DoorplateSystemManagerService getService(){
//            return DoorplateSystemManagerService.this;
//        }
//    }


//    private MyBinder binder = new MyBinder();

    @Override
    public void onCreate() {
        super.onCreate();

        //init button group
        this.doorplateFunctionButtonGroup = new DoorplateFunctionButtonGroup(smatekManager);
        GPIOButton gpio2Button = new GPIOButton(GPIOButton.GPIO_PATH+'2', GPIOButton.ONE,
                GPIOButton.ZERO,
                DoorplateFunctionButtonGroup.GPIO_2_BUTTON_NAME,
                new Button.Callback() {
                    @Override
                    public void onClick() {
                        if(MainActivity.startConnect.get()){
                            MainActivity.startConnect.set(false);
                            Intent dialogIntent = new Intent(getBaseContext(), PoliceAffairsActivity.class);
                            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //必须添加 Intent.FLAG_ACTIVITY_NEW_TASK
                            getApplication().startActivity(dialogIntent);
                        }else{
                            MainActivity.startConnect.set(true);
                            Intent dialogIntent = new Intent(getBaseContext(), MediaShowActivity.class);
                            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //必须添加 Intent.FLAG_ACTIVITY_NEW_TASK
                            getApplication().startActivity(dialogIntent);
                        }
                    }

                    @Override
                    public void onDoubleClick() {

                    }

                    @Override
                    public void onLongPress() {

                    }
                });
        doorplateFunctionButtonGroup.addButton(gpio2Button);
        doorplateFunctionButtonGroup.init();

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

        if(this.smatekManager==null){
            throw new IllegalArgumentException("smatekManager is not init");
        }

        Log.e(TAG, "onStartCommand");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
