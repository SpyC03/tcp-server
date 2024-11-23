SET
SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET
time_zone = "+07:00";

CREATE TABLE `users`
(
    `id`                 int(20) NOT NULL,
    `username`           varchar(30)  NOT NULL,
    `password`           varchar(100) NOT NULL,
    `status`             int(11) DEFAULT 0 COMMENT '0: Deactivate, 1: Active, 2: Block',
    `activated`          tinyint(4) NOT NULL DEFAULT 0,
    `online`             tinyint(1) NOT NULL DEFAULT 0,
    `last_attendance_at` bigint(20) DEFAULT 0,
    `ip_address`         longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
    `ban_until`          timestamp NULL DEFAULT NULL,
    `created_at`         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY                  `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

CREATE TABLE `players`
(
    `id`            int(11) NOT NULL,
    `user_id`       int(11) NOT NULL,
    `name`          varchar(15) NOT NULL,
    `point`         int(11) NOT NULL DEFAULT 0,
    `coin`          bigint(20) NOT NULL DEFAULT 0,
    `energy`        int(11) NOT NULL DEFAULT 0,
    `max_energy`    int(11) NOT NULL DEFAULT 100,
    `last_update_energy` bigint(20) NOT NULL DEFAULT 0,
    `number_cell_bag` int(11) NOT NULL DEFAULT 30,
    `bag`           longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    `online`        smallint(6) NOT NULL DEFAULT 0,
    `exp`           bigint(20) NOT NULL DEFAULT 0,
    `created_at`    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY             `user_id` (`user_id`),
    KEY             `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

CREATE TABLE `waves`
(
    `id`         int(11) NOT NULL,
    `user_id`    int(11) NOT NULL,
    `wave`       int(11) NOT NULL,
    `exp`        bigint(20) NOT NULL DEFAULT 0,
    `inventory`  longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    `created_at` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY         `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

ALTER TABLE `waves`
    MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1;

CREATE TABLE `items`(
    `id`     int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

ALTER TABLE `users`
    MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1;

ALTER TABLE `players`
    MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1;

COMMIT;