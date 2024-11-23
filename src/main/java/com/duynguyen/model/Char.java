package com.duynguyen.model;

import com.duynguyen.database.jdbc.DbManager;
import com.duynguyen.network.Message;
import com.duynguyen.network.Service;
import com.duynguyen.server.Server;
import com.duynguyen.utils.Log;
import com.duynguyen.utils.Utils;
import lombok.Getter;
import lombok.Setter;

import java.io.DataInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Char {
    public int id;
    public User user;
    public String name;
    public long coin;
    @Getter
    @Setter
    public Service service;
    public boolean isCleaned;
    public int energy, maxEnergy;
    public int level;
    public long exp, maxExp;
    public int potentialPoints;
    public Item[] bag;
    public byte numberCellBag;
    public long lastUpdateEnergy;


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

    private boolean isValidItem(String itemId) {

        return true;
    }

    public void addItem(Message message) {
        try (DataInputStream ds = message.reader()) {
            if(bag.length >= numberCellBag){
                serverMessage("Túi đồ đã đầy.");
                return;
            }
            byte number = ds.readByte();
            if (number <= 0) {
                serverMessage("Số lượng vật phẩm không hợp lệ.");
                return;
            }

            int availableSlots = numberCellBag - bag.length;
            if (number > availableSlots) {
                serverMessage("Không đủ chỗ trong túi đồ.");
                return;
            }

            List<Item> newItems = new ArrayList<>();
            for (int i = 0; i < number; i++) {
                String itemId = ds.readUTF();

                if (!isValidItem(itemId)) {
                    serverMessage("Vật phẩm không hợp lệ: " + itemId);
                    return;
                }
                newItems.add(new Item(itemId));
            }
            bag = mergeBag(bag, newItems.toArray(new Item[0]));
            int res = DbManager.getInstance().updateBag(Utils.bagToString(bag), this.id);

            if (res <= 0) {
                serverMessage("Có lỗi xảy ra khi lưu túi đồ.");
                return;
            }
            serverMessage("bạn nhận được " + number + " vật phẩm.");
        } catch (Exception ex) {
            Log.error("add item: " + ex.getMessage(), ex);
        }
    }

    private Item[] mergeBag(Item[] existingBag, Item[] newItems) {
        int totalLength = existingBag.length + newItems.length;
        Item[] mergedBag = new Item[totalLength];

        System.arraycopy(existingBag, 0, mergedBag, 0, existingBag.length);

        System.arraycopy(newItems, 0, mergedBag, existingBag.length, newItems.length);

        return mergedBag;
    }




    public synchronized void addCoin(Message message) {
        try (DataInputStream ds = message.reader()) {
            long coin = ds.readLong();
            if (coin == 0) {
                return;
            }
            if (this.coin < -coin) {
                serverMessage("Số xu deo đủ.");
                return;
            }

            long pre = this.coin;
            this.coin += coin;
            if (this.coin < 0) {
                this.coin = 0;
            }

            int res = DbManager.getInstance().updateCoin(this.coin, this.id);
            if (res == 0 || res == -1) {
                this.coin = pre;
                serverMessage("Có lỗi xảy ra.");
                return;
            }

            getService().addCoin((int) (this.coin - pre));
            updateWithBalanceMessage();
        } catch (Exception e) {
            Log.error("add coin: " + e.getMessage(), e);
        }
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
