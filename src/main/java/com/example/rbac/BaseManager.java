package com.example.rbac;

import java.util.*;

public abstract class BaseManager implements IManager{

    private List<String> defaultRoles;

    abstract Item getItem(String name);

    abstract Map<String, Item> getItems(int itemType);

    abstract boolean addItem(Item item);

    abstract boolean addRule(Rule rule);

    abstract boolean removeItem(Item item);

    abstract boolean removeRule(Rule rule);

    abstract boolean updateItem(String itemName, Item item);

    abstract boolean updateRule(String ruleName, Rule rule);
    @Override
    public Role createRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        return role;
    }

    @Override
    public Permission createPermission(String permissionName) {
        Permission permission = new Permission();
        permission.setName(permissionName);
        return permission;
    }

    @Override
    public boolean add(Object object) {
        if(object instanceof Item){
            Item item = (Item)object;
            if(item.getRuleName() != null && this.getRule(item.getRuleName()) == null){
                Rule rule = new Rule();
                rule.setName(item.getRuleName());
                this.addRule(rule);
            }
            return this.addItem(item);
        }else if(object instanceof Rule){
            return this.addRule((Rule)object);
        }
        throw new IllegalArgumentException("Adding unsupported object type.");
    }

    @Override
    public boolean remove(Object object){
        if(object instanceof Item){
            return this.removeItem((Item)object);
        }else if(object instanceof Rule){
            return this.removeRule((Rule)object);
        }
        throw new IllegalArgumentException("Removing unsupported object type.");
    }

    @Override
    public boolean update(String name, Object object) {
        if(object instanceof Item){
            Item item = (Item)object;
            if(item.getRuleName() != null && this.getRule(item.getRuleName()) == null){
                Rule rule = new Rule();
                rule.setName(item.getRuleName());
                this.addRule(rule);
            }
            return this.updateItem(name, item);
        }else if(object instanceof Rule){
            return this.updateRule(name, (Rule)object);
        }
        throw new IllegalArgumentException("Adding unsupported object type.");
    }

    @Override
    public Role getRole(String name) {
        Item item = this.getItem(name);
        return item != null && item instanceof Item && item.getType() == Item.TYPE_ROLE ? (Role) item : null;
    }

    @Override
    public Map<String, Role> getRoles() {
        Map<String, Role> result = new HashMap<>();
        Map<String, Item> list = this.getItems(Item.TYPE_ROLE);
        for(Map.Entry<String, Item> entry : list.entrySet()){
            result.put(entry.getKey(), (Role) entry.getValue());
        }
        return result;
    }

    @Override
    public Permission getPermission(String permissionName) {
        Item item = this.getItem(permissionName);
        return item != null && item instanceof Item && item.getType() == Item.TYPE_PERMISSION ? (Permission)item : null;
    }

    @Override
    public Map<String, Permission> getPermissions() {
        Map<String, Permission> result = new HashMap<>();
        Map<String, Item> list = this.getItems(Item.TYPE_PERMISSION);
        for(Map.Entry<String, Item> entry : list.entrySet()){
            result.put(entry.getKey(), (Permission) entry.getValue());
        }
        return result;
    }


    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<String> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public Map<String, Role> getDefaultRoleObject(){
        Map<String, Role> roleMap = new HashMap<>();
        if(this.defaultRoles != null && this.defaultRoles.size() > 0){
            for (String roleName: this.defaultRoles) {
                roleMap.put(roleName, this.createRole(roleName));
            }
        }
        return roleMap;
    }

    public boolean executeRule(Object userId, Item item, Map params){
        if(item.getRuleName() == null){
            return true;
        }
        Rule rule = this.getRule(item.getRuleName());
        if(rule != null && rule instanceof Rule){
            return rule.executeRule(userId, item, params);
        }
        throw new IllegalArgumentException("Rule not found: " + item.getRuleName());
    }

    public boolean hasNoAssignments(Map<String, Assignment> assignments){
        return (assignments == null || assignments.size() == 0) && (this.defaultRoles == null || this.defaultRoles.size() == 0);
    }
}
