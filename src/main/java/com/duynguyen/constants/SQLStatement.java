package com.duynguyen.constants;

public class SQLStatement {
    public static final String GET_USER = "SELECT * FROM users WHERE username = ?";
    public static final String LOAD_PLAYER = "SELECT * FROM `players` WHERE `id` = ? LIMIT 1;";
    public static final String SAVE_DATA = "UPDATE `users` SET `online` = ?, `last_attendance_at` = ?, `ip_address` = ? WHERE `id` = ? LIMIT 1;";
    public static final String SAVE_DATA_PLAYER = "UPDATE `players` SET `energy` = ?, `max_energy` = ?, `exp` = ?, `coin` = ?, `point` = ?, `number_cell_bag` = ?, `bag` = ?, `online` = ? WHERE `id` = ? LIMIT 1;";
    public static final String REGISTER = "INSERT INTO users (username, password, status, activated, online) VALUES (?, ?, ?, ?, ?);";
    public static final String CHECK_PLAYER_EXIST = "SELECT COUNT(*) FROM `players` WHERE `name` = ?";
    public static final String CREATE_PLAYER = "INSERT INTO players(`user_id`, `name`, `coin`, `bag`) VALUES (?, ?, ?, ?)";
    public static final String LOAD_INIT_CHARACTER = "SELECT `players`.`id`, `players`.`name`, `players`.`online`, `players`.`exp` FROM `players` WHERE `players`.`user_id` = ?";
    public static final String LOCK_ACCOUNT = "UPDATE `users` SET `status` = 0 WHERE `id` = ? LIMIT 1;";
}
