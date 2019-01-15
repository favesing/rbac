package com.example.rbac;

import javafx.util.Pair;
import java.util.HashMap;
import java.util.Map;

public class MapCache extends Cache {
    private Map<Object, Object> cache = new HashMap<>();

    @Override
    protected boolean addValue(Object key, Object value, int duration) {
        if(cache.containsKey(key) && ((Pair<Object, Long>)cache.get(key)).getValue() > System.currentTimeMillis()){
            return false;
        }
        cache.put(key, new Pair<Object, Long>(value, System.currentTimeMillis() + duration));
        return true;
    }

    @Override
    protected boolean setValue(Object key, Object value, int duration) {
        cache.put(key, new Pair<Object, Long>(value, System.currentTimeMillis() + duration));
        return true;
    }

    @Override
    protected Object getValue(Object key) {
        if(cache.containsKey(key) && ((Pair<Object, Long>)cache.get(key)).getValue() > System.currentTimeMillis()){
            return ((Pair<Object, Long>)cache.get(key)).getKey();
        }else{
            this.deleteValue(key);
        }
        return null;
    }

    @Override
    protected boolean deleteValue(Object key) {
        cache.remove(key);
        return true;
    }

    @Override
    protected boolean flushValues() {
        cache.clear();
        return true;
    }
}
