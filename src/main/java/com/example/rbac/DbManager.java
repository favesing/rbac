package com.example.rbac;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import javafx.util.Pair;
import org.apache.ibatis.jdbc.SQL;
import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;

@Component
public class DbManager extends BaseManager implements ApplicationContextAware
{
    private static final Logger log = LoggerFactory.getLogger(DbManager.class);

    public String itemTable = "auth_item";
    public String itemChildTable = "auth_item_child";
    public String ruleTable = "auth_rule";
    public String assignmentTable = "auth_assignment";

    private ApplicationContext applicationContext;
    private ConnectionHelper connectionHelper;
    private ICache cache;
    private String cacheKey = "rbac";

    protected Map<String, Item> items;
    protected Map<String, Rule> rules;
    protected Map<String, List<String>> parents;
    private Map<Object, Map<String, Assignment>> checkAccessAssignments;

    public DbManager() {
        cache = new MapCache();
    }

    @Override
    Item getItem(String name) {
        if (name == null || name.length() == 0)
            return null;
        if (this.items != null && this.items.containsKey(name)) {
            return this.items.get(name);
        }

        try {
            String sql = MessageFormat.format("select * from {0} where name=? limit 1", itemTable);
            ResultSet rs = connectionHelper.query(sql, name);

            if(rs == null || !rs.next()){
                rs.close();
                return null;
            }
            Item item = this.populateItem(rs);
            rs.close();
            return item;

        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    Map<String, Item> getItems(int itemType){

        try{
            String sql = MessageFormat.format("select * from {0} where type=?", itemTable);
            ResultSet rs = connectionHelper.query(sql, itemType);
            Map<String, Item> result = new TreeMap<>();
            while (rs.next()){
                Item item = this.populateItem(rs);
                result.put(item.getName(), item);
            }
            rs.close();
            return result;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    boolean addItem(Item item){
        Long now = System.currentTimeMillis();
        if (item.getCreatedAt() == null) {
            item.setCreatedAt(now);
        }
        if (item.getUpdatedAt() == null) {
            item.setUpdatedAt(now);
        }
        try {
            String sql = MessageFormat.format("insert into {0} (name, type, description, rule_name, data, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?)", this.itemTable);
            connectionHelper.exec(sql, item.getName(), item.getType(), item.getDescription(), item.getRuleName(), item.getData(), item.getCreatedAt(), item.getUpdatedAt());
            this.invalidateCache();
            return true;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    boolean removeItem(Item item){
        try {
            String deleteFromItemChildTable = MessageFormat.format("delete from {0} where parent=? or child=?", itemChildTable);
            connectionHelper.exec(deleteFromItemChildTable, item.getName());

            String deleteFromAssignmentTable = MessageFormat.format("delete from {0} where item_name=?", assignmentTable);
            connectionHelper.exec(deleteFromAssignmentTable, item.getName());

            String deleteFromItemTable = MessageFormat.format("delete from {0} where name=?", itemTable);
            connectionHelper.exec(deleteFromItemTable, item.getName());

            this.invalidateCache();
            return true;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    boolean updateItem(String itemName, Item item){

        try {
            String updateParentFromItemChildTable = MessageFormat.format("update {0} set parent=? where parent=?", itemChildTable);
            connectionHelper.update(updateParentFromItemChildTable, item.getName(), itemName);

            String updateChildFromItemChildTable = MessageFormat.format("update {0} set child=? where child=?", itemChildTable);
            connectionHelper.update(updateChildFromItemChildTable, item.getName(), itemName);

            String updateChildFromAssignmentTable = MessageFormat.format("update {0} set item_name=? where item_name=?", assignmentTable);
            connectionHelper.update(updateChildFromItemChildTable, item.getName(), itemName);

            item.setUpdatedAt(System.currentTimeMillis());
            String updateFromItemTable = MessageFormat.format("update {0} set name=?, description=?, rule_name=?, data=?, updated_at=? where name=?", itemTable);
            connectionHelper.update(updateFromItemTable, item.getName(), item.getDescription(), item.getRuleName(), item.getData(), item.getUpdatedAt(), itemName);

            this.invalidateCache();
            return true;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Rule getRule(String ruleName) {
        if(this.rules != null){
            return this.rules.containsKey(ruleName) ? this.rules.get(ruleName) : null;
        }
        try {
            String sql = MessageFormat.format("select data from {0} where name=? limit 1", ruleTable);
            ResultSet rs = connectionHelper.query(sql, ruleName);
            boolean exits = rs.next();
            if(!exits){
                rs.close();
                return null;
            }
            String data = rs.getString("data");
            Class clazz = Class.forName(ruleName);
            Rule rule = (Rule) JSONObject.parseObject(data, clazz);
            rs.close();
            return rule;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Map<String, Rule> getRules() {
        if(this.rules != null){
            return this.rules;
        }
        try {
            String sql = MessageFormat.format("select * from {0}", ruleTable);
            ResultSet rs = connectionHelper.query(sql, null);
            Map<String, Rule> result = new TreeMap<>();
            while (rs.next()){
                String name = rs.getString("name");
                String data = rs.getString("data");
                Class clazz = Class.forName(name);
                Rule rule = (Rule) JSONObject.parseObject(data, clazz);
                result.put(name, rule);
            }
            rs.close();
            return result;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    boolean addRule(Rule rule){
        long now = System.currentTimeMillis();
        if (rule.getCreatedAt() == null) {
            rule.setCreatedAt(now);
        }
        if (rule.getUpdatedAt() == null) {
            rule.setUpdatedAt(now);
        }
       try {
           rule.setName(rule.getClass().getTypeName());
           String data = JSONObject.toJSONString(rule);
           String sql = MessageFormat.format("insert into {0} (name, data, created_at, updated_at) values (?, ?, ?, ?)", ruleTable);
           connectionHelper.exec(sql, rule.getName(), data, rule.getCreatedAt(), rule.getUpdatedAt());
           this.invalidateCache();
           return true;
       }catch (SQLException ex){
           throw new RuntimeException(ex);
       }
    }

    @Override
    boolean removeRule(Rule rule) {
        try {
            String updateFromItemTable = MessageFormat.format("update {0} set rule_name=null where rule_name=?", itemTable);
            connectionHelper.update(updateFromItemTable, rule.getName());

            String deleteFromRuleTable = MessageFormat.format("delete from {0} where name=?", ruleTable);
            connectionHelper.exec(deleteFromRuleTable, rule.getName());

            this.invalidateCache();
            return true;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    boolean updateRule(String ruleName, Rule rule){
        try {
            String updateFromItemTable = MessageFormat.format("update {0} set rule_name=? where rule_name=?", itemTable);
            connectionHelper.update(updateFromItemTable, rule.getName(), ruleName);

            rule.setUpdatedAt(System.currentTimeMillis());
            String updateFromRuleTable = MessageFormat.format("update {0} set name=?, data=?, updated_at=? where name=?", ruleTable);
            connectionHelper.update(updateFromRuleTable, rule.getName(), rule.getData(), rule.getUpdatedAt(), ruleName);

            return false;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Map<String, Role> getRolesByUser(Object userId) {
        if(userId == null){
            return new TreeMap<>();
        }

        try {
            Map<String, Role> roles = this.getDefaultRoleObject();
            String sql = MessageFormat.format("select b.* from {0} as a, {1} as b where a.item_name=b.name and a.user_id=? and b.type=1", assignmentTable, itemTable);
            ResultSet rs = connectionHelper.query(sql, userId);

            while (rs.next()){
                Item item = this.populateItem(rs);
                roles.put(item.getName(), (Role) item);
            }
            rs.close();
            return roles;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Map<String, Role> getChildRoles(String roleName) {
        Role role = this.getRole(roleName);
        if(role == null){
            throw new IllegalArgumentException(String.format("Role: %s not found.", roleName));
        }

        Map<String, Boolean> result = new TreeMap<>();
        this.getChildrenRecursive(roleName, this.getChildrenList(), result);

        Map<String, Role> roles = new TreeMap<>();
        roles.put(roleName, role);

        for (Role roleItem : this.getRoles().values()){
            if(result.containsKey(roleItem.getName())){
                roles.put(roleItem.getName(), roleItem);
            }
        }

        return roles;
    }

    @Override
    public Map<String, Permission> getPermissionsByUser(Object userId) {
        if(this.isEmptyUserId(userId)){
            return new HashMap<>();
        }

        Map<String, Permission> directPermission = this.getDirectPermissionsByUser(userId);
        Map<String, Permission> inheritedPermission = this.getInheritedPermissionsByUser(userId);
        directPermission.putAll(inheritedPermission);
        return directPermission;
    }

    @Override
    public boolean canAddChild(Item parent, Item child) {
        return !this.detectLoop(parent, child);
    }

    @Override
    public boolean addChild(Item parent, Item child) {
        if(parent.getName().equals(child.getName())){
            throw new IllegalArgumentException(String.format("Cannot add '%s' as a child of itself.", parent.getName()));
        }
        if(parent instanceof Permission && child instanceof Role){
            throw new IllegalArgumentException("Cannot add a role as a child of a permission.");
        }
        if(this.detectLoop(parent, child)){
            throw new IllegalArgumentException(String.format("Cannot add '%s' as a child of '%s'. A loop has been detected.", child.getName(), parent.getName()));
        }

        try {
            String sql = MessageFormat.format("insert into {0} (parent, child) values(?, ?)", itemChildTable);
            connectionHelper.exec(sql, parent.getName(), child.getName());
            return true;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean removeChild(Item parent, Item child) {
        try {
            String sql = MessageFormat.format("delete from {0} where parent=? and child=?", itemChildTable);
            connectionHelper.exec(sql, parent.getName(), child.getName());
            this.invalidateCache();
            return true;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean removeChildren(Item parent) {
        try {
            String sql = MessageFormat.format("delete from {0} where parent=?", itemChildTable);
            connectionHelper.exec(sql, parent.getName());
            this.invalidateCache();
            return true;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean hasChild(Item parent, Item child) {
        try {
            String sql = MessageFormat.format("select 1 from {0} where parent=? and child=? limit 1", itemChildTable);
            ResultSet rs = connectionHelper.query(sql, parent.getName(), child.getName());
            boolean exits = rs.next();
            rs.close();
            return exits;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Map<String, Item> getChildren(String parentName) {
        try {
            String sql = MessageFormat.format("select name, type, description, rule_name, data, created_at, updated_at from {0}, {1} where parent=? and name=child", itemTable, itemChildTable);
            ResultSet rs = connectionHelper.query(sql, parentName);
            Map<String, Item> map = new TreeMap<>();
            while (rs.next()){
                Item item = this.populateItem(rs);
                map.put(item.getName(), item);
            }
            rs.close();
            return map;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Assignment assign(Item rule, Object userId) {
        Assignment assignment = new Assignment();
        assignment.setUserId(userId);
        assignment.setRoleName(rule.getName());
        assignment.setCreatedAt(System.currentTimeMillis());

        try {
            String sql = MessageFormat.format("insert into {0} (user_id, item_name, created_at) values(?, ?, ?)", assignmentTable);
            connectionHelper.query(sql, userId, assignment.getRoleName(), assignment.getCreatedAt());
            if(this.checkAccessAssignments.containsKey(userId)){
                this.checkAccessAssignments.remove(userId);
            }
            return assignment;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean revoke(Item item, Object userId) {
        if(this.isEmptyUserId(userId)){
            return false;
        }
        if(this.checkAccessAssignments.containsKey(userId)){
            this.checkAccessAssignments.remove(userId);
        }
        try {
            String sql = MessageFormat.format("delete from {0} where user_id=? and item_name=?", assignmentTable);
            connectionHelper.exec(sql, userId, item.getName());
            return true;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean revokeAll(Object userId) {
        if(this.isEmptyUserId(userId)){
            return false;
        }
        if(this.checkAccessAssignments.containsKey(userId)){
            this.checkAccessAssignments.remove(userId);
        }
        try {
            String sql = MessageFormat.format("delete from {0} where user_id=?", assignmentTable);
            connectionHelper.exec(sql, userId);
            return true;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Assignment getAssignment(String roleName, Object userId) {
        if(this.isEmptyUserId(userId)){
            return null;
        }
        try {
            String sql = MessageFormat.format("select * from {0} where user_id=? and item_name=?", assignmentTable);
            ResultSet rs = connectionHelper.query(sql, userId, roleName);
            boolean exits = rs.next();
            if(!exits){
                return null;
            }
            Assignment assignment = new Assignment();
            assignment.setUserId(rs.getObject("user_id"));
            assignment.setRoleName(rs.getString("item_name"));
            assignment.setCreatedAt(rs.getLong("created_at"));
            return assignment;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Map<String, Assignment> getAssignments(Object userId) {
        if(this.isEmptyUserId(userId)){
            return new HashMap<>();
        }
        try {
            String sql = MessageFormat.format("select * from {0} where user_id=?", assignmentTable);
            ResultSet rs = connectionHelper.query(sql, userId);
            Map<String, Assignment> result = new TreeMap<>();
            while (rs.next()){
                Assignment assignment = new Assignment();
                assignment.setUserId(rs.getObject("user_id"));
                assignment.setRoleName(rs.getString("item_name"));
                assignment.setCreatedAt(rs.getLong("created_at"));
                result.put(assignment.getRoleName(), assignment);
            }

            return result;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<Object> getUserIdsByRole(String roleName) {
        if(!StringUtils.hasText(roleName)){
            return new ArrayList<>();
        }
        try {
            String sql = MessageFormat.format("select user_id from {0} where item_name=?", assignmentTable);
            ResultSet rs = connectionHelper.query(sql, roleName);
            List<Object> result = new ArrayList<>();
            while (rs.next()){
                Object userId = rs.getObject("user_id");
                result.add(userId);
            }
            return result;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean removeAll() {
        try {
            this.removeAllAssignments();

            String sql = MessageFormat.format("delete from {0}", itemChildTable);
            String sql2 = MessageFormat.format("delete from {0}", itemTable);
            String sql3 = MessageFormat.format("delete from {0}", ruleTable);
            connectionHelper.exec(sql, null);
            connectionHelper.exec(sql2, null);
            connectionHelper.exec(sql3, null);

            this.invalidateCache();
            return true;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean removeAllPermissions() {
        this.removeAllItems(Item.TYPE_PERMISSION);
        return true;
    }

    @Override
    public boolean removeAllRoles() {
        this.removeAllItems(Item.TYPE_ROLE);
        return true;
    }

    @Override
    public boolean removeAllRules() {
        try {
            String sql = MessageFormat.format("update {0} set rule_name=null", itemTable);
            connectionHelper.update(sql, null);

            String sql2 = MessageFormat.format("delete from {0}", ruleTable);
            connectionHelper.exec(sql2, null);
            this.invalidateCache();
            return true;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean removeAllAssignments() {
        try {
            this.checkAccessAssignments = new HashMap<>();

            String sql = MessageFormat.format("delete from {0}", assignmentTable);
            connectionHelper.exec(sql);

            return true;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean checkAccess(Object userId, String permissionName, Map params) {
        Map<String, Assignment> assignments = null;
        if(this.checkAccessAssignments.containsKey(userId)){
            assignments = this.checkAccessAssignments.get(userId);
        }else{
            assignments = this.getAssignments(userId);
            this.checkAccessAssignments.put(userId, assignments);
        }
        if(this.hasNoAssignments(assignments)){
            return false;
        }
        this.loadFromCache();
        if(this.items != null){
            return this.checkAccessFromCache(userId, permissionName, params, assignments);
        }
        return this.checkAccessRecursive(userId, permissionName, params, assignments);
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        DataSource dataSource = (DataSource) this.applicationContext.getBean(DataSource.class);
        this.connectionHelper = new ConnectionHelper(dataSource, false);
    }

    //region "protected"

    public void invalidateCache(){
        if(this.cache != null){
            this.cache.delete(this.cacheKey);
            this.items = null;
            this.rules = null;
            this.parents = null;
        }
        this.checkAccessAssignments = new HashMap<>();
    }

    protected boolean checkAccessFromCache(Object userId, String itemName, Map params, Map<String, Assignment> assignments){
        if(!this.items.containsKey(itemName)){
            return false;
        }

        Item item = this.items.get(itemName);
        log.debug(item instanceof Role ? "Checking role: " + itemName : "Checking permission: " + itemName, "checkAccessFromCache");

        if(!this.executeRule(userId, item, params)){
            return false;
        }

        if(assignments.containsKey(itemName) || this.getDefaultRoles() != null && this.getDefaultRoles().contains(itemName)){
            return true;
        }

        if(this.parents != null && this.parents.containsKey(itemName)){
            for (String parent : this.parents.get(itemName)){
                if(this.checkAccessFromCache(userId, parent, params, assignments)){
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean checkAccessRecursive(Object userId, String itemName, Map params, Map<String, Assignment> assignments){
        Item item = this.getItem(itemName);
        if(item == null){
            return false;
        }
        log.debug(item instanceof Role ? "Checking role: " + itemName : "Checking permission: " + itemName, "checkAccessRecursive");

        if(!this.executeRule(userId, item, params)){
            return false;
        }

        if(assignments.containsKey(itemName) || this.getDefaultRoles() != null && this.getDefaultRoles().contains(itemName)){
            return true;
        }

        try {
            String sql = MessageFormat.format("select parent from {0} where child=?", itemChildTable);
            ResultSet rs = connectionHelper.query(sql, itemName);
            List<String> parents = new ArrayList<>();
            while (rs.next()){
                String parent = rs.getString("parent");
                parents.add(parent);
            }
            rs.close();

            for (String parent : parents){
                if(this.checkAccessRecursive(userId, parent, params, assignments)){
                    return true;
                }
            }
            return false;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    public void loadFromCache(){
        if(this.items != null && !(this.cache instanceof ICache)){
            return;
        }

        Triplet<Map<String,Item>, Map<String, Rule>, Map<String, List<String>>> data = (Triplet<Map<String,Item>, Map<String, Rule>, Map<String, List<String>>>) this.cache.get(this.cacheKey);
        if(data != null && data.getSize() == 3){
            this.items = data.getValue0();
            this.rules = data.getValue1();
            this.parents = data.getValue2();
            return;
        }

        try {
            String sql = MessageFormat.format("select * from {0}", itemTable);
            ResultSet rs = connectionHelper.query(sql, null);
            this.items = this.populateItemMap(rs);

            String sql2 = MessageFormat.format("select * from {0}", ruleTable);
            ResultSet rs2 = connectionHelper.query(sql2, null);
            this.rules = this.populateRuleMap(rs2);

            String sql3 = MessageFormat.format("select * from {0}", itemChildTable);
            ResultSet rs3 = connectionHelper.query(sql3, null);
            this.parents = this.populateParents(rs3, this.items);

            this.cache.set(this.cacheKey, Triplet.with(this.items, this.rules, this.parents), 0);

        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    protected Map<String, List<String>> populateParents(ResultSet rs, Map<String, Item> items) throws SQLException{
        Map<String, List<String>> parents = new TreeMap<>();
        while (rs.next()){
            String child = rs.getString("child");

            if(items.containsKey(child)){
                if (!parents.containsKey(child)){
                    parents.put(child, new ArrayList<>());
                }
                String parent = rs.getString("parent");
                parents.get(child).add(parent);
            }
        }
        rs.close();
        return parents;
    }

    protected Map<String, Rule> populateRuleMap(ResultSet rs) throws SQLException{
        Map<String, Rule> result = new TreeMap<>();
        while (rs.next()){
            Rule rule = this.populateRule(rs);
            result.put(rule.getName(), rule);
        }
        rs.close();
        return result;
    }

    protected Rule populateRule(ResultSet rs) throws SQLException {
//        Rule rule = new Rule();
//        rule.setName(rs.getString("name"););
//        rule.setData(rs.getString("data"));
//        rule.setCreatedAt(rs.getLong("created_at"));
//        rule.setUpdatedAt(rs.getLong("updated_at"));
        try {
            String name = rs.getString("name");
            String data = rs.getString("data");
            Class clazz = Class.forName(name);
            Rule rule = (Rule) JSONObject.parseObject(data, clazz);
            return rule;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    protected Map<String, Item> populateItemMap(ResultSet rs) throws SQLException{
        Map<String, Item> result = new TreeMap<>();
        while (rs.next()){
            Item item = this.populateItem(rs);
            result.put(item.getName(), item);
        }
        rs.close();
        return result;
    }

    protected Item populateItem(ResultSet rs) throws SQLException {
        Item item = null;
        int type = rs.getInt("type");
        if (type == Item.TYPE_ROLE) {
            Role role = new Role();
            role.setName(rs.getString("name"));
            role.setType(type);
            role.setDescription(rs.getString("description"));
            role.setData(rs.getString("data"));
            role.setRuleName(rs.getString("rule_name"));
            role.setCreatedAt(rs.getLong("created_at"));
            role.setUpdatedAt(rs.getLong("updated_at"));

            item = role;
        } else {
            Permission permission = new Permission();
            permission.setName(rs.getString("name"));
            permission.setType(type);
            permission.setDescription(rs.getString("description"));
            permission.setData(rs.getString("data"));
            permission.setRuleName(rs.getString("rule_name"));
            permission.setCreatedAt(rs.getLong("created_at"));
            permission.setUpdatedAt(rs.getLong("updated_at"));
            item = permission;
        }
        return item;
    }

    protected void getChildrenRecursive(String roleName, Map<String,List<String>> childrenList, Map<String, Boolean> result ){
        if(childrenList.containsKey(roleName)){
            for (String child : childrenList.get(roleName)){
                result.put(child, true);
                this.getChildrenRecursive(child, childrenList, result);
            }
        }
    }

    protected Map<String,List<String>> getChildrenList(){
        Map<String,List<String>> parents = new TreeMap<>();

        try {
            String sql = MessageFormat.format("select * from {0}", itemChildTable);
            ResultSet rs = connectionHelper.query(sql, null);
            while (rs.next()){
                String parent = rs.getString("parent");
                String child = rs.getString("child");
                if(!parents.containsKey(parent)){
                    parents.put(parent, new ArrayList<>());
                }
                parents.get(parent).add(child);
            }
            rs.close();
            return parents;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    protected Boolean detectLoop(Item parent, Item child){
        if(child.getName().equals(parent.getName())){
            return true;
        }
        for (Item grandChild : this.getChildren(child.getName()).values()){
            if(this.detectLoop(parent, grandChild)){
                return true;
            }
        }
        return false;
    }

    protected Map<String, Permission> getDirectPermissionsByUser(Object userId){
        try {
            String sql = MessageFormat.format("select b.* from {0} as a, {1} as b where a.item_name=b.name and a.user_id=? and b.type={2}", assignmentTable, itemTable, Item.TYPE_PERMISSION);
            ResultSet rs = connectionHelper.query(sql, userId);
            Map<String, Permission> result = new TreeMap<>();
            while (rs.next()){
                Permission permission = (Permission)this.populateItem(rs);
                result.put(permission.getName(), permission);
            }
            rs.close();
            return result;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    protected Map<String, Permission> getInheritedPermissionsByUser(Object userId){
        try {

            Map<String, List<String>> childrenList = this.getChildrenList();
            String sql = MessageFormat.format("select item_name from {0} where user_id=?", assignmentTable);
            ResultSet rs = connectionHelper.query(sql, userId);
            Map<String, Boolean> result = new TreeMap<>();
            while (rs.next()){
                String roleName = rs.getString("item_name");
                this.getChildrenRecursive(roleName, childrenList, result);
            }
            rs.close();
            if(result.size() == 0){
                return new HashMap<>();
            }
            String roleNames = StringUtils.collectionToDelimitedString(result.keySet(), ",", "'", "'");
            String sql2 = MessageFormat.format("select * from {0} where type={1} and name in({2})", itemTable, Item.TYPE_PERMISSION, roleNames);
            ResultSet rs2 = connectionHelper.query(sql2, null);
            Map<String, Permission> result2 = new TreeMap<>();
            while (rs2.next()){
                Permission permission = (Permission) this.populateItem(rs2);
                result2.put(permission.getName(), permission);
            }
            rs2.close();
            return result2;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    protected boolean removeAllItems(int type){
        try {
            String sql = MessageFormat.format("select name from {0} where type=?", itemTable);
            ResultSet rs = connectionHelper.query(sql, null);
            List<String> names = new ArrayList<>();
            while (rs.next()){
                String name = rs.getString("name");
                names.add(name);
            }
            rs.close();
            if(names.size() == 0){
                return true;
            }

            String key = type == Item.TYPE_PERMISSION ? "child" :  "parent";
            String names_in = StringUtils.collectionToDelimitedString(names, ",", "'", "'");

            String sql2 = MessageFormat.format("delete from {0} where {1} in ({2})", itemChildTable, key, names_in);
            connectionHelper.exec(sql2, null);

            String sql3 = MessageFormat.format("delete from {0} where item_name in ({1})", assignmentTable, names_in);
            connectionHelper.exec(sql3, null);

            String sql4 = MessageFormat.format("delete from {0} where type=?", itemTable);
            connectionHelper.exec(sql4, type);

            this.invalidateCache();
            return true;
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    protected boolean isEmptyUserId(Object userId){
        return userId == null || (userId instanceof String && !StringUtils.hasText((String)userId));
    }
    //endregion
}

