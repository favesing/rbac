
CREATE TABLE `auth_item` (
   `name` varchar(64) NOT NULL,
   `type` smallint(6) NOT NULL,
   `description` text,
   `rule_name` varchar(64) DEFAULT NULL,
   `data` text,
   `created_at` bigint(18) DEFAULT NULL,
   `updated_at` bigint(18) DEFAULT NULL,
   PRIMARY KEY (`name`),
   KEY `rule_name` (`rule_name`),
   KEY `type` (`type`),
   CONSTRAINT `auth_item_ibfk_1` FOREIGN KEY (`rule_name`) REFERENCES `auth_rule` (`name`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `auth_item_child` (
   `parent` varchar(64) NOT NULL,
   `child` varchar(64) NOT NULL,
   PRIMARY KEY (`parent`,`child`),
   KEY `child` (`child`),
   CONSTRAINT `auth_item_child_ibfk_1` FOREIGN KEY (`parent`) REFERENCES `auth_item` (`name`) ON DELETE CASCADE ON UPDATE CASCADE,
   CONSTRAINT `auth_item_child_ibfk_2` FOREIGN KEY (`child`) REFERENCES `auth_item` (`name`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `auth_rule` (
    `name` varchar(64) NOT NULL,
    `data` text,
    `created_at` bigint(18) DEFAULT NULL,
    `updated_at` bigint(18) DEFAULT NULL,
    PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `auth_assignment` (
   `item_name` varchar(64) NOT NULL,
   `user_id` varchar(64) NOT NULL,
   `created_at` bigint(18) DEFAULT NULL,
   PRIMARY KEY (`item_name`,`user_id`),
   KEY `auth_assignment_user_id_idx` (`user_id`),
   CONSTRAINT `auth_assignment_ibfk_1` FOREIGN KEY (`item_name`) REFERENCES `auth_item` (`name`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;