package com.duynguyen.network;

import com.duynguyen.constants.CMD;
import com.duynguyen.model.User;
import com.duynguyen.server.MainEntry;
import com.duynguyen.server.ServerManager;
import com.duynguyen.utils.Log;
import com.duynguyen.utils.Utils;
import lombok.Getter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Session implements ISession {

    private byte[] key;
    public Socket sc;
    public DataInputStream dis;
    public DataOutputStream dos;
    public int id;
    public User user;
    private IMessageHandler controller;
    @Getter
    private Service service;
    public boolean connected;
    public boolean isLoginSuccess;
    private byte curR, curW;
    private final Sender sender;
    private Thread collectorThread;
    protected Thread sendThread;
    protected String plastfrom;
    public String version;
    protected byte clientType;
    public boolean clientOK;
    public byte zoomLevel;
    protected boolean isGPS;
    protected int width;
    protected int height;
    protected boolean isQwert;
    protected boolean isTouch;
    protected int provider;
    protected String agent;
    public String IPAddress;
    public boolean sendKeyComplete;
    public boolean isLogin;
    public boolean isSetClientType;
    public static HashMap<String, Lock> lockLogins = new HashMap<>();
    public boolean isClosed;

    public Session(Socket sc, int id) throws IOException {
        Log.info("New session: " + id);
        this.sc = sc;
        this.id = id;
        this.sc.setKeepAlive(true);
        // this.sc.setTcpNoDelay(true);
        // this.sc.setSoTimeout(300000);//ko hoat dong -> close
        connected = true;

        //khoi tao io stream
        this.dis = new DataInputStream(sc.getInputStream());
        this.dos = new DataOutputStream(sc.getOutputStream());

        //khoi tao handler
        setHandler(new Controller(this));
        setService(new Service(this));

        //thiet lap thread xu ly gui/nhan message
        sendThread = new Thread(sender = new Sender());
        String remoteSocketAddress = sc.getRemoteSocketAddress().toString();
        sendThread.setName("sender: " + remoteSocketAddress);
        collectorThread = new Thread(new MessageCollector());
        collectorThread.setName("reader: " + remoteSocketAddress);
        collectorThread.start();
    }

    public void setClientType(Message mss) throws IOException {
        try(DataInputStream di = mss.reader()) {
            if (!isSetClientType) {
                this.clientType = di.readByte();
                this.zoomLevel = di.readByte();
                if (this.zoomLevel < 1 || this.zoomLevel > 4) {
                    this.zoomLevel = 1;
                }
                this.isGPS = di.readBoolean();
                this.width = di.readInt();
                this.height = di.readInt();
                this.isQwert = di.readBoolean();
                this.isTouch = di.readBoolean();
                this.plastfrom = di.readUTF();
                di.readInt();
                di.readByte();
                this.provider = di.readInt();
                this.agent = di.readUTF();
                this.isSetClientType = true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            mss.cleanup();
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void setHandler(IMessageHandler messageHandler) {
        this.controller = messageHandler;
    }

    public IMessageHandler getMessageHandler() {
        return this.controller;
    }

    @Override
    public void setService(Service service) {
        this.service = service;
    }

    @Override
    public void sendMessage(Message message) {
        if (connected) {
            sender.addMessage(message);
        }
    }

    /*
     * Trường hợp value == CMD.FULL_SIZE:
    Ghi m.getCommand() (mã lệnh gốc).
    Ghi 4 byte biểu diễn num (32 bit):
        byte2 = writeKey((byte) (num >> 24)): byte cao nhất (bits từ 24–31).
        byte3 = writeKey((byte) (num >> 16)): byte thứ hai (bits từ 16–23).
        byte4 = writeKey((byte) (num >> 8)): byte thứ ba (bits từ 8–15).
        byte5 = writeKey((byte) (num & 255)): byte thấp nhất (bits từ 0–7).

    * Trường hợp value != CMD.FULL_SIZE và sendKeyComplete = true:
    Ghi 2 byte biểu diễn num (16 bit):
        byte6 = writeKey((byte) (num >> 8)): byte cao (bits từ 8–15).
        byte7 = writeKey((byte) (num & 255)): byte thấp (bits từ 0–7).

    * Trường hợp value != CMD.FULL_SIZE và sendKeyComplete = false:
    Ghi trực tiếp 2 byte không mã hóa:
        dos.writeByte(num & 0xFF00): byte cao.
        dos.writeByte(num & 0xFF): byte thấp.
     * 
    */
    private void doSendMessage(Message m) {
        try {
            byte[] data = m.getData();
            byte value = m.getCommand();
            int num = data.length;
            value = num > Short.MAX_VALUE ? CMD.FULL_SIZE : value;
            byte b = value;
            if (sendKeyComplete) {
                b = writeKey(value);
            }
            dos.writeByte(b);
            if (value == CMD.FULL_SIZE) {
                if (sendKeyComplete) {
                    dos.writeByte(writeKey(m.getCommand()));
                } else {
                    dos.writeByte(m.getCommand());
                }
                int byte2 = writeKey((byte) (num >> 24));
                dos.writeByte(byte2);
                int byte3 = writeKey((byte) (num >> 16));
                dos.writeByte(byte3);
                int byte4 = writeKey((byte) (num >> 8));
                dos.writeByte(byte4);
                int byte5 = writeKey((byte) (num & 255));
                dos.writeByte(byte5);
            } else if (sendKeyComplete) {
                int byte6 = writeKey((byte) (num >> 8));
                dos.writeByte(byte6);
                int byte7 = writeKey((byte) (num & 255));
                dos.writeByte(byte7);
            } else {
                dos.writeByte(num & 0xFF00);
                dos.writeByte(num & 0xFF);
            }
            if (sendKeyComplete) {
                for (int i = 0; i < num; i++) {
                    data[i] = writeKey(data[i]);
                }
            }
            dos.write(data);
            dos.flush();
        } catch (Exception e) {
            Log.error("doSendMessage err", e);
        }
    }

    public byte readKey(byte b) {
        byte b2 = this.curR;
        this.curR = (byte) (b2 + 1);
        byte result = (byte) ((key[b2] & 255) ^ (b & 255));
        if (this.curR >= key.length) {
            this.curR %= (byte) key.length;
        }
        return result;
    }

    public byte writeKey(byte b) {
        byte b2 = this.curW;
        this.curW = (byte) (b2 + 1);
        byte result = (byte) ((key[b2] & 255) ^ (b & 255));
        if (this.curW >= key.length) {
            this.curW %= (byte) key.length;
        }
        return result;
    }

    public void generateKey() {
        this.key = ("game_" + (Utils.nextInt(10000))).getBytes();
        // this.key = "game_1234".getBytes();
    }

    @Override
    public void close() {
        cleanNetwork();
    }

    private void cleanNetwork() {
        try {
            if (user != null) {
                if (!user.isCleaned) {
                    user.cleanUp();
                }
            }
            curR = 0;
            curW = 0;
            connected = false;
            isLoginSuccess = false;
            if (sc != null) {
                sc.close();
                sc = null;
            }
            if (dos != null) {
                dos.close();
                dos = null;
            }
            if (dis != null) {
                dis.close();
                dis = null;
            }
            if (sendThread != null) {
                sendThread.interrupt();
                sendThread = null;
            }
            if (collectorThread != null) {
                collectorThread.interrupt();
                collectorThread = null;
            }
            controller = null;
            service = null;
            // System.gc();
        } catch (IOException e) {
            Log.error("cleanNetwork err", e);
        }
    }

    @Override
    public String toString() {
        if (this.user != null) {
            return this.user.toString();
        }
        return "Client " + this.id;
    }

    public void sendKey() {
        try {
            if (!sendKeyComplete) {
                generateKey();
                Message ms = new Message(CMD.GET_SESSION_ID);
                try (DataOutputStream ds = ms.writer()) {
                    ds.writeByte(key.length);
                    ds.writeByte(key[0]);
                    for (int i = 1; i < key.length; i++) {
                        ds.writeByte(key[i] ^ key[i - 1]);
                    }
                    ds.flush();
                }
                doSendMessage(ms);
                sendKeyComplete = true;
                sendThread.start();
            }
        } catch (IOException e) {
            Log.error("sendKey err", e);
        }
    }


    public void login(Message ms) {
        try(DataInputStream di = ms.reader()) {
            String username = di.readUTF().trim();
            String password = di.readUTF().trim();
            String version = di.readUTF().trim();
            di.readUTF();
            di.readUTF();
            String random = di.readUTF().trim();
            byte server = di.readByte();
            Log.info(String.format("Client id: %d - username: %s - version: %s - random: %s - server: %d", id,
                    username, version, random, server));
            this.version = version;
            if (!connected || MainEntry.isStop || !sendKeyComplete) {
                disconnect();
                return;
            }
            if (this.isLoginSuccess) {
                return;
            }
            if (isLogin) {
                return;
            }
            isLogin = true;

            User us = new User(this, username, password, random);
            us.login();
            if (us.isLoadFinish) {
                this.isLoginSuccess = true;
                isLogin = false;
                this.user = us;
                Controller controller = (Controller) getMessageHandler();
                controller.setUser(us);
                controller.setService(service);
                service.loginSuccess();
            } else {
                this.isLoginSuccess = false;
                isLogin = false;
                Log.info("Client " + this.id + ": Đăng nhập thất bại.");
            }
        } catch (IOException ex) {
            Logger.getLogger(Session.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void register(Message ms){
        try (DataInputStream di = ms.reader()) {
            String username = di.readUTF();
            String password = di.readUTF();
            User us = new User(this, username, password, "");
            us.register();
        } catch (IOException e) {
            Log.error("register err", e);
        }
    }



    public void clientOk() {
        if (clientOK) {
            return;
        }
    
        try {
            if (user == null) {
                Log.error("Client " + this.id + ": User not logged in");
                disconnect();
                return;
            }
    
            boolean loadSuccess = user.loadPlayerData();
            
            if (loadSuccess && user.character == null) {
                try {
                    service.createCharacter();
                } catch (Exception e) {
                    Log.error("Error creating character for user: " + user.id, e);
                    loadSuccess = false;
                }
            }
    
            if (loadSuccess) {
                clientOK = true;
                service.playerLoadAll();
                Log.info("Client " + this.id + ": đăng nhập thành công");
            } else {
                Log.error("Client " + this.id + ": Failed to load player data");
                disconnect();
            }
    
        } catch (Exception e) {
            Log.error("Error in clientOk for client: " + this.id, e);
            disconnect();
        }
    }
    

    public void disconnect() {
        try {
            if (sc != null) {
                sc.close();
                connected = false;
            }
        } catch (Exception e) {
            Log.error("disconnect err", e);
        }
    }

    public void closeMessage() {
        try {
            if (isClosed) {
                return;
            }
            isClosed = true;
            try {
                ServerManager.remove(this.IPAddress);
            } catch (Exception e) {
                Log.error("remove ipv4 from list", e);
            } finally {
                ServerManager.removeUser(user);
            }
            try {
                if (user != null) {
                    try {
                        user.saveData();
                    } catch (Exception e) {
                        Log.error("save user: " + user.username + " - err: " + e.getMessage(), e);
                    } finally {
                        ServerManager.removeChar(user.character);
                    }
                }
            } finally {
                if (controller != null) {
                    controller.onDisconnected();
                }
                close();
            }
        } catch (Exception e) {
            Log.error("closeMessage err: " + e.getMessage(), e);
        }
    }

    public void addAttendance() {
        String ip = this.IPAddress;
        int countAttendanceByIp = getCountAttendance();
        ServerManager.countAttendanceByIp.put(ip, countAttendanceByIp + 1);
    }

    public int getCountAttendance() {
        if (!ServerManager.countAttendanceByIp.containsKey(this.IPAddress)) {
            ServerManager.countAttendanceByIp.put(this.IPAddress, 0);
        }
        return ServerManager.countAttendanceByIp.get(this.IPAddress);
    }

    public void setName(String name) {
        if (collectorThread != null) {
            collectorThread.setName(name);
        }
        if (sendThread != null) {
            sendThread.setName(name);
        }
    }

    private void processMessage(Message ms) {
        if (!isClosed) {
            controller.onMessage(ms);
        }
    }

    //quan ly hang doi message can gui, ma hoa message, gui message toi client
    private class Sender implements Runnable {

        private final ArrayList<Message> sendingMessage;

        public Sender() {
            sendingMessage = new ArrayList<>();
        }

        public void addMessage(Message message) {
            sendingMessage.add(message);
        }

        @Override
        public void run() {
            while (connected) {
                if (sendKeyComplete) {
                    while (!sendingMessage.isEmpty()) {
                        try {
                            Message m = sendingMessage.get(0);
                            if (m != null) {
                                doSendMessage(m);
                            }
                            sendingMessage.remove(0);
                        } catch (Exception e) {
                            disconnect();
                            return;
                        }
                    }
                }
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
//                    Log.error("Sender err", e);
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    //lien tuc lang nghe message tu client, giai ma message, chuyen huong toi handler xu ly (da khai bao o tren)
    class MessageCollector implements Runnable {
        @Override
        public void run() {
            Message message;
            try {
                while (connected) {
                    message = readMessage();
                    if (message != null) {
                        try {
                            if (!sendKeyComplete) {
                                sendKey();
                            } else {
                                processMessage(message);
                            }
                        } catch (Exception e) {
                            Log.error("MessageCollector err", e);
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception ex) {
//                Log.error("MessageCollector err", ex);
            }
            closeMessage();
        }

        private Message readMessage() throws Exception {
            try {
                byte cmd = dis.readByte();
                if(sendKeyComplete){
                    cmd = readKey(cmd);
                }
                int size;
                if (sendKeyComplete) {
                    byte b1 = dis.readByte();
                    byte b2 = dis.readByte();
                    size = (readKey(b1) & 255) << 8 | readKey(b2) & 255;
                } else {
                    size = dis.readUnsignedShort();
                }

                byte[] data = new byte[size];
                int len = 0;
                int byteRead = 0;
                while (len != -1 && byteRead < size) {
                    len = dis.read(data, byteRead, size - byteRead);
                    if (len > 0) {
                        byteRead += len;
                    }
                }
                if (sendKeyComplete) {
                    for (int i = 0; i < data.length; i++) {
                        data[i] = readKey(data[i]);
                    }
                }

                return new Message(cmd, data);
            } catch (EOFException e) {
                return null;
            }
        }
    }
}
