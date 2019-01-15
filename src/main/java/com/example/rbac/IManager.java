package com.example.rbac;

import java.util.List;
import java.util.Map;

public interface IManager extends ICheckAccess{

    Role createRole(String roleName);

    Permission createPermission(String permissionName);

    boolean add(Object object);

    boolean remove(Object object);

    boolean update(String name, Object object);

    Role getRole(String name);

    Map<String, Role> getRoles();

    Map<String, Role> getRolesByUser(Object userId);

    Map<String, Role> getChildRoles(String roleName);

    Permission getPermission(String permissionName);

    Map<String, Permission> getPermissions();

    Map<String, Permission> getPermissionsByUser(Object userId);

    Rule getRule(String ruleName);

    Map<String, Rule> getRules();

    boolean canAddChild(Item parent, Item child);

    boolean addChild(Item parent, Item child);

    boolean removeChild(Item parent, Item child);

    boolean removeChildren(Item parent);

    boolean hasChild(Item parent, Item child);

    Map<String, Item> getChildren(String parentName);

    Assignment assign(Item rule, Object userId);

    boolean revoke(Item rule, Object userId);

    boolean revokeAll(Object userId);

    Assignment getAssignment(String roleName, Object userId);

    Map<String, Assignment> getAssignments(Object userId);

    List<Object> getUserIdsByRole(String ruleName);

    boolean removeAll();

    boolean removeAllPermissions();

    boolean removeAllRoles();

    boolean removeAllRules();

    boolean removeAllAssignments();
}
