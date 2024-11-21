package com.duynguyen.network;


import com.duynguyen.constants.CMD;
import com.duynguyen.model.Char;
import com.duynguyen.model.User;
import com.duynguyen.utils.Log;
import lombok.Getter;
import lombok.Setter;

import java.io.DataInputStream;
import java.io.IOException;

public class Controller implements IMessageHandler {

    public final Session client;
    @Setter
    public Service service;
    @Setter
    public User user;
    @Setter
    @Getter
    public Char _char;

    public Controller(Session client) {
        this.client = client;
    }


    @Override
    public void onMessage(Message mss) {
        if (mss != null) {
            try {
                int command = mss.getCommand();
                if (command != CMD.NOT_IN_GAME && command != CMD.IN_GAME) {
                    if (user == null || user.isCleaned) {
                        return;
                    }
                }
                switch (command) {
                    case CMD.NOT_IN_GAME:
                        messageNotInGame(mss);
                        break;
                    
                    case CMD.IN_GAME:
                        messageNotGame(mss);
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
    public void messageNotInGame(Message mss) {
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
    public void messageNotGame(Message ms) {
        if(ms!=null){
            try (DataInputStream dis = ms.reader()){
                if(user.isCleaned){
                    return;
                }
                byte command = dis.readByte();
                switch(command) {
                    case CMD.READY_GET_IN -> user.selectChar();
                    case CMD.CREATE_PLAYER -> user.createCharacter(ms);
                }
            } catch (IOException ex){
                Log.error("messageSubCommand: " + ex.getMessage());
            }
        }
        
    }

}
