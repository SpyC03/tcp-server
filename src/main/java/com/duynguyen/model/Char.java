package com.duynguyen.model;

import com.duynguyen.constants.SQLStatement;
import com.duynguyen.database.jdbc.DbManager;
import com.duynguyen.network.Message;
import com.duynguyen.network.Service;
import com.duynguyen.server.Server;
import com.duynguyen.utils.Log;
import com.duynguyen.utils.Utils;
import lombok.Getter;
import lombok.Setter;

import java.io.DataInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
    public long lastUpdateEnergy;
    public int level;
    public long exp, maxExp;
    public int potentialPoints;
    public Item[] bag;
    public byte numberCellBag;
    public Wave waveState;


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
            service.serverDialog("Game Data Error! Your account has been locked.");
            user.lock();
        }
    }

    public void updateEveryMinute() {
        energy = getCurrentEnergy();
        if (energy < maxEnergy) {
            service.updateEnergy();
        }
    }

    public void removeItem(Message message) {
        try (DataInputStream dis = message.reader()) {
            byte index = dis.readByte();
            if (index < 0 || index >= bag.length) {
                serverMessage("Invald index.");
                return;
            }


            for (int i = index; i < bag.length - 1; i++) {
                bag[i] = bag[i + 1];
            }

            bag[bag.length - 1] = null;

            String bagData = Utils.bagToString(bag);
            int res = DbManager.getInstance().updateBag(bagData, this.id);

            if (res == 0 || res == -1) {
                serverMessage("An error occurred while saving the bag.");
                return;
            }

            serverMessage("Item removed : " + index);
        } catch (Exception ex) {
            Log.error("remove item: " + ex.getMessage(), ex);
        }
    }


    private boolean isValidItem(String itemId) {
        return true;
    }

    public void addItem(Message message) {
        try (DataInputStream ds = message.reader()) {
            if (bag.length >= numberCellBag) {
                serverMessage("Bag is full.");
                return;
            }
            byte number = ds.readByte();
            if (number <= 0) {
                serverMessage("Invalid number.");
                return;
            }

            int availableSlots = numberCellBag - bag.length;
            if (number > availableSlots) {
                serverMessage("Not enough space in the bag.");
                return;
            }

            List<Item> newItems = new ArrayList<>();
            for (int i = 0; i < number; i++) {
                String itemId = ds.readUTF();

                if (!isValidItem(itemId)) {
                    serverMessage("Invalid item: " + itemId);
                    return;
                }
                newItems.add(new Item(itemId));
            }
            bag = mergeBag(bag, newItems.toArray(new Item[0]));
            int res = DbManager.getInstance().updateBag(Utils.bagToString(bag), this.id);

            if (res <= 0) {
                serverMessage("An error occurred while saving the bag.");
                return;
            }
            serverMessage("You have received " + number + " items.");
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
                serverMessage("Not enough coin.");
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
                serverMessage("An error occurred while saving the coin.");
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
        serverMessage("You have " + Utils.getCurrency(coin) + " coin in bag.");
    }

    public void serverMessage(String text) {
        getService().serverMessage(text);
    }

    public void updateWaveData(Message mss) {
        try (DataInputStream dis = mss.reader()) {
            int wave = dis.readInt();
            int exp = dis.readInt();
            byte num = dis.readByte();
            Item[] inventory = new Item[num];
            for (int i = 0; i < num; i++) {
                inventory[i] = new Item(dis.readUTF());
            }
            try (Connection conn = DbManager.getInstance().getConnection(DbManager.SAVE_DATA);
                 PreparedStatement smt = conn.prepareStatement(SQLStatement.SAVE_WAVE_DATA)
            ) {
                smt.setInt(1, exp);
                smt.setInt(2, wave);
                smt.setString(3, Utils.bagToString(inventory));
                smt.setInt(4, id);
                smt.executeUpdate();
                serverMessage("Wave data saved.");
            } catch (Exception e) {
                Log.error("save wave data: " + e.getMessage(), e);
            }
            waveState.wave = wave;
            waveState.exp = exp;
            waveState.inventory = inventory;
        } catch (Exception e) {
            Log.error("create wave data: " + e.getMessage(), e);
        }
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

    public synchronized int getCurrentEnergy() {
        long now = System.currentTimeMillis();
        long timePassed = now - lastUpdateEnergy;
        int energyGained = (int) (timePassed / (60 * 1000));
        lastUpdateEnergy = now;
        return Math.min(energy + energyGained, maxEnergy);
    }

    public void useEnergy(Message mss) {
        try(DataInputStream dis = mss.reader()) {
            int amount = dis.readInt();
            int currentEnergy = getCurrentEnergy();
            if (currentEnergy < amount) {
                serverMessage("Not enough energy.");
                return;
            }
            energy = currentEnergy + amount;
            lastUpdateEnergy = System.currentTimeMillis();
        } catch (Exception e) {
            Log.error("use energy: " + e.getMessage(), e);
        }
    }
}
