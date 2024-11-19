package com.duynguyen.server;
import com.duynguyen.database.jdbc.DbManager;
import com.duynguyen.utils.Utils;
import com.duynguyen.utils.Log;

public class MainEntry {
    public static boolean isStop = false;
    public static void main(String[] args) {
        if(Config.getInstance().load()){
            if (!DbManager.getInstance().start()) {
                return;
            }
            if (Utils.availablePort(Config.getInstance().getPort())) {
                new MainEntry();
                if (!Server.init()) {
                    Log.error("Khoi tao that bai!");
                    return;
                }
                new Thread(Server::start).start();
            } else {
                Log.error("Port " + Config.getInstance().getPort() + " da duoc su dung!");
            }
        } else {
            Log.error("Khoi tao that bai!");
        }
    }
}
