package com.example.myapplicationdfsd.hardWareSystem;

import java.util.concurrent.atomic.AtomicBoolean;

public interface InputDevices {
    AtomicBoolean active = new AtomicBoolean(false);

    void active();
    void inactive();
}
