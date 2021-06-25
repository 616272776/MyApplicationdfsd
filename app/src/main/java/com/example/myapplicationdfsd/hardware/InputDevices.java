package com.example.myapplicationdfsd.hardware;

import java.util.concurrent.atomic.AtomicBoolean;

public interface InputDevices {
    AtomicBoolean active = new AtomicBoolean(false);

    void active();
    void inactive();
}
