package com.example.rbac;

import java.util.Map;

public interface IExecuteRule {
    boolean executeRule(Object userId, Item item, Map params);
}
