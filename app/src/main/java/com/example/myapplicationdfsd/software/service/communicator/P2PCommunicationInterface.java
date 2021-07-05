package com.example.myapplicationdfsd.software.service.communicator;

public interface P2PCommunicationInterface extends CommunicatorInterface {
    P2PCommunicationInterface buildCommunicator(P2PConnectParameter p2PConnectParameter);
    void changeConnectParameter(P2PConnectParameter p2PConnectParameter);
    void connectP2P(P2PConnectParameter p2PConnectParameter);
    void disconnectP2P();
    P2PConnectStatus getConnectStatus();
}
