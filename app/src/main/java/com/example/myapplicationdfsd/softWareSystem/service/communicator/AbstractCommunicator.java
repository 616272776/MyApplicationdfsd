package com.example.myapplicationdfsd.softWareSystem.service.communicator;

public abstract class AbstractCommunicator {
    abstract void init();
    abstract void connect();
    abstract void disconnect();
    abstract void reconnect();
    abstract void release();
}
