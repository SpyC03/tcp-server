package com.duynguyen.network;


import com.duynguyen.constants.CMD;
import com.duynguyen.model.User;
import com.duynguyen.utils.Log;

public class Controller implements IMessageHandler {

    private final Session client;
    private Service service;
    private User user;

    public Controller(Session client) {
        this.client = client;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public void setUser(User us) {
        this.user = us;
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
                e.printStackTrace();
            }
        } else{
            Log.info("message is null");
        }
    }

    @Override
    public void newMessage(Message mss) {
        if (mss != null) {
            try {
                if (user == null) {
                    return;
                }
                byte command = mss.reader().readByte();
                switch (command) {
                    default:
                        Log.debug(String.format("Client %d: newMessage: %d", client.id, command));
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void messageNotLogin(Message mss) {
        if (mss != null) {
            try {
                if (user != null) {
                    return;
                }
                byte command = mss.reader().readByte();
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
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onConnectionFail() {
        Log.debug(String.format("Client %d: Kết nối thất bại!", client.id));
    }

    @Override
    public void onDisconnected() {
        Log.debug(String.format("Client %d: Mất kết nối!", client.id));
    }

    @Override
    public void onConnectOK() {
        Log.debug(String.format("Client %d: Kết nối thành công!", client.id));
    }

    @Override
    public void messageNotMap(Message ms) {
        
    }

    @Override
    public void messageSubCommand(Message ms) {
        
    }

}
