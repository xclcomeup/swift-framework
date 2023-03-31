package com.liepin.swift.framework.mvc.impl.app;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import com.liepin.common.conf.PropUtil;
import com.liepin.common.des.AesEcbOncePlus;
import com.liepin.common.des.AesEcbPlus;
import com.liepin.common.des.RsaPlus;
import com.liepin.common.other.StaggerTime;
import com.liepin.zookeeper.client.IZookeeperClient;
import com.liepin.zookeeper.client.enums.EnumChangedEvent;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.listener.NewNodeListener;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public class AppSafeHelper {

    private final AtomicReference<Map<String, SafeHold>> holder = new AtomicReference<>();

    private final String schema;
    private static AppSafeHelper instance = new AppSafeHelper();

    private AppSafeHelper() {
        this.schema = PropUtil.getInstance().get("app.schema");
        load();
        createListener();
    }

    public static AppSafeHelper getInstance() {
        return instance;
    }

    @SuppressWarnings("unchecked")
    private void load() {
        Map<String, Object> map = ZookeeperFactory.useDefaultZookeeperWithoutException().getMap(EnumNamespace.PUBLIC,
                "security/app");
        if (map == null || map.isEmpty()) {
            return;
        }
        Map<String, SafeHold> data = new HashMap<>();
        for (Entry<String, Object> entry : map.entrySet()) {
            String schema = entry.getKey();
            Map<String, Object> schemaMap = (Map<String, Object>) entry.getValue();
            SafeHold safeHold = new SafeHold();
            safeHold.setSchema(schema);
            Map<String, Object> asymmetricMap = (Map<String, Object>) schemaMap.get("asymmetric");
            safeHold.setPublicKey((String) asymmetricMap.get("publicKey"));
            safeHold.setPrivateKey((String) asymmetricMap.get("privateKey"));
            safeHold.setAesKey((String) schemaMap.get("symmetric"));
            safeHold.setWhileList1((List<String>) schemaMap.get("whiteList"));
            safeHold.setStartUp((Boolean) schemaMap.get("startUp"));
            data.put(schema, safeHold);
        }
        holder.getAndSet(data);
    }

    private void createListener() {
        ZookeeperFactory.useDefaultZookeeperWithoutException().addListener(new NewNodeListener() {

            @Override
            public String listeningPath() {
                return EnumNamespace.PUBLIC.getNamespace() + "/security/app";
            }

            @Override
            public void nodeChanged(IZookeeperClient zookeeperClient, EnumChangedEvent type) {
                if (EnumChangedEvent.UPDATED == type) {
                    // 错开时间
                    StaggerTime.waited();
                    load();
                }
            }
        });
    }

    public Map<String, Object> export(String schema) throws Exception {
        Map<String, SafeHold> map = holder.get();
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        SafeHold safeHold = map.get(schema);
        if (safeHold == null) {
            return Collections.emptyMap();
        }
        if (!safeHold.isStartUp()) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> schemaMap = new HashMap<>();
        schemaMap.put("priKey",
                RsaPlus.getInstance().encryptByPrivateKey(safeHold.getAesKey(), safeHold.getPrivateKey()));// 使用非对称私钥加密对称私钥
        schemaMap.put("whiteList", safeHold.getWhileList1());
        result.put(schema, schemaMap);
        return result;
    }

    public Map<String, Object> export(String schema, String token) throws Exception {
        Map<String, SafeHold> map = holder.get();
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        SafeHold safeHold = map.get(schema);
        if (safeHold == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> schemaMap = new HashMap<>();
        // 使用非对称私钥解密客户端用非对称公钥加密的动态aes对称私钥
        String dynamicAesKey = RsaPlus.getInstance().decryptByPrivateKey(token, safeHold.getPrivateKey());
        schemaMap.put("priKey", AesEcbOncePlus.getInstance().encrypt(safeHold.getAesKey(), dynamicAesKey));
        schemaMap.put("whiteList", (safeHold.isStartUp()) ? safeHold.getWhileList1() : Collections.emptyList());
        result.put(schema, schemaMap);
        return result;
    }

    public String encode(String content, String path) throws Exception {
        Map<String, SafeHold> map = holder.get();
        if (map == null || map.isEmpty()) {
            return content;
        }
        if (schema == null) {
            return content;
        }
        SafeHold safeHold = map.get(schema);
        if (safeHold == null) {
            return content;
        }
        if (!safeHold.isStartUp() || safeHold.isWhileList(path)) {
            return content;
        }
        return AesEcbPlus.getInstance().encrypt(content, safeHold.getAesKey());
    }

    public String decode(String content, String path) throws Exception {
        Map<String, SafeHold> map = holder.get();
        if (map == null || map.isEmpty()) {
            return content;
        }
        if (schema == null) {
            return content;
        }
        SafeHold safeHold = map.get(schema);
        if (safeHold == null) {
            return content;
        }
        if (!safeHold.isStartUp() || safeHold.isWhileList(path)) {
            return content;
        }
        return AesEcbPlus.getInstance().decrypt(content, safeHold.getAesKey());
    }

}
