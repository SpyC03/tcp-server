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

    public static boolean init(){
        return true;
    }
    public static void start(){
        try {     
            SocketIO.init();
            int port = Config.getInstance().getPort();
            Log.info("Start socket port=" + port);
            server = new ServerSocket(port);
            boolean start = true;
            int id = 0;
            Log.info("Start server Success!");
            while (start) {
                try {
                    Socket client = server.accept();
                    String ip = client.getInetAddress().getHostAddress();
                    int number = ServerManager.frequency(ip);
                    if (number >= Config.getInstance().getIpAddressLimit()) {
                        client.close();
                        continue;
                    }
                    Session cl = new Session(client, ++id);
                    cl.IPAddress = ip;
                    ServerManager.add(ip);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            if (ServerManager.getUsers().isEmpty()) {

            }
            Log.info("End socket");
        } catch (IOException e) {
            Log.error("Can not close server", e);
        }
    }

    
}
