package com.example.rbac;

import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class Cache implements ICache {

    public String keyPrefix;

    @Override
    public String buildKey(Object key) {
        String cacheKey = null;
        if(key instanceof String){
            String keyStr = (String)key;
            cacheKey = keyStr.getBytes().length <= 32 ? keyStr : StringHelper.md5Hex(keyStr);
        }else{
            cacheKey = JSONObject.toJSONString(key);
        }
        return (this.keyPrefix != null ? this.keyPrefix : "").concat(cacheKey);
    }

    @Override
    public Object get(Object key) {
        String cacheKey = this.buildKey(key);
        Object value = this.getValue(cacheKey);
        return value;
    }

    @Override
    public boolean exists(Object key) {
        String cacheKey = this.buildKey(key);
        Object value = this.getValue(cacheKey);
        return value != null;
    }

    @Override
    public Map<Object, Object> multiGet(List<Object> keys) {
        Map<Object, Object> keyMap = new HashMap<>();
        for (Object key : keys){
            keyMap.put(key, this.buildKey(key));
        }
        Map<Object, Object> values = this.getValues(keyMap.values().toArray());
        Map<Object, Object> result = new HashMap<>();
        for (Map.Entry<Object,Object> entry : keyMap.entrySet()){
            if(values.containsKey(entry.getKey())){
                result.put(entry.getKey(), values.get(entry.getKey()));
            }
        }
        return result;
    }

    @Override
    public boolean set(Object key, Object value, int duration) {
        String newKey = this.buildKey(key);
        return this.setValue(newKey, value, duration);
    }

    @Override
    public List<Object> multiSet(Map<Object, Object> item, int duration) {
        Map<Object, Object> data = new HashMap<>();
        for (Map.Entry<Object, Object> entry : item.entrySet()){
            String newKey = this.buildKey(entry.getKey());
            data.put(newKey, entry.getValue());
        }
        return this.setValues(data, duration);
    }

    @Override
    public boolean add(Object key, Object value, int duration) {
        String newKey = this.buildKey(key);
        return this.addValue(newKey, value, duration);
    }

    @Override
    public List<Object> multiAdd(Map<Object, Object> item, int duration) {
        Map<Object, Object> data = new HashMap<>();
        for (Map.Entry<Object, Object> entry : item.entrySet()){
            String newKey = this.buildKey(entry.getKey());
            data.put(newKey, entry.getValue());
        }
        return this.addValues(data, duration);
    }

    @Override
    public boolean delete(Object key) {
        String newKey = this.buildKey(key);
        return this.deleteValue(newKey);
    }

    @Override
    public boolean flush() {
        return this.flushValues();
    }

    @Override
    public Object getOrSet(Object key, Function callback, int duration) {
        Object value = this.get(key);
        if(value != null){
            return value;
        }

        value = callback.apply(this);
        if(this.setValue(key, value, duration) != false){
            throw new IllegalArgumentException("Failed to set cache value for key " + JSONObject.toJSONString(key));
        }
        return value;
    }


    //region "protected"
    protected List<Object> addValues(Map<?, ?> data, int duration){
        List<Object> failedKeys = new ArrayList<>();
        for (Map.Entry entry : data.entrySet()){
            if(this.addValue(entry.getKey(), entry.getValue(), duration) == false){
                failedKeys.add(entry.getKey());
            }
        }
        return failedKeys;
    }
    protected List<Object> setValues(Map<?, ?> data, int duration){
        List<Object> failedKeys = new ArrayList<>();
        for (Map.Entry entry : data.entrySet()){
            if(this.setValue(entry.getKey(), entry.getValue(), duration) == false){
                failedKeys.add(entry.getKey());
            }
        }
        return failedKeys;
    }
    protected Map<Object,Object> getValues(Object[] keys){
        Map<Object,Object> result = new HashMap<>();
        for (Object key : keys){
            result.put(key, this.getValue(key));
        }
        return result;
    }

    protected abstract boolean addValue(Object key, Object value, int duration);
    protected abstract boolean setValue(Object key, Object value, int duration);
    protected abstract Object getValue(Object key);
    protected abstract boolean deleteValue(Object key);
    protected abstract boolean flushValues();
    //endregion
}
