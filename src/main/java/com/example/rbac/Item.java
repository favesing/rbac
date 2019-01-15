package com.example.rbac;

import java.util.Date;

public class Item {

    public final static int TYPE_ROLE = 1;
    public final static int TYPE_PERMISSION = 2;

    private String name;
    private int type;
    private String description;
    private String ruleName;
    private String data;
    private Long createdAt;
    private Long updatedAt;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
//    CREATE TABLE `auth_item` (
//        `name` varchar(64) NOT NULL,
//        `type` smallint(6) NOT NULL,
//        `description` text,
//        `rule_name` varchar(64) DEFAULT NULL,
//        `data` blob,
//        `created_at` int(11) DEFAULT NULL,
//        `updated_at` int(11) DEFAULT NULL,
//        PRIMARY KEY (`name`),
//        KEY `rule_name` (`rule_name`),
//        KEY `type` (`type`),
//        CONSTRAINT `auth_item_ibfk_1` FOREIGN KEY (`rule_name`) REFERENCES `auth_rule` (`name`) ON DELETE SET NULL ON UPDATE CASCADE
//        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;