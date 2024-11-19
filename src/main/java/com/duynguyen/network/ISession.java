package com.duynguyen.network;

public interface ISession {

    boolean isConnected();

    void setHandler(IMessageHandler messageHandler);

    void setService(Service service);

    void sendMessage(Message message);

    void close();

}
