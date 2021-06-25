package com.example.myapplicationdfsd.softWareSystem.service.communicator;

public class AbstractSignalingCommunicator extends AbstractCommunicator implements RoomCommunicationInterface{

    protected String RoomName;
    protected String communicatorNumber;
    @Override
    void init() {

    }

    @Override
    void connect() {

    }

    @Override
    void disconnect() {

    }

    @Override
    void reconnect() {

    }

    @Override
    void release() {

    }

    @Override
    public void createOrJoinRoom(String roomNumber) {

    }

    @Override
    public AbstractMessage receiveMessage() {
        return null;
    }

    @Override
    public void sendMessage(AbstractMessage message) {

    }

    @Override
    public String getRoomNumber() {
        return RoomName;
    }

    @Override
    public String getCommunicatorNumber() {
        return communicatorNumber;
    }
}
