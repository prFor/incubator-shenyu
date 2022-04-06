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
import org.apache.shenyu.common.utils.JsonUtils;
import org.apache.shenyu.registry.api.AbstractRegistry;
import org.apache.shenyu.registry.api.RegistryConfig;
import org.apache.shenyu.registry.api.RegistryConsumer;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * ZookeeperRegistry .
 * zookeeper Implementation of the registry.
 */
public class ZookeeperRegistry extends AbstractRegistry {

    private static final String SEPARATOR = "/";

    /**
     * client.
     */
    private ZkClient zkClient;

    /**
     * Instantiates a new Zookeeper registry.
     */
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
    }

    @Override
    public void registry(List<String> pathList, Map<String, Object> data, boolean ephemeral) {
        Assert.notNull(pathList, "path is null");
        //build a path.
        String path = this.getPath(pathList);
        String dataJson = this.getData(data);
        this.create(path, dataJson, ephemeral);
    }

    @Override
    public void unRegistry(Map<String, Object> path) {

    }

    @Override
    public void subscribe(Map<String, Object> path, RegistryConsumer consumer) {

    }

    private void create(String path,
                        String data,
                        boolean ephemeral) {
        if (!ephemeral) {
            if (zkClient.exists(path)) {
                this.remove(path);
            }
        }
        int index = path.lastIndexOf(SEPARATOR);
        if (index > 0) {
            create(path.substring(0, index), ephemeral);
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
            create(path.substring(0, index), ephemeral);
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

    private String getPath(List<String> pathList) {
        return SEPARATOR + String.join(SEPARATOR, pathList);
    }

    private String getData(Map<String, Object> data) {
        return JsonUtils.toJson(data);
    }
}
