package com.duynguyen.network;

public interface IMessageHandler {

    void onMessage(Message message);

    void onConnectionFail();

    void onDisconnected();

    void onConnectOK();

    void messageNotMap(Message ms);

    void messageSubCommand(Message ms);

    void messageNotLogin(Message ms);

    void newMessage(Message ms);

}
