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

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.apache.shenyu.common.exception.ShenyuException;
import org.apache.shenyu.registry.api.AbstractRegistry;
import org.apache.shenyu.registry.api.RegistryConfig;
import org.apache.shenyu.registry.api.RegistryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * NacosRegistry .
 * nacos registration implementation.
 */
public class NacosRegistry extends AbstractRegistry {
    
    private final Logger logger = LoggerFactory.getLogger(NacosRegistry.class);
    
    private NamingService namingService;
    
    /**
     * Instantiates a new Abstract registry.
     *
     * @param registryConfig the registry config
     */
    public NacosRegistry(final RegistryConfig registryConfig) {
        super(registryConfig);
        createClient(registryConfig);
    }
    
    private void createClient(RegistryConfig config) {
        String serverAddr = config.getServerLists();
        Properties properties = config.getProps();
        Properties nacosProperties = new Properties();
        nacosProperties.put(PropertyKeyConst.SERVER_ADDR, serverAddr);
        Optional.ofNullable(properties.getProperty("nacosNameSpace")).ifPresent(e -> {
            nacosProperties.put(PropertyKeyConst.NAMESPACE, e);
        });
        // the nacos authentication username
        nacosProperties.put(PropertyKeyConst.USERNAME, properties.getProperty(PropertyKeyConst.USERNAME, ""));
        // the nacos authentication password
        nacosProperties.put(PropertyKeyConst.PASSWORD, properties.getProperty(PropertyKeyConst.PASSWORD, ""));
        // access key for namespace
        nacosProperties.put(PropertyKeyConst.ACCESS_KEY, properties.getProperty(PropertyKeyConst.ACCESS_KEY, ""));
        // secret key for namespace
        nacosProperties.put(PropertyKeyConst.SECRET_KEY, properties.getProperty(PropertyKeyConst.SECRET_KEY, ""));
        try {
            this.namingService = NamingFactory.createNamingService(nacosProperties);
        } catch (NacosException e) {
            logger.warn("create nacos server error", e);
            throw new ShenyuException(e);
        }
    }
    
    @Override
    protected void doSubscribe(final RegistryInfo registryInfo) {
        String path = this.getPath(registryInfo);
        try {
            namingService.subscribe(path, event -> {
                NamingEvent namingEvent = (NamingEvent) event;
                List<Instance> instances = namingEvent.getInstances();
                for (final Instance instance : instances) {
                    Map<String, String> metadata = instance.getMetadata();
                    this.notify(instance.getServiceName(), metadata);
                }
            });
        } catch (NacosException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void registry(final RegistryInfo registryInfo, final Map<String, String> data, final boolean ephemeral) {
        try {
            this.createInstance(registryInfo, data, ephemeral);
        } catch (Exception ex) {
            logger.warn("registry nacos service error: {}", registryInfo);
        }
    }
    
    /**
     * Do un subscribe.
     *
     * @param registryInfo the registry info
     */
    @Override
    protected void doUnSubscribe(final RegistryInfo registryInfo) {
    
    }
    
    /**
     * Create your own registered type..
     *
     * @return the string
     */
    @Override
    protected String type() {
        return "nacos";
    }
    
    /**
     * unregister.
     *
     * @param registryInfo the registry info
     */
    @Override
    public void unRegistry(final RegistryInfo registryInfo) {
        String path = this.getPath(registryInfo);
        try {
            this.namingService.deregisterInstance(path, registryInfo.getHost(), registryInfo.getPort());
        } catch (NacosException e) {
            logger.warn(" unRegistry nacos service error: {}", registryInfo);
        }
    }
    
    private void createInstance(RegistryInfo registryInfo,
                                final Map<String, String> data,
                                boolean ephemeral) throws NacosException {
        Instance instance = new Instance();
        instance.setEphemeral(ephemeral);
        instance.setPort(registryInfo.getPort());
        instance.setIp(registryInfo.getHost());
        instance.setMetadata(data);
        String path = this.getPath(registryInfo);
        namingService.registerInstance(path, instance);
    }
}
