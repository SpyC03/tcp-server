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
                Log.info("CMD: " + command);
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
                        messageInGame(mss);
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
                byte command = dis.readByte();
                Log.info("Sub command: " + command);
                switch (command) {
                    case CMD.LOGIN:
                        client.login(mss);
                        break;
                    case CMD.CLIENT_OK:
                        client.clientOk();
                        break;

                    case CMD.CLIENT_INFO:
                        client.setClientType(mss);
                        break;



                    case CMD.REGISTER:
                        client.register(mss);
                        break;
                    case CMD.GET_SESSION_ID:
                        Log.info("Client " + client.id + ": GET_SESSION_ID");
                        break;
                    default:
                        Log.info(String.format("Client %d: messageNotLogin: %d", client.id, command));
                        break;
                }
            } catch (Exception e) {
                Log.error("messageNotLogin: " + e.getMessage());
            }
        }
    }

    @Override
    public void newMessage(Message ms) {
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
    public void messageInGame(Message ms) {
        if(ms!=null){
            try (DataInputStream dis = ms.reader()){
                if(user.isCleaned){
                    return;
                }
                byte command = dis.readByte();
                switch(command) {
                    case CMD.CREATE_PLAYER:
                        Log.info("Client " + client.id + ": CREATE_PLAYER");
                        user.createCharacter(ms);
                        break;
                    default:
                        Log.info(String.format("Client %d: messageInGame: %d", client.id, command));
                        break;
                }
            } catch (IOException ex){
                Log.error("messageSubCommand: " + ex.getMessage());
            }
        }
        
    }

}
