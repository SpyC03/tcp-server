package com.duynguyen.database.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.duynguyen.server.Config;
import com.duynguyen.utils.Log;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DbManager {
    public class ZConnection {

        private Connection connection;
        private int timeOut;
    
        public ZConnection(int timeOut) {
            this.timeOut = timeOut;
        }
    
        public Connection getConnection() {
            try {
                if (connection != null) {
                    if (!connection.isValid(timeOut)) {
                        connection.close();
                    }
                }
                if (connection == null || connection.isClosed()) {
                    connection = DbManager.getInstance().getConnection();
                    return getConnection();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return connection;
        }
    }
    private static DbManager instance = null;
        private HikariDataSource hikariDataSource;
        private List<ZConnection> connections;
        private final int timeOut;
    
        //related to game logic, logs for example
        public static final int GAME = 0;
        public static final int LOGIN = 1;
        //data user
        public static final int SAVE_DATA = 2;
        public static final int SERVER = 3;
        public static final int UPDATE = 4;
    
        public static DbManager getInstance() {
            if (instance == null) {
                instance = new DbManager();
            }
            return instance;
        }
    
        private DbManager() {
            timeOut = 10;
            connections = new ArrayList<>();
            connections.add(new ZConnection(timeOut));
            connections.add(new ZConnection(timeOut));
            connections.add(new ZConnection(timeOut));
            connections.add(new ZConnection(timeOut));
            connections.add(new ZConnection(timeOut));
            connections.add(new ZConnection(timeOut));
            connections.add(new ZConnection(timeOut));
            connections.add(new ZConnection(timeOut));
        }
    
        public Connection getConnection() throws SQLException {
            return this.hikariDataSource.getConnection();
        }
    
        public boolean start() {
            if (this.hikariDataSource != null) {
                Log.warn("DB Connection Pool has already been created.");
                return false;
            } else {
                try {
                    Config serverConfig = Config.getInstance();
                    HikariConfig config = new HikariConfig();
                    config.setJdbcUrl(Config.getInstance().getJdbcUrl());
                    config.setDriverClassName(serverConfig.getDbDriver());
                    config.setUsername(serverConfig.getDbUser());
                    config.setPassword(serverConfig.getDbPassword());
                    config.addDataSourceProperty("minimumIdle", serverConfig.getDbMinConnections());
                    config.addDataSourceProperty("maximumPoolSize", serverConfig.getDbMaxConnections());
                    config.setConnectionTimeout(serverConfig.getDbConnectionTimeout());
                    config.setLeakDetectionThreshold(serverConfig.getDbLeakDetectionThreshold());
                    config.setIdleTimeout(serverConfig.getDbIdleTimeout());
                    config.addDataSourceProperty("cachePrepStmts", "true");
                    config.addDataSourceProperty("prepStmtCacheSize", "250");
                    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    
                    this.hikariDataSource = new HikariDataSource(config);
                    Log.info("DB Connection Pool has created.");
                    return true;
                } catch (Exception e) {
                    Log.error("DB Connection Pool Creation has failed.");
                    return false;
                }
            }
        }
    
        public ArrayList<HashMap<String, Object>> convertResultSetToList(ResultSet rs) {
            ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
            try {
                ResultSetMetaData resultSetMetaData = rs.getMetaData();
                int count = resultSetMetaData.getColumnCount();
                while (rs.next()) {
                    HashMap<String, Object> map = new HashMap<>();
                    int index = 1;
                    while (index <= count) {
                        int type = resultSetMetaData.getColumnType(index);
                        String name = resultSetMetaData.getColumnName(index);
                        switch (type) {
                            case -5: {
                                map.put(name, rs.getLong(index));
                                break;
                            }
                            case 6: {
                                map.put(name, rs.getFloat(index));
                                break;
                            }
                            case 12: {
                                map.put(name, rs.getString(index));
                                break;
                            }
                            case 1: {
                                map.put(name, rs.getString(index));
                                break;
                            }
                            case 4: {
                                map.put(name, rs.getInt(index));
                                break;
                            }
                            case 16: {
                                map.put(name, rs.getBoolean(index));
                                break;
                            }
                            case -7: {
                                map.put(name, rs.getByte(index));
                                break;
                            }
    
                            default: {
                                map.put(name, rs.getObject(index));
                                break;
                            }
                        }
                        ++index;
                    }
                    list.add(map);
                }
                rs.close();
            } catch (SQLException sQLException) {
                Log.error("convertResultSetToList ex: " + sQLException.getMessage());
            }
            return list;
        }
    
        public Connection getConnection(int index) throws SQLException {
            if (hikariDataSource == null) {
                while (hikariDataSource == null) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ex) {
                        Log.error("getConnection() EXCEPTION: " + ex.getMessage(), ex);
                    }
                }
            }
            return connections.get(index).getConnection();
        }
    
        public void shutdown() {
            try {
                if (this.hikariDataSource != null) {
                    this.hikariDataSource.close();
                    Log.info("DB Connection Pool is shutting down.");
                }
                this.hikariDataSource = null;
            } catch (Exception e) {
                Log.warn("Error when shutting down DB Connection Pool");
            }
        }
    
        public int update(String sql, Object... params) {
            try {
                Connection conn = getInstance().getConnection(UPDATE);
                PreparedStatement stmt = conn.prepareStatement(sql);
                try {
                    for (int i = 0; i < params.length; i++) {
                        stmt.setObject(i + 1, params[i]);
                    }
                    int result = stmt.executeUpdate();
                    return result;
                } finally {
                    stmt.close();
                }
            } catch (SQLException e) {
                Log.error("update() EXCEPTION: " + e.getMessage(), e);
            }
            return -1;
        }
}
