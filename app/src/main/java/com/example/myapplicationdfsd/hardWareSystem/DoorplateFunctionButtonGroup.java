package com.example.myapplicationdfsd.hardWareSystem;

import android.app.SmatekManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class DoorplateFunctionButtonGroup implements InputDevices {

    public static final String GPIO_2_BUTTON_NAME = "gpio2Button";

    private SmatekManager smatekManager;
    private Map<String,GPIOButton> gpioButtonGroup = new HashMap<>();

    public DoorplateFunctionButtonGroup(SmatekManager smatekManager) {
        this.smatekManager = smatekManager;
    }

    public void addButton(GPIOButton gpioButton) {
        gpioButtonGroup.put(gpioButton.getmButtonName(),gpioButton);

    }


    public void init() {
        gpioButtonGroup.forEach((key,value)->{
            value.active();

            smatekManager.writeToNode(value.getmGIIOPath(), value.getmInitValue());
            new Thread(){
                @Override
                public void run() {
                    super.run();
                    while (value.isActive()){
                        // todo 单机和双击还不够
                        try {
                            Thread.sleep(400);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        String dataFromNode = smatekManager.getDataFromNode(value.getmGIIOPath());
                        if(value.getmActivityValue().equals(dataFromNode)){
                            value.onClick();
                        }
                    }
                }
            }.start();
        });
    }

}
