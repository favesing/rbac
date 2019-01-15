package com.example.rbac;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class RbacApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(RbacApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        //this.testBuildAuthData();
        testBuildAuthData2();
    }

    @Autowired
    private BaseManager auth;

    private void testBuildAuthData(){
        System.out.println("============清除授权数据=============");
        auth.removeAll();

        System.out.println("============创建授权数据=============");
        Permission createPost = auth.createPermission("createPost");
        createPost.setDescription("Create a post");
        auth.add(createPost);

        Permission updatePost = auth.createPermission("updatePost");
        updatePost.setDescription("Update a post");
        auth.add(updatePost);

        Role author = auth.createRole("author");
        auth.add(author);
        auth.addChild(author, createPost);

        Role admin = auth.createRole("admin");
        auth.add(admin);
        auth.addChild(admin, updatePost);
        auth.addChild(admin, author);

        auth.assign(author, 2);
        auth.assign(admin, 1);

        //query
        System.out.println("============查询授权数据=============");
        Map<String, Item> roles = auth.getItems(Item.TYPE_ROLE);
        print("getItems(Role)", roles);

        Map<String, Item> permissions = auth.getItems(Item.TYPE_PERMISSION);
        print("getItems(Permission)", permissions);

        Item createPostItem = auth.getItem("createPost");
        print("getItem(createPost)", createPostItem);

        Item updatePostItem = auth.getItem("updatePost");
        print("getItem(updatePost)", updatePostItem);

        Permission createPostPermission = auth.getPermission("createPost");
        print("getPermission(createPost)", createPostPermission);

        Permission updatePostPermission = auth.getPermission("updatePost");
        print("getPermission(updatePost)", updatePostPermission);

        Role adminRole = auth.getRole("admin");
        print("getRole(admin)", adminRole);

        Role authorRole = auth.getRole("author");
        print("getRole(author)", authorRole);

        Map<String, Permission> permissionMap = auth.getPermissions();
        print("getPermissions", permissionMap);

        Map<String, Role> roleMap = auth.getRoles();
        print("getRoles", roleMap);

        Map<String, Permission> user1Permission = auth.getPermissionsByUser(1);
        print("getPermissionsByUser(1)", user1Permission);

        Map<String, Permission> user2Permission = auth.getPermissionsByUser(2);
        print("getPermissionsByUser(2)", user2Permission);

        Map<String, Role> user1Roles = auth.getRolesByUser(1);
        print("getRolesByUser(1)", user1Roles);

        Map<String, Role> user2Roles = auth.getRolesByUser(2);
        print("getRolesByUser(2)", user2Roles);

        boolean canAccess1 = auth.checkAccess(2, "createPost", null );
        boolean canAccess2 = auth.checkAccess(2, "updatePost", null );

        System.out.println(">>> checkAccess(2, createPost) = " + canAccess1);
        System.out.println(">>> checkAccess(2, updatePost) = " + canAccess2);
    }

    private void testBuildAuthData2(){
        System.out.println("============清除授权数据=============");
        auth.removeAll();

        System.out.println("============创建授权数据=============");
        Permission createPost = auth.createPermission("createPost");
        createPost.setDescription("Create a post");
        auth.add(createPost);

        Permission updatePost = auth.createPermission("updatePost");
        updatePost.setDescription("Update a post");
        auth.add(updatePost);

        Role author = auth.createRole("author");
        auth.add(author);
        auth.addChild(author, createPost);

        Role admin = auth.createRole("admin");
        auth.add(admin);
        auth.addChild(admin, updatePost);
        auth.addChild(admin, author);

        auth.assign(author, 2);
        auth.assign(admin, 1);

        Rule rule = new AuthorRule();
        auth.addRule(rule);

        Permission updateOwnPost = auth.createPermission("updateOwnPost");
        updateOwnPost.setDescription("Update own post");
        updateOwnPost.setRuleName(rule.getName());
        auth.add(updateOwnPost);

        auth.addChild(updateOwnPost, updatePost);
        auth.addChild(author, updateOwnPost);

        Map<String, Post> params = new HashMap<>();
        Post post = new Post();
        post.setCreatedBy(2);
        params.put("post", post);
        boolean canAccess1 = auth.checkAccess(1, "updatePost", params );
        boolean canAccess2 = auth.checkAccess(2, "updatePost", params );

        System.out.println(">>> checkAccess(1, updateOwnPost) = " + canAccess1);
        System.out.println(">>> checkAccess(2, updateOwnPost) = " + canAccess2);
    }

    public static void print(String message, Item item){
        System.out.println(">>>" + message);
        System.out.println("\t" + JSONObject.toJSONString(item));
    }
    public static void print(String message, Map<?, ?> map){
        System.out.println(">>>" + message);
        for (Map.Entry<?, ?> entry : map.entrySet()){
            System.out.println("\t" + entry.getKey() + "=" + JSONObject.toJSONString(entry.getValue()));
        }
    }
}

