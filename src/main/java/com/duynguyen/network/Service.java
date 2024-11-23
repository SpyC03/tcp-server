package com.duynguyen.network;

import com.duynguyen.constants.CMD;
import com.duynguyen.model.Char;
import com.duynguyen.model.Item;
import com.duynguyen.server.ServerManager;
import com.duynguyen.utils.Log;
import lombok.Getter;
import lombok.Setter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.duynguyen.server.Server;


public class Service extends AbsService{
    public final Session session;
    @Setter
    @Getter
    public Char player;

    public Thread threadUpdateChar;
    public long lastUpdateEveryFiveSecond, lastUpdateEveryFiveMinutes;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public Service(Session session) {
        this.session = session;
        update();
    }

    @Override
    public void sendMessage(Message ms) {
        if (this.session != null) {
            this.session.sendMessage(ms);
        }
    }

    public void loginSuccess() {
        try {
            Message ms = messageNotInGame(CMD.LOGIN_OK);
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Log.info("Response login success error" + ex);
        }
    }

    public void registerSuccess() {
        try {
            Message ms = messageNotInGame(CMD.REGISTER_OK);
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Log.info("Response register success error" + ex);
        }
    }

    public void addCoin(int coin) {
        try {
            Message ms = messageInGame(CMD.ME_UP_COIN_BAG);
            DataOutputStream ds = ms.writer();
            ds.writeInt(coin);
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void addExp(long exp) {
        try {
            Message ms = new Message(CMD.PLAYER_UP_EXP);
            DataOutputStream ds = ms.writer();
            ds.writeLong(exp);
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
        } catch (IOException ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void loadCoin() {
        try {
            Message ms = messageInGame(CMD.ME_LOAD_INFO);
            DataOutputStream ds = ms.writer();
            ds.writeInt((int)player.coin);
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
        } catch (IOException ex) {
            Log.error("load info err: " + ex.getMessage(), ex);
        }
    }



    public void playerLoadAll(Char pl) {
        try {
            if (pl.isCleaned) {
                return;
            }
            Message ms = messageInGame(CMD.PLAYER_LOAD_ALL);
            DataOutputStream ds = ms.writer();
            ds.writeInt(pl.id);
            charInfo(ms, pl);
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void updateEnergy(){
            Message ms = messageInGame(CMD.UPDATE_ENERGY);
        try (DataOutputStream ds = ms.writer();){
            ds.writeInt(player.energy);
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void charInfo(Message ms, Char _char) {
        try(DataOutputStream ds = ms.writer()) {
            ds.writeUTF(_char.name);
            ds.writeInt(_char.energy);
            ds.writeInt(_char.maxEnergy);
            ds.writeByte(_char.level);

            //wave data
            ds.writeInt(_char.waveState.wave);
            ds.writeInt(_char.waveState.exp);
            byte num1 = (byte) _char.waveState.inventory.length;
            ds.writeByte(num1);
            for(int i = 0; i < num1; i++) {
                String itemId = _char.waveState.inventory[i].itemId();
                Log.info("itemId: " + itemId);
                ds.writeUTF(itemId);
            }
        } catch (Exception e) {
            Log.error("charInfo err: " + e.getMessage(), e);
        }
    }

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

    public void removeItem(int index) {
        try {
            Message ms = new Message(CMD.ITEM_BAG_CLEAR);
            DataOutputStream ds = ms.writer();
            ds.writeByte(index);
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void addItem(Item item) {
        try {
            Message ms = new Message(CMD.ITEM_BAG_ADD);
            DataOutputStream ds = ms.writer();
            ds.writeUTF(item.itemId());
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void levelUp() {
        try {
            Message ms = messageInGame(CMD.ME_LOAD_LEVEL);
            DataOutputStream ds = ms.writer();
            ds.writeInt(player.maxEnergy);
            ds.writeLong(player.exp);
            ds.writeShort((short) player.potentialPoints);
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void showWait(String title) {
        try {
            Message ms = messageInGame(CMD.SHOW_WAIT);
            DataOutputStream ds = ms.writer();
            ds.writeUTF(title);
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void update() {
        this.threadUpdateChar = new Thread(new Runnable() {
            @Override
            public void run() {
                updatePlayer();
            }

        });
    }

    public void updatePlayer() {
        while (Server.start) {
            try {
                long l1 = System.currentTimeMillis();
                lock.readLock().lock();
                try {
                    long now = System.currentTimeMillis();
                    boolean isUpdateEveryFiveSecond = ((now - this.lastUpdateEveryFiveSecond) >= 1000);
                    if (isUpdateEveryFiveSecond) {
                        this.lastUpdateEveryFiveSecond = now;
                    }

                    boolean isUpdateEveryFiveMinutes = ((now - this.lastUpdateEveryFiveMinutes) >= 300000);
                    if (isUpdateEveryFiveMinutes) {
                        this.lastUpdateEveryFiveMinutes = now;
                    }

                    if (isUpdateEveryFiveSecond || isUpdateEveryFiveMinutes) {
                        List<Char> chars = ServerManager.getChars();
                        for (Char _char : chars) {
                            if (_char != null) {
                                if (isUpdateEveryFiveSecond) {
                                    _char.updateEveryFiveSecond();
                                }
                                if (isUpdateEveryFiveMinutes) {
                                    _char.updateEveryFiveMinutes();
                                }
                            }
                        }
                    }
                } finally {
                    lock.readLock().unlock();
                }

                long l2 = System.currentTimeMillis() - l1;
                if (l2 >= 1000L) {
                    continue;
                }
                try {
                    Thread.sleep(1000L - l2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void createCharacter() {
        try {
            Message ms = messageInGame(CMD.CREATE_PLAYER);
            sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}