package com.example.myapplicationdfsd.software.service.communicator;

public interface SignalingClientInterface {
   void connectSignaling();
   void disconnectSignaling();
   void setCallback(SignalingClientCallback callback);

   interface SignalingClientCallback{
      void OnPeepConnect();
      void OnPeepLeave();
   }
}
