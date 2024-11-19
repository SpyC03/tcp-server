package com.duynguyen.network;

import java.io.DataOutputStream;

import com.duynguyen.constants.CMD;
import com.duynguyen.utils.Log;

public class Service extends AbsService{
    private Session session;

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
            Log.info("Response login success error" + ex.toString());
        }
    }

    public void register(){
        try {
            Message ms = messageNotLogin(-126);
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Log.info("Response register error" + ex.toString());
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

}