package com.example.rbac;

import java.util.Map;

public class Rule implements IExecuteRule{
    private String name;
    private String data;
    private Long createdAt;
    private Long updatedAt;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public boolean executeRule(Object userId,  Item item, Map params) {
        return true;
    }
}
//    CREATE TABLE `auth_rule` (
//        `name` varchar(64) NOT NULL,
//        `data` blob,
//        `created_at` int(11) DEFAULT NULL,
//        `updated_at` int(11) DEFAULT NULL,
//        PRIMARY KEY (`name`)
//        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;