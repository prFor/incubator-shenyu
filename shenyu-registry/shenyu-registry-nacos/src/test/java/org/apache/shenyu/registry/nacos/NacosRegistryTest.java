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

package org.apache.shenyu.registry.nacos;

import org.apache.shenyu.registry.api.RegistryConfig;
import org.apache.shenyu.registry.api.RegistryConsumer;
import org.apache.shenyu.registry.api.RegistryInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;

public class NacosRegistryTest {
    
    @Test
    public void testRegistry() {
    
    }
    
    public static void main(String[] args) throws InterruptedException {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setServerLists("127.0.0.1:8848");
        Properties properties = new Properties();
//        properties.setProperty("nacosNameSpace", "test");
        properties.setProperty("username", "nacos");
        properties.setProperty("password", "nacos");
        registryConfig.setProps(properties);
        NacosRegistry zookeeperRegistry = new NacosRegistry(registryConfig);
        Set<String> paths = new HashSet<>();
        paths.add("shenyu");
        paths.add("registry");
        paths.add("service");
        paths.add("test");
        Map<String, String> data = new HashMap<>();
        data.put("service2", "org.apache.shenyu.registry.zookeeper.ZookeeperRegistryTest");
        RegistryInfo registryInfo = new RegistryInfo();
        registryInfo.setHost("127.0.0.1");
        registryInfo.setPort(3232);
        registryInfo.setPaths(paths);
        registryInfo.setParameters(data);
        zookeeperRegistry.registry(registryInfo, data, true);
        RegistryConsumer registryConsumer = System.out::println;
        zookeeperRegistry.subscribe(registryInfo, registryConsumer);
        Thread.sleep(Integer.MAX_VALUE);
    }
}