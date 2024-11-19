package com.duynguyen.model;

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
import com.duynguyen.network.Service;
import com.duynguyen.network.Session;
import com.duynguyen.server.ServerManager;
import com.duynguyen.utils.Utils;
import com.duynguyen.utils.Log;

public class User {
    public Session session;
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
    public boolean isEntered;
    public boolean isCleaned;
    public boolean isDuplicate;
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
            Pattern p = Pattern.compile("^[a-zA-Z0-9]+$");
            Matcher m1 = p.matcher(username);
            if (!m1.find()) {
                Log.info("Tên tài khoản có kí tự lạ.");
                return;
            }
            HashMap<String, Object> map = getUserMap();
            if (map == null) {
                service.serverMessage("Tài khoản hoặc mật khẩu không chính xác.");
                return;
            }

            this.id = (int) ((long) (map.get("id")));
            this.role = (int) map.get("role");
            this.lastAttendance = (long) map.get("last_attendance_at");
            this.status = (byte) ((int) map.get("status"));
            this.activated = (byte) ((int) map.get("activated"));
            Log.info("id: " + id + "status: " + status + "activated: " + activated);
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
                if (!str.equals("")) {
                    JSONArray jArr = (JSONArray) JSONValue.parse(str);
                    int size = jArr.size();
                    for (int i = 0; i < size; i++) {
                        IPAddress.add(jArr.get(i).toString());
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
                        service.serverMessage("Có người đăng nhập vào tài khoản của bạn.");
                    }
                    Utils.setTimeout(() -> {
                        try {
                            if (!u.isCleaned) {
                                u.session.disconnect();
                            }
                        } catch (Exception e) {
                        } finally {
                            ServerManager.removeUser(u);
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
            PreparedStatement stmt = DbManager.getInstance().getConnection(DbManager.SAVE_DATA)
                    .prepareStatement("INSERT INTO users (username, password) VALUES (?, ?);");
            try {
                stmt.setString(1, username);
                stmt.setString(2, password);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    service.register();
                    Log.info("Đăng ký thành công");
                } else {
                    Log.info("Đăng ký thất bại");
                }
            } finally {
                stmt.close();
            }
        } catch (Exception e) {
            Log.error("register err", e);
        }
    }

    public void saveData() {
        try {
            if (isLoadFinish && !saving) {
                saving = true;
                try {
                    // save data
                } finally {
                    saving = false;
                }
            }
        } catch (Exception e) {
            Log.error("save data user: " + username);
        }
    }

    public HashMap<String, Object> getUserMap() {
        try {
            ArrayList<HashMap<String, Object>> list;
            PreparedStatement stmt = DbManager.getInstance().getConnection(DbManager.LOGIN).prepareStatement(
                    SQLStatement.GET_USER, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt.setString(1, this.username);
            ResultSet data = stmt.executeQuery();
            try {
                list = DbManager.getInstance().convertResultSetToList(data);
            } finally {
                data.close();
                stmt.close();
            }
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
        } catch (SQLException e) {
            Log.error("getUserMap() err", e);
        }
        return null;
    }
}