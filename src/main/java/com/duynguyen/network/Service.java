package com.duynguyen.network;

import com.duynguyen.constants.CMD;
import com.duynguyen.model.Char;
import com.duynguyen.model.Item;
import com.duynguyen.server.Server;
import com.duynguyen.utils.Log;
import lombok.Getter;
import lombok.Setter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Service extends AbsService{
    public final Session session;
    @Setter
    @Getter
    public Char player;

    public Thread threadUpdateChar;
    private volatile boolean running = false;
    private volatile long lastUpdateEveryFiveSecond;
    private volatile long lastUpdateEveryMinute;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public Service(Session session) {
        this.session = session;
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



    public void playerLoadAll() {
        if (player == null) {
            Log.error("Player is null in playerLoadAll");
            return;
        }
    
        if (player.isCleaned) {
            return;
        }
        Message ms = null;
        update();
        try {
            ms = messageInGame(CMD.PLAYER_LOAD_ALL);
            DataOutputStream ds = ms.writer();
            
            ds.writeInt(player.id);
            charInfo(ms, player);
            ds.flush();
            sendMessage(ms);
            
        } catch (Exception ex) {
            Log.error("Error in playerLoadAll", ex);
            throw new RuntimeException("Failed to load player data", ex);
        } finally {
            if (ms != null) {
                ms.cleanup();
            }
        }
    }

    public void updateEnergy(){
            Message ms = messageInGame(CMD.PLAYER_LOAD_ENERGY);
        try (DataOutputStream ds = ms.writer();){
            ds.writeInt(player.energy);
            ds.writeInt(player.maxEnergy);
            ds.writeLong(player.lastUpdateEnergy);
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
            ds.writeLong(_char.coin);

            //load bag
            byte num = (byte) _char.bag.length;
            ds.writeByte(num);
            for(int i = 0; i < num; i++) {
                String itemId = _char.bag[i].itemId();
                ds.writeUTF(itemId);
            }

            //wave data
            ds.writeInt(_char.waveState.wave);
            ds.writeInt(_char.waveState.exp);
            byte num1 = (byte) _char.waveState.inventory.length;
            ds.writeByte(num1);
            for(int i = 0; i < num1; i++) {
                String itemId = _char.waveState.inventory[i].itemId();
                ds.writeUTF(itemId);
            }
        } catch (Exception e) {
            Log.error("charInfo err: " + e.getMessage(), e);
        }
    }

    public void serverMessage(String text) {
        try {
            Log.info("Server message: " + text);
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
        if (threadUpdateChar != null && threadUpdateChar.isAlive()) {
            running = false;
            threadUpdateChar.interrupt();
            try {
                threadUpdateChar.join(1000);
            } catch (InterruptedException e) {
                Log.error("Error stopping update thread", e);
            }
        }

        running = true;
        threadUpdateChar = new Thread(this::updatePlayer, "UpdatePlayer-" + player.id);
        threadUpdateChar.setDaemon(true);
        threadUpdateChar.start();
        
        Log.info("Started update thread for player: " + player.id);
    }

    public void updatePlayer() {
        Log.info("Update player thread started");
        
        while (running && Server.start) {
            long startTime = System.currentTimeMillis();
            
            try {
                lock.readLock().lock();
                try {
                    long now = System.currentTimeMillis();
                    boolean isUpdateEveryFiveSecond = (now - lastUpdateEveryFiveSecond) >= 5000;
                    boolean isUpdateEveryMinute = (now - lastUpdateEveryMinute) >= 60000;

                    if (!isUpdateEveryFiveSecond && !isUpdateEveryMinute) {
                        continue;
                    }

                    if (player != null) {
                        lock.readLock().unlock();
                        lock.writeLock().lock();
                        try {
                            if (isUpdateEveryFiveSecond) {
                                lastUpdateEveryFiveSecond = now;
                                player.updateEveryFiveSecond();
                            }
                            
                            if (isUpdateEveryMinute) {
                                lastUpdateEveryMinute = now;
                                player.updateEveryMinute();
                            }
                        } finally {
                            lock.readLock().lock();
                            lock.writeLock().unlock();
                        }
                    }
                } finally {
                    lock.readLock().unlock();
                }

                long processingTime = System.currentTimeMillis() - startTime;
                long sleepTime = Math.max(0, 1000 - processingTime);
                
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }

            } catch (InterruptedException e) {
                Log.info("Update thread interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.error("Error in update loop", e);
            }
        }
        
        Log.info("Update player thread stopped");
    }

    public void stopUpdate() {
        running = false;
        if (threadUpdateChar != null) {
            threadUpdateChar.interrupt();
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