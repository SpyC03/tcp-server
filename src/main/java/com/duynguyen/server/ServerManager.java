package com.duynguyen.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import com.duynguyen.model.Char;
import com.duynguyen.model.User;

public class ServerManager {
    public static final ArrayList<User> users = new ArrayList<>();
    private static final ArrayList<String> ips = new ArrayList<>();
    private static final ArrayList<Char> chars = new ArrayList<>();
    public static final HashMap<String, Integer> countAttendanceByIp = new HashMap<>();
    private static final ReadWriteLock lockSession = new ReentrantReadWriteLock();
    private static final ReadWriteLock lockUser = new ReentrantReadWriteLock();
    private static final ReadWriteLock lockChar = new ReentrantReadWriteLock();


    @SuppressWarnings("unchecked")
    public static List<User> getUsers() {
        return (List<User>) users.clone();
    }

    public static int getNumberOnline() {
        return chars.size();
    }

    @SuppressWarnings("unchecked")
    public static List<Char> getChars() {
        return (List<Char>) chars.clone();
    }

    public static int frequency(String ip) {
        lockSession.readLock().lock();
        try {
            return Collections.frequency(ips, ip);
        } finally {
            lockSession.readLock().unlock();
        }
    }

    public static Char findCharByName(String name) {
        lockChar.readLock().lock();
        try {
            for (Char _char : chars) {
                if (_char != null && !_char.isCleaned && _char.name.equals(name)) {
                    return _char;
                }
            }
        } finally {
            lockChar.readLock().unlock();
        }
        return null;
    }

    public static void addChar(Char _char) {
        lockChar.writeLock().lock();
        try {
            chars.add(_char);
        } finally {
            lockChar.writeLock().unlock();
        }
    }

    public static void removeChar(Char _char) {
        lockChar.writeLock().lock();
        try {
            chars.removeIf(c -> c.id == _char.id);
        } finally {
            lockChar.writeLock().unlock();
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
            users.removeIf(u -> u.id == user.id);
        } finally {
            lockUser.writeLock().unlock();
        }
    }
}
