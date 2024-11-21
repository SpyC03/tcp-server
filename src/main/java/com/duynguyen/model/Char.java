package com.duynguyen.model;

import com.duynguyen.constants.SQLStatement;
import com.duynguyen.database.jdbc.DbManager;
import com.duynguyen.network.Service;
import com.duynguyen.server.ServerManager;
import com.duynguyen.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import com.duynguyen.utils.Log;
import com.duynguyen.server.Server;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

public class Char {
    public int id;
    public User user;
    public String name;
    public long coin;
    @Getter
    @Setter
    public Service service;
    public boolean isCleaned;
    public boolean isOnline;
    public int energy, maxEnergy;
    public int level;
    public long exp, maxExp;
    public int potentialPoints;
    public Item[] bag;
    public byte numberCellBag;


    public Char(int id) {
        this.id = id;
    }

    public void loadDisplay(ResultSet rs) {
        try {
            this.name = rs.getString("name");
            this.exp = rs.getLong("exp");
            if (this.exp > Server.EXP_MAX) {
                this.exp = Server.EXP_MAX;
            }
            this.level = Utils.getLevel(this.exp);
        } catch (SQLException ex) {
            Log.error("load display err: " + ex.getMessage(), ex);
        }
    }

    //check hack, mod game and lock user
    public void updateEveryFiveSecond() {
        if (this.coin < 0) {
            service.serverDialog("Dữ liệu game không hợp lệ. Bạn đã bị khóa tài khoản.");
            user.lock();
        }
    }

    public void updateEveryFiveMinutes() {
        //update energy
        if (energy < maxEnergy) {
            energy++;
            service.updateEnergy();
        }
    }

    public void removeItem(int index) {
        try {
            getService().removeItem(index);
        } catch (Exception ex) {
            Log.error("remove item: " + ex.getMessage(), ex);
        }
    }

    public void addItem(Item item) {
        try {
            if (item == null) {
                return;
            }
            getService().addItem(item);
        } catch (Exception ex) {
            Log.error("add item: " + ex.getMessage(), ex);
        }
    }

    public synchronized boolean load() {
        try (PreparedStatement stmt = DbManager.getInstance().getConnection(DbManager.LOAD_CHAR).prepareStatement(
                SQLStatement.LOAD_PLAYER, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    loadDisplay(rs);
                    this.coin = rs.getLong("coin");
                    this.maxEnergy = rs.getInt("maxEnergy");
                    this.energy = rs.getInt("energy");
                    this.potentialPoints = rs.getInt("potentialPoints");
                    this.numberCellBag = rs.getByte("numberCellBag");
                    this.exp = rs.getLong("exp");
                    this.bag = new Item[this.numberCellBag];
                    JSONArray array = (JSONArray) JSONValue.parse(rs.getString("bag"));
                    if (array != null) {
                        int size = array.size();
                        for (int i = 0; i < size; i++) {
                            Item item = new Item((String) array.get(i));
                            this.bag[i] = item;
                        }
                    }
                    return true;
                }
            }
        } catch (SQLException ex) {
            Log.error("load char data : " + ex.getMessage(), ex);

        }
        return false;
    }

    public synchronized void addCoin(long coin) {
        if (coin == 0) {
            return;
        }
        long pre = this.coin;


        this.coin += coin;
        if (this.coin < 0) {
            this.coin = 0;
        }

        getService().addXu((int) (this.coin - pre));
        updateWithBalanceMessage();
    }

    public void updateWithBalanceMessage() {
        getService().loadCoin();
        serverMessage("Bạn có " + Utils.getCurrency(coin) + " xu trong tài khoản.");
    }

    public void serverMessage(String text) {
        getService().serverMessage(text);
    }


    public synchronized void addExp(long exp) {
        this.exp += exp;
        if (this.exp > Server.EXP_MAX) {
            this.exp = Server.EXP_MAX;
            return;
        }
        getService().addExp(exp);
        int preLevel = this.level;
        this.level = Utils.getLevel(this.exp);
        int nextLevel = this.level;
        int num = nextLevel - preLevel;
        if (num > 0) {
            for (int i = 0; i < num; i++) {
                potentialPoints += 5;
            }
            getService().levelUp();
        }
    }

}
