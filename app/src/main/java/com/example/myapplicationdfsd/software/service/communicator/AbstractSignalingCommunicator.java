package com.example.myapplicationdfsd.software.service.communicator;

public class AbstractSignalingCommunicator  implements RoomCommunicationInterface,CommunicatorInterface{

    protected String RoomName;
    protected String communicatorNumber;


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

    @Override
    public void init() {

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
}
