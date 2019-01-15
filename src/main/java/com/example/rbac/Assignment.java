package com.example.rbac;

import java.util.Date;

public class Assignment {
    private Object userId;
    private String roleName;
    private Long createdAt;

    public Object getUserId() {
        return userId;
    }

    public void setUserId(Object userId) {
        this.userId = userId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
//    CREATE TABLE `auth_assignment` (
//        `item_name` varchar(64) NOT NULL,
//        `user_id` varchar(64) NOT NULL,
//        `created_at` int(11) DEFAULT NULL,
//        PRIMARY KEY (`item_name`,`user_id`),
//        KEY `auth_assignment_user_id_idx` (`user_id`),
//        CONSTRAINT `auth_assignment_ibfk_1` FOREIGN KEY (`item_name`) REFERENCES `auth_item` (`name`) ON DELETE CASCADE ON UPDATE CASCADE
//        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
