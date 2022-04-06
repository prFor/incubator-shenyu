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
import org.apache.shenyu.common.utils.ReflectUtils;
import org.apache.shenyu.registry.api.Registry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The type Zookeeper registry test.
 */
public class ZookeeperRegistryTest {

    private Registry zookeeperRegistry;
    private final Map<String, Object> zookeeperBroker = new HashMap<>();
    private ZkClient zkClient;

    /**
     * Sets up.
     */
    @BeforeEach
    public void setUp() {
        this.zookeeperRegistry = new ZookeeperRegistry();
        zkClient = mockClient();
        ReflectUtils.setFieldValue(zookeeperRegistry, "zkClient", zkClient);
        zookeeperBroker.clear();
    }

    /**
     * Registry.
     */
    @Test
    public void registry() {
        List<String> paths = new ArrayList<>();
        paths.add("shenyu");
        paths.add("registry");
        paths.add("service");
        paths.add("test");
        HashMap<String, Object> data = new HashMap<>();
        data.put("ip", "127.0.0.1");
        data.put("port", 4331);
        data.put("service", "org.apache.shenyu.registry.zookeeper.ZookeeperRegistryTest");
        String pathJoin = "/" + String.join("/", paths);
        zookeeperRegistry.registry(paths, data, true);
        Mockito.verify(zkClient, Mockito.times(1)).createEphemeral(Mockito.anyString(), Mockito.any(Object.class));
        Mockito.verify(zkClient, Mockito.times(3)).createEphemeral(Mockito.anyString());
        Assertions.assertTrue(zookeeperBroker.containsKey(pathJoin));
        Object dataJson = zookeeperBroker.get(pathJoin);
        Assertions.assertNotEquals("", dataJson);
    }

    private ZkClient mockClient() {
        ZkClient zkClient = Mockito.mock(ZkClient.class);
        Mockito.doAnswer(invocationOnMock -> {
            Object argument = invocationOnMock.getArgument(0);
            Object data = invocationOnMock.getArgument(1);
            zookeeperBroker.put(argument.toString(), data);
            return 1;
        }).when(zkClient).createEphemeral(Mockito.anyString(), Mockito.any(Object.class));
        return zkClient;
    }
}