package com.example.rbac;

import java.util.Map;

public class AuthorRule extends Rule {

    private String name = "isAuthor";

    public boolean executeRule(Object userId,  Item item, Map params){
        return params.containsKey("post") ? ((Post)params.get("post")).getCreatedBy().equals(userId) : false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
