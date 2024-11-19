package com.duynguyen.network;

import java.io.DataOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.duynguyen.constants.CMD;


public abstract class AbsService {

    public abstract void sendMessage(Message ms);

    public abstract void chat(String name, String text);

    public Message messageNotLogin(int command) {
        try {
            Message ms = new Message(CMD.NOT_LOGIN);
            ms.writer().writeByte(command);
            return ms;
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public Message newMessage(int command) {
        try {
            Message ms = new Message(CMD.NEW_MESSAGE);
            ms.writer().writeByte(command);
            return ms;
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void serverMessage(String text) {
        try {
            Message ms = new Message(CMD.SERVER_MESSAGE);
            DataOutputStream ds = ms.writer();
            ds.writeUTF(text);
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
