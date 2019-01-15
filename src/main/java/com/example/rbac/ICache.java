package com.example.rbac;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface ICache {
    String buildKey(Object key);

    Object get(Object key);

    boolean exists(Object key);

    Map<Object, Object> multiGet(List<Object> keys);

    boolean set(Object key, Object value, int duration);

    List<Object> multiSet(Map<Object, Object> item, int duration);

    boolean add(Object key, Object value, int duration);

    List<Object> multiAdd(Map<Object, Object> item, int duration);

    boolean delete(Object key);

    boolean flush();

    Object getOrSet(Object key, Function callback, int duration);
}
