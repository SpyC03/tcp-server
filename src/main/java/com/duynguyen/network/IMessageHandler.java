package com.duynguyen.network;

public interface IMessageHandler {

    void onMessage(Message message);

    void onConnectionFail();

    void onDisconnected();

    void onConnectOK();


    void messageInGame(Message ms);

    void messageNotInGame(Message ms);

    void newMessage(Message ms);


}
