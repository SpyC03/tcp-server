package com.duynguyen.network;

import com.duynguyen.constants.CMD;
import com.duynguyen.utils.Log;

import java.io.DataOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Service extends AbsService{
    private final Session session;

    public Service(Session session) {
        this.session = session;
    }

    @Override
    public void sendMessage(Message ms) {
        if (this.session != null) {
            this.session.sendMessage(ms);
        }
    }

    @Override
    public void chat(String name, String text) {
        
    }

    public void loginSuccess() {
        try {
            Message ms = messageNotLogin(-127);
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Log.info("Response login success error" + ex);
        }
    }

    public void register(){
        try {
            Message ms = messageNotLogin(-126);
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Log.info("Response register error" + ex);
        }
    }

    public void serverDialog(){}
    public void serverMessage(String text) {
        try {
            Message ms = new Message(CMD.SERVER_MESSAGE);
            DataOutputStream ds = ms.writer();
            ds.writeUTF(text);
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Log.error("Send server message error: " + ex.getMessage());
        }
    }

    public void showWait(String title) {
        try {
            Message ms = messageSubCommand(CMD.SHOW_WAIT);
            DataOutputStream ds = ms.writer();
            ds.writeUTF(title);
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
    }



}