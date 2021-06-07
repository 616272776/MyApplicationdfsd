package com.example.myapplicationdfsd.component;

import android.annotation.SuppressLint;
import android.app.SmatekManager;
import android.content.Context;
import android.util.Log;

public class ButtonComponent {

    private static final String TAG = ButtonComponent.class.getSimpleName();
    private static ButtonComponent buttonComponent;


    // 常量
    private final static String ZERO = "0";
    private final static String ONE = "1";
    private final static String GPIO1 = "sys/class/smatek_gpio/gpio5c1";
    private final static String GPIO2 = "sys/class/smatek_gpio/gpio5c2";


    // 回调
    private ButtonCallback mMainButtonCallback;
    private ButtonCallback mAlarmButtonCallback;
    private ButtonCallback mWarningButtonCallback;

    // 系统管理
    private SmatekManager smatekManager;

    public static ButtonComponent getInstance(){
        if(buttonComponent==null){
            buttonComponent = new ButtonComponent();
        }
        return buttonComponent;
    }

    @SuppressLint("WrongConstant")
    public ButtonComponent prepare(Context context) {
        smatekManager = (SmatekManager) context.getSystemService("smatek");

        if(smatekManager == null){
            return getInstance();
        }

        // 初始化按钮
        smatekManager.writeToNode(GPIO1, ZERO); //写入1是打开，写入0 是关闭
        smatekManager.writeToNode(GPIO2, ONE); //写入1是打开，写入0 是关闭
        new Thread() {
            @Override
            public void run() {
                super.run();
                while (true) {
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String gpio1Result = smatekManager.getDataFromNode(GPIO1);
                    String gpio2Result = smatekManager.getDataFromNode(GPIO2);

                    // gpio1是解除按钮
                    if (ONE.equals(gpio1Result)) {
                        Log.i(TAG, "button: " + GPIO1);
                        if (mAlarmButtonCallback != null) {
                            mAlarmButtonCallback.onGPIO1();
                        }
                        if (mWarningButtonCallback != null) {
                            mWarningButtonCallback.onGPIO1();
                        }
                    }
                    // gpio2是报警按钮
                    if (ZERO.equals(gpio2Result)) {
                        Log.i(TAG, "button: " + GPIO2);
                        if (mMainButtonCallback != null) {
                            mMainButtonCallback.onGPIO2();
                        }
                    }
                }
            }
        }.start();
        return getInstance();
    }

    public void setMainButtonCallback(ButtonCallback mMainButtonCallback) {
        this.mMainButtonCallback = mMainButtonCallback;
    }

    public void setAlarmButtonCallback(ButtonCallback mAlarmButtonCallback) {
        this.mAlarmButtonCallback = mAlarmButtonCallback;
    }

    public void setWarningButtonCallback(ButtonCallback mWarningButtonCallback) {
        this.mWarningButtonCallback = mWarningButtonCallback;
    }

    public interface ButtonCallback {
        void onGPIO1();

        void onGPIO2();
    }
}
