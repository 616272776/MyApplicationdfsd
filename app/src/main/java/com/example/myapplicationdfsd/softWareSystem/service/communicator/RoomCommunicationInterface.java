package com.example.myapplicationdfsd.softWareSystem.service.communicator;

public interface RoomCommunicationInterface {
    void createOrJoinRoom(String roomNumber);
    AbstractMessage receiveMessage();
    void sendMessage(AbstractMessage message);
    String getRoomNumber();
    String getCommunicatorNumber();
}
