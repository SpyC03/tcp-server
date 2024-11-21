package com.duynguyen.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import com.duynguyen.model.User;

import com.duynguyen.network.Session;
import com.duynguyen.socket.SocketIO;
import com.duynguyen.utils.Log;

public class Server {
    private static ServerSocket server;
    public static boolean start;
    public static int[] exps = new int[]{0,200,600,1200,2500,5000,9000,18000,20000,24000,36000,54000,64800};
    public static final long EXP_MAX = 64800;

    public static boolean init(){
        start = false;
        return true;
    }
    public static void start(){
        try {
            SocketIO.init();
            int port = Config.getInstance().getPort();
            Log.info("Start socket port=" + port);
            server = new ServerSocket(port);
            start = true;
            int id = 0;
            Log.info("Start server Success!");
            while (start) {
                try {
                    Socket client = server.accept();
                    String ip = client.getInetAddress().getHostAddress();
                    int number = ServerManager.frequency(ip);
                    Log.info("IP: " + ip + " number: " + number);
                    if (number >= Config.getInstance().getIpAddressLimit()) {
                        client.close();
                        continue;
                    }
                    Session cl = new Session(client, ++id);
                    cl.IPAddress = ip;
                    ServerManager.add(ip);
                } catch (Exception e) {
                    Log.error("Can not accept client", e);
                }
            }
        } catch (IOException e) {
            Log.error("Can not start server", e);
        }
    }

    public static void close() {
        try {
            List<User> users = ServerManager.getUsers();
            for (User user : users) {
                if (!user.isCleaned) {
                    user.session.closeMessage();
                }
            }
            server.close();
            server = null;
            Log.info("End socket");
        } catch (IOException e) {
            Log.error("Can not close server", e);
        }
    }

    
}
