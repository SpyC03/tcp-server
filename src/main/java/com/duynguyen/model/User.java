package com.duynguyen.model;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import com.duynguyen.constants.SQLStatement;
import com.duynguyen.database.jdbc.DbManager;
import com.duynguyen.network.Controller;
import com.duynguyen.network.Message;
import com.duynguyen.network.Service;
import com.duynguyen.network.Session;
import com.duynguyen.server.MainEntry;
import com.duynguyen.server.ServerManager;
import com.duynguyen.utils.Log;
import com.duynguyen.utils.Utils;

import lombok.Getter;

public class User {
    public Session session;
    public Char character;
    @Getter
    public Service service;
    public int id;
    public String username;
    public String password;
    public String random;
    public int role;
    public byte status;
    public byte activated;
    public Timestamp banUntil;
    public long lastAttendance;
    public boolean isLoadFinish;
    public boolean isCleaned;
    public ArrayList<String> IPAddress;
    private boolean saving;


    public User(Session client, String username, String password, String random) {
        this.session = client;
        this.service = client.getService();
        this.username = username;
        this.password = password;
        this.random = random;
    }

    public void cleanUp() {
        this.isCleaned = true;
        this.session = null;
        this.service = null;
        Log.debug("clean user " + this.username);
    }

    public void login() {
        try {
            if (username.equals("-1") && password.equals("asfaf")) {
                Log.info("Login admin");
                return;
            }
            Pattern p = Pattern.compile("^[a-zA-Z0-9]+$|^[a-zA-Z0-9._%+-]+@gmail\\.com$");
            Matcher m1 = p.matcher(username);
            if (!m1.find()) {
                service.serverMessage("Tên tài khoản có kí tự lạ.");
                return;
            }
            HashMap<String, Object> map = getUserMap();
            if (map == null) {
                service.serverMessage("Tài khoản hoặc mật khẩu không chính xác.");
                return;
            }

            this.id = (int) (map.get("id"));
            this.role = (int) map.get("role");
            this.lastAttendance = (long) map.get("last_attendance_at");
            this.status = (byte) ((int) map.get("status"));
            this.activated = (byte) ((int) map.get("activated"));
            Object obj = map.get("ban_until");
            if (obj != null) {
                this.banUntil = (Timestamp) obj;
                long now = System.currentTimeMillis();
                long timeRemaining = banUntil.getTime() - now;
                if (timeRemaining > 0) {
                    service.serverMessage(
                            String.format("Tài khoản bị khóa trong %s. Vui lòng liên hệ admin để biết thêm chi tiết.",
                                    10));
                    return;
                }
            }


            if (this.status == 0) {
                service.serverMessage("Liên hệ AD để kích hoạt acc!");
                return;
            }

            this.IPAddress = new ArrayList<>();
            obj = map.get("ip_address");
            if (obj != null) {
                String str = obj.toString();
                if (!str.isEmpty()) {
                    JSONArray jArr = (JSONArray) JSONValue.parse(str);
                    for (Object o : jArr) {
                        IPAddress.add(o.toString());
                    }
                }
            }
            if (!IPAddress.contains(session.IPAddress)) {
                IPAddress.add(session.IPAddress);
            }

            synchronized (ServerManager.users) {
                User u = ServerManager.findUserByUsername(this.username);
                if (u != null && !u.isCleaned) {
                    service.serverMessage("Tài khoản đã có người đăng nhập.");
                    if (u.session != null && u.session.getService() != null) {
                        u.service.serverMessage("Có người đăng nhập vào tài khoản của bạn.");
                    }
                    Utils.setTimeout(() -> {
                        try {
                            if (!u.isCleaned) {
                                u.session.closeMessage();
                            }
                        } catch (Exception e) {
                            Log.error("close old session err", e);
                        } finally {
                            ServerManager.removeUser(u);
                        }
                    }, 1000);

                    Utils.setTimeout(() -> {
                        try {
                            if (!this.isCleaned) {
                                this.session.closeMessage();
                            }
                        } catch (Exception e) {
                            Log.error("close current session err", e);
                        } finally {
                            ServerManager.removeUser(this);
                        }
                    }, 1000);
                    return;
                }
                ServerManager.addUser(this);
            }
            this.isLoadFinish = true;
        } catch (Exception ex) {
            Log.error("login err", ex);
        }
    }

    public void register() {
        try {
            //check if username already exists
            HashMap<String, Object> map = getUserMap();
            if (map != null) {
                service.serverMessage("Tài khoản đã tồn tại.");
                return;
            }

            //check username is valid
            Pattern p = Pattern.compile("^[a-zA-Z0-9]+$|^[a-zA-Z0-9._%+-]+@gmail\\.com$");
            Matcher m1 = p.matcher(username);
            if (!m1.find()) {
                service.serverMessage("Tên tài khoản không hợp lệ.");
                return;
            }
            try (Connection conn = DbManager.getInstance().getConnection(DbManager.SAVE_DATA);
                    PreparedStatement stmt = conn
                    .prepareStatement(SQLStatement.REGISTER)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.setInt(3, 1);
                stmt.setInt(4, 1);
                stmt.setInt(5, 0);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    service.registerSuccess();
                    Log.info("Đăng ký thành công");
                } else {
                    Log.info("Đăng ký thất bại");
                }
            }
        } catch (Exception e) {
            Log.error("register err", e);
        }
    }

    public void createCharacter(Message ms) {
        try (DataInputStream dis = ms.reader()) {
            String name = dis.readUTF();
            Pattern p = Pattern.compile("^[a-z0-9]+$");
            Matcher m1 = p.matcher(name);

            if (!m1.find()) {
                service.serverDialog("Tên nhân vật không được chứa ký tự đặc biệt!");
                return;
            }
            if (name.length() < 6 || name.length() > 15) {
                service.serverDialog("Tên tài khoản chỉ cho phép từ 6 đến 15 ký tự!");
                return;
            }

            try (Connection conn = DbManager.getInstance().getConnection(DbManager.SAVE_DATA)) {
                try (PreparedStatement checkStmt = conn.prepareStatement(
                        SQLStatement.CHECK_PLAYER_EXIST)) {
                    checkStmt.setString(1, name);
                    try (ResultSet check = checkStmt.executeQuery()) {
                        if (check.next() && check.getInt(1) > 0) {
                            service.serverDialog("Tên nhân vật đã tồn tại!");
                            return;
                        }
                    }
                }
                try (PreparedStatement insertStmt = conn.prepareStatement(
                        SQLStatement.CREATE_PLAYER)) {
                    insertStmt.setInt(1, this.id);
                    insertStmt.setString(2, name);
                    insertStmt.setLong(3, 1000);
                    insertStmt.setString(4, "[]");
                    insertStmt.setLong(5, System.currentTimeMillis());
                    insertStmt.executeUpdate();
                }

                try(PreparedStatement stmt = conn.prepareStatement(SQLStatement.CREATE_WAVE_DATA)){
                    stmt.setInt(1, this.id);
                    stmt.setInt(2, 0);
                    stmt.setInt(3, 0);
                    stmt.setString(4, "[]");
                    stmt.executeUpdate();
                }
                Log.info("Tạo nhân vật thành công");
            } catch (SQLException e) {
                Log.error("create char SQL error", e);
                service.serverDialog("Tạo nhân vật thất bại do lỗi hệ thống!");
            }

        } catch (IOException e) {
            Log.error("create char IO error", e);
            service.serverDialog("Tạo nhân vật thất bại!");
        }
    }


    public synchronized boolean load() {
        try (Connection conn = DbManager.getInstance().getConnection(DbManager.LOAD_CHAR);

        ) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    SQLStatement.LOAD_PLAYER, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        character = new Char(id);
                        character.loadDisplay(rs);
                        character.coin = rs.getLong("coin");
                        character.maxEnergy = rs.getInt("max_energy");
                        character.energy = rs.getInt("energy");
                        character.potentialPoints = rs.getInt("point");
                        character.numberCellBag = rs.getByte("number_cell_bag");
                        character.exp = rs.getLong("exp");
                        character.lastUpdateEnergy = rs.getLong("last_update_energy");
                        character.energy = character.getCurrentEnergy();
                        Log.info("lastUpdateEnergy: " + character.lastUpdateEnergy + ", energy: " + character.energy);

                        JSONArray array = (JSONArray) JSONValue.parse(rs.getString("bag"));
                        if (array != null) {
                            character.bag = new Item[array.size()];
                            int size = array.size();
                            for (int i = 0; i < size; i++) {
                                Item item = new Item((String) array.get(i));
                                character.bag[i] = item;
                            }
                        }
                    }
                }
                return loadWaveData();
            }
        } catch (SQLException ex) {
            Log.error("load char data : " + ex.getMessage(), ex);

        }
        return false;
    }

    public boolean loadWaveData() {
        try (Connection conn = DbManager.getInstance().getConnection(DbManager.LOAD_CHAR);
             PreparedStatement smt = conn.prepareStatement(SQLStatement.LOAD_WAVE_DATA)) {
            smt.setInt(1, id);
            ResultSet rs = smt.executeQuery();
            if(rs.next()) {
                Wave wave = new Wave(id);
                wave.exp = rs.getInt("exp");
                wave.wave = rs.getInt("wave");
                JSONArray array = (JSONArray) JSONValue.parse(rs.getString("inventory"));
                if (array != null) {
                    wave.inventory = new Item[array.size()];
                    int size = array.size();
                    for (int i = 0; i < size; i++) {
                        Item item = new Item((String) array.get(i));
                        wave.inventory[i] = item;
                    }
                }
                if(character != null) {
                    character.waveState = wave;
                }

            }
            return true;
        } catch (Exception e) {
            Log.error("load wave data err: " + e.getMessage(), e);
        }
        return false;
    }


    public synchronized void loadPlayerData() {
        try {
            if (MainEntry.isStop) {
                service.serverDialog("Hệ thống Máy chủ bảo trì vui lòng thoát game để tránh mất dữ liệu.");
                Thread.sleep(1000);
            }
            if (!load()) {
                session.disconnect();
            }
            if (character != null) {
                character.user = this;
                Controller controller = (Controller) session.getMessageHandler();
                controller.set_char(character);
                character.setService(this.service);
                service.setPlayer(this.character);
                session.setName(character.name);
                ServerManager.addChar(character);
            }
        } catch (InterruptedException e) {
            Log.error("select char err", e);
        }
    }


    @SuppressWarnings("unchecked")
    public synchronized void saveData() {
        try {
            Log.info("saving data user: " + username);
            if (isLoadFinish && !saving) {
                saving = true;
                try {
                    JSONArray list = new JSONArray();
                    list.addAll(IPAddress);
                    String jList = list.toJSONString();

                    try (Connection conn = DbManager.getInstance().getConnection(DbManager.SAVE_DATA)) {
                        try (PreparedStatement stmt = conn.prepareStatement(
                                SQLStatement.SAVE_DATA)) {
                            stmt.setInt(1, 0);
                            stmt.setLong(2, this.lastAttendance);
                            stmt.setString(3, jList);
                            stmt.setInt(4, this.id);
                            stmt.executeUpdate();
                        }

                        try (PreparedStatement stmt = conn.prepareStatement(
                                SQLStatement.SAVE_DATA_PLAYER)) {
                            if(character.energy < 0) {
                                character.energy = 0;
                            }
                            if(character.energy > character.maxEnergy) {
                                character.energy = character.maxEnergy;
                            }
                            stmt.setInt(1, this.character.energy);
                            stmt.setInt(2, this.character.maxEnergy);
                            stmt.setLong(3, this.character.exp);
                            stmt.setLong(4, this.character.coin);
                            stmt.setInt(5, this.character.potentialPoints);
                            stmt.setByte(6, this.character.numberCellBag);
                            JSONArray array = new JSONArray();
                            for (Item item : this.character.bag) {
                                if (item != null) {
                                    array.add(item.itemId());
                                }
                            }
                            stmt.setString(7, array.toJSONString());
                            stmt.setInt(8, 0); //online
                            stmt.setLong(9, this.character.lastUpdateEnergy);
                            stmt.setInt(10, this.character.id);
                            stmt.executeUpdate();
                        }

                        try (PreparedStatement stmt = conn.prepareStatement(
                                SQLStatement.SAVE_WAVE_DATA)) {
                            stmt.setInt(1, this.character.waveState.exp);
                            stmt.setInt(2, this.character.waveState.wave);
                            JSONArray array = new JSONArray();
                            for (Item item : this.character.waveState.inventory) {
                                if (item != null) {
                                    array.add(item.itemId());
                                }
                            }
                            stmt.setString(3, array.toJSONString());
                            stmt.setInt(4, this.character.id);
                            stmt.executeUpdate();
                        }

                    }
                } finally {
                    saving = false;
                }
            }
        } catch (Exception e) {
            Log.error("save data user: " + username);
        }
    }

    public HashMap<String, Object> getUserMap() {
        try (
                Connection conn = DbManager.getInstance().getConnection(DbManager.LOGIN);
                PreparedStatement stmt = conn.prepareStatement(SQLStatement.GET_USER, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)
        ) {
            stmt.setString(1, this.username);

            try (ResultSet data = stmt.executeQuery()) {
                ArrayList<HashMap<String, Object>> list = DbManager.getInstance().convertResultSetToList(data);
                if (list.isEmpty()) {
                    return null;
                }
                HashMap<String, Object> map = list.get(0);
                if (map != null) {
                    String passwordHash = (String) map.get("password");
                    if (!passwordHash.equals(password)) {
                        return null;
                    }
                }
                return map;
            }
        } catch (SQLException e) {
            Log.error("getUserMap() err", e);
        }
        return null;
    }


    public void lock() {
        try (Connection conn = DbManager.getInstance().getConnection(DbManager.SAVE_DATA);
             PreparedStatement stmt = conn
                     .prepareStatement(SQLStatement.LOCK_ACCOUNT)) {
            stmt.setInt(2, this.id);
            stmt.executeUpdate();
            session.disconnect();
        } catch (Exception e) {
            Log.error("lock user: " + username);
        }
    }
}