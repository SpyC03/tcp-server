package com.duynguyen.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import com.duynguyen.model.User;

public class ServerManager {
    public static ArrayList<User> users = new ArrayList<>();
    private static ArrayList<String> ips = new ArrayList<>();
    public static HashMap<String, Integer> countAttendanceByIp = new HashMap<>();
    private static ReadWriteLock lockSession = new ReentrantReadWriteLock();
    private static ReadWriteLock lockUser = new ReentrantReadWriteLock();


    @SuppressWarnings("unchecked")
    public static List<User> getUsers() {
        return (List<User>) users.clone();
    }

    public static int getNumberOnline() {
        return users.size();
    }

    public static int frequency(String ip) {
        lockSession.readLock().lock();
        try {
            return Collections.frequency(ips, ip);
        } finally {
            lockSession.readLock().unlock();
        }
    }

    public static void add(String ip) {
        lockSession.writeLock().lock();
        try {
            ips.add(ip);
        } finally {
            lockSession.writeLock().unlock();
        }
    }

    public static void remove(String ip) {
        lockSession.writeLock().lock();
        try {
            ips.remove(ip);
        } finally {
            lockSession.writeLock().unlock();
        }
    }

    public static User findUserByUserID(int id) {
        lockUser.readLock().lock();
        try {
            for (User user : users) {
                if (user.id == id) {
                    return user;
                }
            }
        } finally {
            lockUser.readLock().unlock();
        }
        return null;
    }

    public static long count(long id) {
        lockUser.readLock().lock();
        try {
            return users.stream().filter(u -> u.id == id).count();
        } finally {
            lockUser.readLock().unlock();
        }
    }

    public static User findUserByUsername(String username) {
        lockUser.readLock().lock();
        try {
            for (User user : users) {
                if (user.username.equals(username)) {
                    return user;
                }
            }
        } finally {
            lockUser.readLock().unlock();
        }
        return null;

    }

    public static void addUser(User user) {
        lockUser.writeLock().lock();
        try {
            users.add(user);
        } finally {
            lockUser.writeLock().unlock();
        }
    }


    public static void removeUser(User user) {
        lockUser.writeLock().lock();
        try {
            users.removeIf(u -> {
                if (u.id == user.id) {
                    return true;
                }
                return false;
            });
        } finally {
            lockUser.writeLock().unlock();
        }
    }
}
