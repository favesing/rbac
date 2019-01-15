package com.example.rbac;

import java.util.Map;

public interface ICheckAccess {
    boolean checkAccess(Object userId, String $permissionName, Map params);
}
