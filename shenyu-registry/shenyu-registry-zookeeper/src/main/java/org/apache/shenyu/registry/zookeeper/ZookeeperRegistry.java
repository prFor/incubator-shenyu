/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.registry.zookeeper;


import org.I0Itec.zkclient.ZkClient;
import org.apache.shenyu.common.annotations.Beta;
import org.apache.shenyu.common.utils.JsonUtils;
import org.apache.shenyu.registry.api.AbstractRegistry;
import org.apache.shenyu.registry.api.RegistryConfig;
import org.apache.shenyu.registry.api.RegistryInfo;
import org.apache.shenyu.spi.Join;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ZookeeperRegistry .
 * zookeeper Implementation of the registry.
 */
@Join
public class ZookeeperRegistry extends AbstractRegistry {
    
    private final Logger logger = LoggerFactory.getLogger(ZookeeperRegistry.class);
    
    /**
     * client.
     */
    private ZkClient zkClient;
    
    private final Map<String, ZookeeperListener> listeners = new ConcurrentHashMap<>();
    
    /**
     * Instantiates a new Zookeeper registry.
     */
    @Beta
    public ZookeeperRegistry() {
        super(null);
    }
    
    /**
     * Instantiates a new Abstract registry.
     *
     * @param registryConfig the registry config
     */
    public ZookeeperRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
        //create zkClient.
        createClient();
    }
    
    private void createClient() {
        Properties props = this.getRegistryConfig().getProps();
        int sessionTimeout = Integer.parseInt(props.getProperty("sessionTimeout", "30000"));
        int connectionTimeout = Integer.parseInt(props.getProperty("connectionTimeout", "3000"));
        this.zkClient = new ZkClient(this.getRegistryConfig().getServerLists(), sessionTimeout, connectionTimeout);
        this.zkClient.setZkSerializer(new ZookeeperSerializer());
    }
    
    @Override
    public void registry(final RegistryInfo registryInfo, final Map<String, Object> data, final boolean ephemeral) {
        Assert.notNull(registryInfo, "path is null");
        //build a path.
        String path = this.getPath(registryInfo);
        String dataJson = this.getData(data);
        try {
            this.create(path, dataJson, ephemeral);
        } catch (Exception e) {
            logger.warn("registry path {} Fail", path, e);
        }
        
    }
    
    @Override
    public void unRegistry(final RegistryInfo registryInfo) {
        Assert.notNull(registryInfo, "path is null");
        //build a path.
        String path = this.getPath(registryInfo);
        try {
            this.remove(path);
        } catch (Exception ex) {
            logger.warn("unRegistry path {} Fail", path, ex);
        }
    }
    
    @Override
    protected void doSubscribe(final RegistryInfo registryInfo) {
        String path = this.getPath(registryInfo);
        ZookeeperListener zkListener = this.listeners.computeIfAbsent(path, k -> new ZookeeperListener() {
            @Override
            void dataChange(final String path1, final String data) {
                ZookeeperRegistry.this.notify(path1, data);
            }
        });
        this.zkClient.subscribeDataChanges(path, zkListener);
        Object data = this.zkClient.readData(path);
        super.notify(path, data.toString());
    }
    
    @Override
    protected void doUnSubscribe(final RegistryInfo registryInfo) {
        String path = this.getPath(registryInfo);
        ZookeeperListener zkListener = this.listeners.get(path);
        if (zkListener != null) {
            zkClient.unsubscribeDataChanges(path, zkListener);
        }
    }
    
    /**
     * Create your own registered type..
     *
     * @return the string
     */
    @Override
    protected String type() {
        return "zookeeper";
    }
    
    private void create(String path,
                        String data,
                        boolean ephemeral) {
        if (zkClient.exists(path)) {
            this.remove(path);
        }
        int index = path.lastIndexOf(SEPARATOR);
        if (index > 0) {
            create(path.substring(0, index), false);
        }
        if (ephemeral) {
            zkClient.createEphemeral(path, data);
        } else {
            zkClient.createPersistent(path, data);
        }
    }
    
    private void create(String path,
                        boolean ephemeral) {
        if (!ephemeral) {
            if (zkClient.exists(path)) {
                return;
            }
        }
        int index = path.lastIndexOf(SEPARATOR);
        if (index > 0) {
            create(path.substring(0, index), false);
        }
        if (ephemeral) {
            zkClient.createEphemeral(path);
        } else {
            zkClient.createPersistent(path);
        }
    }
    
    private void remove(String path) {
        boolean exists = zkClient.exists(path);
        if (exists) {
            zkClient.delete(path);
        }
    }
    
    private String getData(Map<String, Object> data) {
        return JsonUtils.toJson(data);
    }
}
