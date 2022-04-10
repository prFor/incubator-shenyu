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

package org.apache.shenyu.registry.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * AbstractRegistry .
 */
public abstract class AbstractRegistry implements Registry {
    
    private final Logger logger = LoggerFactory.getLogger(AbstractRegistry.class);
    
    /**
     * The constant SEPARATOR.
     */
    protected static final String SEPARATOR = "/";
    
    private final RegistryConfig registryConfig;
    
    private final Map<String, Set<RegistryConsumer>> subscribed = new ConcurrentHashMap<>(16);
    
    /**
     * Instantiates a new Abstract registry.
     *
     * @param registryConfig the registry config
     */
    public AbstractRegistry(final RegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
    }
    
    /**
     * Gets registry config.
     *
     * @return the registry config
     */
    public RegistryConfig getRegistryConfig() {
        return registryConfig;
    }
    
    @Override
    public void subscribe(final RegistryInfo registryInfo, final RegistryConsumer consumer) {
        Assert.notNull(registryInfo, "pathList is null");
        Assert.notNull(consumer, "consumer is null");
        String path = this.getPath(registryInfo);
        logger.info("subscribe {} success", path);
        Set<RegistryConsumer> lists = subscribed.computeIfAbsent(path, set -> new CopyOnWriteArraySet<>());
        lists.add(consumer);
        this.doSubscribe(registryInfo);
    }
    
    @Override
    public void unSubscribe(final RegistryInfo registryInfo, final RegistryConsumer consumer) {
        Assert.notNull(registryInfo, "pathList is null");
        Assert.notNull(consumer, "consumer is null");
        String path = this.getPath(registryInfo);
        logger.info("unSubscribe {} success", path);
        Set<RegistryConsumer> consumers = subscribed.get(path);
        if (consumers != null) {
            consumers.remove(consumer);
        }
    }
    
    /**
     * Convert data into relevant paths.
     *
     * @param registryInfo the registry info
     * @return String. path
     */
    protected String getPath(final RegistryInfo registryInfo) {
        Assert.notNull(registryInfo, "registry info is null");
        return registryInfo.getServiceKey(this.type());
    }
    
    /**
     * notify data.
     *
     * @param path the path
     * @param data the data.
     */
    protected void notify(String path, Map<String,String> data) {
        Set<RegistryConsumer> consumers = subscribed.get(path);
        Optional.ofNullable(consumers).ifPresent(c -> c.forEach(cs -> cs.data(data)));
    }
    
    /**
     * Subclasses implement subscription operations.
     *
     * @param registryInfo the registry info
     */
    protected abstract void doSubscribe(final RegistryInfo registryInfo);
    
    
    /**
     * Do un subscribe.
     *
     * @param registryInfo the registry info
     */
    protected abstract void doUnSubscribe(final RegistryInfo registryInfo);
    
    /**
     * Create your own registered type..
     *
     * @return the string
     */
    protected abstract String type();
}
