package com.duynguyen.network;


import com.duynguyen.constants.CMD;
import com.duynguyen.model.User;
import com.duynguyen.utils.Log;
import lombok.Setter;

import java.io.DataInputStream;

public class Controller implements IMessageHandler {

    private final Session client;
    @Setter
    private Service service;
    @Setter
    private User user;

    public Controller(Session client) {
        this.client = client;
    }


    @Override
    public void onMessage(Message mss) {
        if (mss != null) {
            try {
                int command = mss.getCommand();
                if (command != CMD.NOT_LOGIN && command != CMD.NOT_MAP && command != CMD.SUB_COMMAND) {
                    if (user == null || user.isCleaned) {
                        return;
                    }
                }
                switch (command) {
                    case CMD.NEW_MESSAGE:
                        newMessage(mss);
                        break;

                    case CMD.NOT_LOGIN:
                        messageNotLogin(mss);
                        break;

                    case CMD.NOT_MAP:
                        messageNotMap(mss);
                        break;
                    
                    case CMD.SUB_COMMAND:
                        messageSubCommand(mss);
                        break;

                    default:
                        Log.info("CMD: " + mss.getCommand());
                        break;
                }
            } catch (Exception e) {
                Log.error("onMessage: " + e.getMessage());
            }
        } else{
            Log.info("message is null");
        }
    }

    @Override
    public void newMessage(Message mss) {
        if (mss != null) {
            try(DataInputStream dis = mss.reader()) {
                if (user == null) {
                    return;
                }
                byte command = dis.readByte();
                switch (command) {
                    default:
                        Log.debug(String.format("Client %d: newMessage: %d", client.id, command));
                        break;
                }
            } catch (Exception e) {
                Log.error("newMessage: " + e.getMessage());
            }
        }
    }

    @Override
    public void messageNotLogin(Message mss) {
        if (mss != null) {
            try(DataInputStream dis = mss.reader()) {
                if (user != null) {
                    return;
                }
                byte command = dis.readByte();
                switch (command) {
                    case CMD.LOGIN:
                        client.login(mss);
                        break;

                    case CMD.CLIENT_INFO:
                        client.setClientType(mss);
                        break;

                    case CMD.REGISTER:
                        client.register(mss);
                        break;

                    default:
                        Log.debug(String.format("Client %d: messageNotLogin: %d", client.id, command));
                        break;
                }
            } catch (Exception e) {
                Log.error("messageNotLogin: " + e.getMessage());
            }
        }
    }

    @Override
    public void onConnectionFail() {
        Log.info(String.format("Client %d: Kết nối thất bại!", client.id));
    }

    @Override
    public void onDisconnected() {
        Log.info(String.format("Client %d: Mất kết nối!", client.id));
    }

    @Override
    public void onConnectOK() {
        Log.info(String.format("Client %d: Kết nối thành công!", client.id));
    }

    @Override
    public void messageNotMap(Message ms) {
        
    }

    @Override
    public void messageSubCommand(Message ms) {
        
    }

}
